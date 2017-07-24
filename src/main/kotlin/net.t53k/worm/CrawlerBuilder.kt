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

    fun onResource(contentType: String, handler: Function<Resource, Collection<String>>): CrawlerBuilder {
        resourceHandler[contentType] = { r: Resource -> handler.apply(r).toList() }
        return this
    }

    fun loadResource(loader: Function<String, Body>): CrawlerBuilder {
        resourceLoader = { url: String -> loader.apply(url) }
        return this
    }

    fun filterLinks(filter: Function<String, Boolean>): CrawlerBuilder {
        linkFilter = { url: String -> filter.apply(url) }
        return this
    }

    fun onError(handler: Consumer<String>): CrawlerBuilder {
        errorHandler = { err: String -> handler.accept(err) }
        return this
    }

    fun onDocument(handler: Consumer<Document>): CrawlerBuilder {
        documentHandler = { doc: Document -> handler.accept(doc) }
        return this
    }

    fun build(): Crawler = Crawler( documentHandler = documentHandler, errorHandler = errorHandler,
            resourceHandler = resourceHandler, resourceLoader = resourceLoader, worker = worker, linkFilter = linkFilter)
}