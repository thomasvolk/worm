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

class Crawler(val onNode: (Node) -> Unit = { _ -> },
              val worker: Int,
              val resourceLoader: (String) -> Body = ResourceLoader.DEFAULT_RESOURCE_LOADER,
              val linkFilter: (String) -> Boolean = { _ -> true },
              val errorHandler: (String) -> Unit = { _ -> },
              val resourceHandler: Map<String, (Resource) -> List<String>> = mutableMapOf(
                      "text/html" to ResourceHandler.DEFAULT_HTML_PAGE_HANDLER,
                      "application/xhtml+xml" to ResourceHandler.DEFAULT_HTML_PAGE_HANDLER
              )) {

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
        val dispatcher = system.actor("worm/dispatcher", WorkDispatcher(onNode = onNode, worker = worker, resourceLoader = resourceLoader,
                linkFilter = linkFilter, errorHandler = errorHandler, resourceHanler = resourceHandler))
        dispatcher send Start(urls)
        timeout.start { dispatcher send PoisonPill }
        system.waitForShutdown()
        return pendingPages.toList()
    }
}

interface Timeout {
    fun start(callback: () -> Unit): Unit
}

object InfinityTimeout : Timeout {
    override fun start(callback: () -> Unit) { /* this will never run the callback */ }
}

class MilliSecondsTimeout(val durationMs: Long) : Timeout {
    override fun start(callback: () -> Unit) {
        Thread.sleep(durationMs)
        callback()
    }
}

object ResourceHandler {
    val DEFAULT_HTML_PAGE_HANDLER: (Resource) -> List<String> = { page ->
        var baseUrl = URI.create(page.url)
        baseUrl = when {
            baseUrl.path == "" -> URI.create(baseUrl.toString() + "/")
            else -> baseUrl
        }
        Jsoup.parse(page.body.content.toString(Charset.forName("UTF-8"))).select("a").map { it.attr("href") }
                .map { it.substringBeforeLast("#") }.toSet()
                .map { baseUrl.resolve(URI.create(it.replace(" ", "%20"))).toString() }
    }

}

object ResourceLoader {
    val DEFAULT_RESOURCE_LOADER: (String) -> Body = { url ->
        val con = URL(url).openConnection()
        val inputStream = con.getInputStream()
        try {
            Body(inputStream.readBytes(), con.contentType ?: "application/octet-stream")
        } finally {
            inputStream.close()
        }
    }
}
