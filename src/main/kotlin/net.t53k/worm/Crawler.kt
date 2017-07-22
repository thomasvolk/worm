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

class Crawler(val onNode: (Node) -> Unit,
              val worker: Int,
              val resourceLoader: (String) -> Body,
              val linkFilter: (String) -> Boolean,
              val errorHandler: (String) -> Unit,
              val linkParser: (Resource) -> List<String>) {

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
                linkFilter = linkFilter, errorHandler = errorHandler, linkParser = linkParser))
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

class CrawlerBuilder {
    companion object {
        val DEFAULT_RESOURCE_LOADER: (String) -> Body = { url ->
            val con = URL(url).openConnection()
            val inputStream = con.getInputStream()
            try {
                Body(inputStream.readBytes(), con.contentType)
            } finally {
                inputStream.close()
            }
        }
        val DEFAULT_LINK_PARSER: (Resource) -> List<String> = { page ->
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
    private var onNode: (Node) -> Unit = { _ -> }
    private var worker: Int = 4
    private var resourceLoader: (String) -> Body = DEFAULT_RESOURCE_LOADER
    private var linkFilter: (String) -> Boolean = { _ -> true }
    private var errorHandler: (String) -> Unit = { _ -> }
    private var linkParser: (Resource) -> List<String> = DEFAULT_LINK_PARSER

    fun onNode(handler: (Node) -> Unit): CrawlerBuilder {
        onNode = handler
        return this
    }

    fun worker(count: Int): CrawlerBuilder {
        worker = count
        return this
    }

    fun resourceLoader(handler: (String) -> Body): CrawlerBuilder {
        resourceLoader = handler
        return this
    }

    fun withLinkFilter(handler: (String) -> Boolean): CrawlerBuilder {
        linkFilter = handler
        return this
    }

    fun onError(handler: (String) -> Unit): CrawlerBuilder {
        errorHandler = handler
        return this
    }

    fun linkParser(handler: (Resource) -> List<String>): CrawlerBuilder {
        linkParser = handler
        return this
    }

    fun build() = Crawler(onNode = onNode, worker = worker, resourceLoader = resourceLoader,
            linkFilter = linkFilter, errorHandler = errorHandler, linkParser = linkParser)
}