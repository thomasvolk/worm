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

import java.util.function.Function
import java.util.function.Consumer

class CrawlerBuilder {
    private var documentHandler: (Document) -> Unit = { }
    private var worker: Int = Crawler.DEFAULT_WORKER_COUNT
    private var resourceLoader: (String) -> Body = Crawler.DEFAULT_URL_RESOURCE_LOADER
    private var linkFilter: (String) -> Boolean = Crawler.DEFAULT_LINK_FILTER
    private var errorHandler: (String) -> Unit = Crawler.DEFAULT_ERROR_HANDLER
    private val resourceHandler: MutableMap<String, (Resource) -> List<String>> = Crawler.DEFAULT_RESOURCE_HANDLER_MAP.toMutableMap()

    fun worker(cnt: Int): CrawlerBuilder {
        require( cnt > 0)
        worker = cnt
        return this
    }

    fun onResource(contentType: String, handler: (Resource) -> List<String>): CrawlerBuilder {
        resourceHandler[contentType] = handler
        return this
    }

    fun loadResource(loader: (String) -> Body): CrawlerBuilder {
        resourceLoader = loader
        return this
    }

    fun filterLinks(filter: (String) -> Boolean): CrawlerBuilder {
        linkFilter = filter
        return this
    }

    fun onError(handler: (String) -> Unit): CrawlerBuilder {
        errorHandler = handler
        return this
    }

    fun onDocument(handler: (Document) -> Unit): CrawlerBuilder {
        documentHandler = handler
        return this
    }

    fun onResource(contentType: String, handler: Function<Resource, Collection<String>>) =
            onResource(contentType) { r: Resource -> handler.apply(r).toList() }

    fun loadResource(loader: Function<String, Body>) = loadResource { url: String -> loader.apply(url) }

    fun filterLinks(filter: Function<String, Boolean>) = filterLinks { url: String -> filter.apply(url) }

    fun onError(handler: Consumer<String>) = onError { err: String -> handler.accept(err) }

    fun onDocument(handler: Consumer<Document>) = onDocument { doc: Document -> handler.accept(doc) }

    fun build(): Crawler = Crawler( documentHandler = documentHandler, errorHandler = errorHandler,
            resourceHandler = resourceHandler, resourceLoader = resourceLoader, worker = worker, linkFilter = linkFilter)
}