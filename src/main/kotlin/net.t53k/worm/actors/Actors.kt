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
package net.t53k.worm.actors

import net.t53k.alkali.Actor
import net.t53k.alkali.ActorReference
import net.t53k.alkali.PoisonPill
import net.t53k.alkali.router.RoundRobinRouter
import net.t53k.worm.Page
import org.slf4j.LoggerFactory

data class LoadPage(var url: String)
data class ProcessPage(var page: Page)
data class Start(val urls: List<String>)
data class LoadPageError(val url: String)
data class Done(val pagesPending: List<String> = listOf())

class PageHandler(val onPage: (Page) -> Unit) : Actor() {
    override fun receive(message: Any) {
        when (message) {
            is Page -> onPage(message)
        }
    }
}

class PagerLoader(val pageLoader: (String) -> String): Actor() {
    private val log = LoggerFactory.getLogger(javaClass)
    override fun receive(message: Any) {
        when(message) {
            is LoadPage -> {
                try {
                    log.debug("load page: ${message.url}")
                    val cnt = pageLoader(message.url)
                    sender() send ProcessPage(Page.parse(message.url, cnt))
                } catch (e: Exception) {
                    if(log.isDebugEnabled) log.error("loading page '${message.url}': $e", e)
                    else log.error("loading page '${message.url}': $e")
                    sender() send LoadPageError(message.url)
                }
            }
        }
    }
}

class WorkDispatcher(val onPage: (Page) -> Unit,
                     val worker: Int,
                     val pageLoader: (String) -> String,
                     val linkFilter: (String) -> Boolean,
                     val errorHandler: (String) -> Unit): Actor() {
    private lateinit var pageLoaderWorker: List<ActorReference>
    private lateinit var pageHandler: ActorReference
    private lateinit var router: ActorReference
    private lateinit var starter: ActorReference
    private val pagesPending = mutableSetOf<String>()

    override fun before() {
        pageHandler = actor("worm/pageHandler", PageHandler(onPage))
        pageLoaderWorker = (1..worker).map { actor("worm/worker$it", PagerLoader(pageLoader)) }
        router = actor("worm/workerRouter", RoundRobinRouter(pageLoaderWorker))
    }

    override fun receive(message: Any) {
        when(message) {
            is Start -> {
                starter = sender()
                pagesPending += message.urls
                message.urls.forEach { router send LoadPage(it) }
            }
            is ProcessPage -> {
                pagesPending -= message.page.url
                val page = message.page
                page.links.filter(linkFilter).filter { !pagesPending.contains(it) }.forEach{
                    pagesPending += it
                    router send LoadPage(it)
                }
                pageHandler send page
            }
            is LoadPageError -> {
                pagesPending -= message.url
                errorHandler(message.url)
            }
        }
        if(pagesPending.isEmpty()) {
            self() send PoisonPill
        }
    }

    override fun after() {
        starter send Done(pagesPending.toList())

    }
}
