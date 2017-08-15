/*
 * Copyright 2017 Thomas Volk
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package net.t53k.worm

import net.t53k.alkali.ActorSystemBuilder
import net.t53k.alkali.PoisonPill
import net.t53k.worm.actors.*
import org.jsoup.Jsoup
import java.net.URI
import java.net.URL
import java.nio.charset.Charset

class Crawler(val documentHandler: (Document) -> Unit,
              val worker: Int = Crawler.DEFAULT_WORKER_COUNT,
              val resourceLoader: (String) -> Body = Crawler.DEFAULT_URL_RESOURCE_LOADER,
              val linkFilter: (String) -> Boolean = Crawler.DEFAULT_LINK_FILTER,
              val errorHandler: (String) -> Unit = Crawler.DEFAULT_ERROR_HANDLER,
              val resourceHandler: Map<String, (Resource) -> List<String>> = Crawler.DEFAULT_RESOURCE_HANDLER_MAP) {

    init {
        require(worker > 0)
    }

    companion object {
        val DEFAULT_ERROR_HANDLER: (String) -> Unit = { _ -> }
        val DEFAULT_LINK_FILTER: (String) -> Boolean = { _ -> true }
        val DEFAULT_WORKER_COUNT = 1
        val DEFAULT_HTML_PAGE_HANDLER: (Resource) -> List<String> = { page ->
            var baseUrl = URI.create(page.url)
            baseUrl = when {
                baseUrl.path == "" -> URI.create(baseUrl.toString() + "/")
                else -> baseUrl
            }
            Jsoup.parse(page.body.text()).select("a").map { it.attr("href") }
                    .map { it.substringBeforeLast("#") }.toSet()
                    .map { baseUrl.resolve(URI.create(it.replace(" ", "%20"))).toString() }
        }
        val DEFAULT_URL_RESOURCE_LOADER: (String) -> Body = { url ->
            val con = URL(url).openConnection()
            val inputStream = con.getInputStream()
            inputStream.use { Body(it.readBytes(), ContentType(con.contentType ?: "application/octet-stream")) }
        }
        val DEFAULT_RESOURCE_HANDLER_MAP: Map<String, (Resource) -> List<String>> = mutableMapOf(
                "text/html" to Crawler.DEFAULT_HTML_PAGE_HANDLER,
                "application/xhtml+xml" to Crawler.DEFAULT_HTML_PAGE_HANDLER
        )
    }

    fun start(urls: List<String>, timeout: Timeout = InfinityTimeout): List<String> {
        val pendingPages = mutableListOf<String>()
        val system = ActorSystemBuilder().onDefaultActorMessage { message ->
            when(message) {
                is Done -> {
                    pendingPages += message.resourcesPending
                    shutdown()
                }
            }
        }.build()
        val dispatcher = system.actor("worm/dispatcher", WorkDispatcher(documentHandler = documentHandler, worker = worker, resourceLoader = resourceLoader,
                linkFilter = linkFilter, errorHandler = errorHandler, resourceHanler = resourceHandler))
        dispatcher send Start(urls)
        timeout.start { dispatcher send PoisonPill }
        system.waitForShutdown()
        return pendingPages.toList()
    }
}
