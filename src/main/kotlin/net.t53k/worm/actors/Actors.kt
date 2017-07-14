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
import net.t53k.worm.Node
import net.t53k.worm.Page
import org.slf4j.LoggerFactory

data class LoadPage(val url: String)
data class ProcessNode(val node: Node)
data class Start(val urls: List<String>)
data class LoadPageError(val url: String)
data class Done(val pagesPending: List<String> = listOf())

class NodeHandler(val onNode: (Node) -> Unit) : Actor() {
    override fun receive(message: Any) {
        when (message) {
            is Node -> onNode(message)
        }
    }
}

class PagerLoader(val pageLoader: (String) -> String, val linkParser: (Page) -> List<String>): Actor() {
    private val log = LoggerFactory.getLogger(javaClass)
    override fun receive(message: Any) {
        when(message) {
            is LoadPage -> {
                try {
                    log.debug("load page: ${message.url}")
                    val page = Page(message.url, pageLoader(message.url))
                    sender() send ProcessNode(Node(page, linkParser(page)))
                } catch (e: Exception) {
                    if(log.isDebugEnabled) log.error("loading page '${message.url}': $e", e)
                    else log.error("loading page '${message.url}': $e")
                    sender() send LoadPageError(message.url)
                }
            }
        }
    }
}

class WorkDispatcher(val onNode: (Node) -> Unit,
                     val worker: Int,
                     val pageLoader: (String) -> String,
                     val linkFilter: (String) -> Boolean,
                     val errorHandler: (String) -> Unit,
                     val linkParser: (Page) -> List<String>): Actor() {
    private lateinit var pageLoaderWorker: List<ActorReference>
    private lateinit var nodeHandler: ActorReference
    private lateinit var router: ActorReference
    private lateinit var starter: ActorReference
    private val pagesPending = mutableSetOf<String>()

    override fun before() {
        nodeHandler = actor("worm/nodeHandler", NodeHandler(onNode))
        pageLoaderWorker = (1..worker).map { actor("worm/worker$it", PagerLoader(pageLoader, linkParser)) }
        router = actor("worm/workerRouter", RoundRobinRouter(pageLoaderWorker))
    }

    override fun receive(message: Any) {
        when(message) {
            is Start -> {
                starter = sender()
                pagesPending += message.urls
                message.urls.forEach { router send LoadPage(it) }
            }
            is ProcessNode -> {
                pagesPending -= message.node.page.url
                message.node.links.filter(linkFilter).filter { !pagesPending.contains(it) }.forEach{
                    pagesPending += it
                    router send LoadPage(it)
                }
                nodeHandler send message.node
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
