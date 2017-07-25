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
import net.t53k.worm.Body
import net.t53k.worm.Document
import net.t53k.worm.Resource
import org.slf4j.LoggerFactory

data class LoadResource(val url: String)
data class ProcessDocument(val node: Document)
data class Start(val urls: List<String>)
data class LoadResourceError(val url: String)
data class Done(val resourcesPending: List<String> = listOf())

class DocumentHandler(val documentHandler: (Document) -> Unit) : Actor() {
    override fun receive(message: Any) {
        when (message) {
            is Document -> documentHandler(message)
        }
    }
}

class ResourceLoader(val resourceLoader: (String) -> Body, val resourceHandler: Map<String, (Resource) -> List<String>>): Actor() {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun getResourceHandler(contentType: String) =
            resourceHandler.getOrElse(contentType.substringBeforeLast(';'), { { listOf() } })

    override fun receive(message: Any) {
        when(message) {
            is LoadResource -> {
                try {
                    log.debug("load resource: ${message.url}")
                    val res = Resource(message.url, resourceLoader(message.url))
                    val handler = getResourceHandler(res.body.contentType)
                    sender() send ProcessDocument(Document(res, handler(res)))
                } catch (e: Exception) {
                    if(log.isDebugEnabled) log.error("loading resource '${message.url}': $e", e)
                    else log.error("loading resource '${message.url}': $e")
                    sender() send LoadResourceError(message.url)
                }
            }
        }
    }
}

class WorkDispatcher(val documentHandler: (Document) -> Unit,
                     val worker: Int,
                     val resourceLoader: (String) -> Body,
                     val linkFilter: (String) -> Boolean,
                     val errorHandler: (String) -> Unit,
                     val resourceHanler: Map<String, (Resource) -> List<String>>): Actor() {
    private lateinit var nodeLoaderWorker: List<ActorReference>
    private lateinit var nodeHandler: ActorReference
    private lateinit var router: ActorReference
    private lateinit var starter: ActorReference
    private val resourcesPending = mutableSetOf<String>()

    override fun before() {
        nodeHandler = actor("worm/nodeHandler", DocumentHandler(documentHandler))
        nodeLoaderWorker = (1..worker).map { actor("worm/worker$it", ResourceLoader(resourceLoader, resourceHanler)) }
        router = actor("worm/workerRouter", RoundRobinRouter(nodeLoaderWorker))
    }

    override fun receive(message: Any) {
        when(message) {
            is Start -> {
                starter = sender()
                resourcesPending += message.urls
                message.urls.forEach { router send LoadResource(it) }
            }
            is ProcessDocument -> {
                resourcesPending -= message.node.resource.url
                message.node.links.filter(linkFilter).filter { !resourcesPending.contains(it) }.forEach{
                    resourcesPending += it
                    router send LoadResource(it)
                }
                nodeHandler send message.node
            }
            is LoadResourceError -> {
                resourcesPending -= message.url
                errorHandler(message.url)
            }
        }
        if(resourcesPending.isEmpty()) {
            self() send PoisonPill
        }
    }

    override fun after() {
        starter send Done(resourcesPending.toList())

    }
}
