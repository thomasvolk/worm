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

import org.junit.Assert.*
import org.junit.Test

class Pacemaker(urls: List<String>) : Timeout {
    private val expectedUrls = urls.toMutableList()
    private lateinit var callback: () -> Unit

    @Synchronized
    fun pace(url: String) {
        if(expectedUrls.remove(url)) {
            if(expectedUrls.isEmpty()) {
                callback()
            }
        }
    }

    override fun start(callback: () -> Unit) {
        this.callback = callback
    }

}

class CrawlerTest {
    val base = "pages/tree"
    val testClass = javaClass
    val pageLoader: (String) -> Body = { url -> Body(testClass.getResourceAsStream("$base/$url").use { it.readBytes() }, "text/html ; charset=utf-8") }
    val linkFilter: (String) -> Boolean = { l -> !l.contains("filterthis") }

    @Test
    fun crawler() {
        (1..50).forEach { _ ->
            val worker = 4
            val pages = mutableSetOf<String>()
            val errorUrls = mutableSetOf<String>()

            val crawler = Crawler(worker = worker,
                    documentHandler = { doc -> pages += doc.resource.url },
                    errorHandler = { url -> errorUrls += url },
                    resourceLoader = pageLoader,
                    linkFilter = linkFilter)
            val pendigPages = crawler.start(listOf("index.html"))

            assertEquals(listOf<String>(), pendigPages)
            assertEquals(
                    listOf("index.html", "subpage.01.a.html", "subpage.01.b.html", "subpage.02.a.html").sorted(),
                    pages.sorted())
            assertEquals(listOf("notfound.html"), errorUrls.sorted())
        }
    }

    @Test
    fun crawlerTimeout() {
        (1..50).forEach { _ ->
            val worker = 4
            val pages = mutableSetOf<Resource>()
            val errorUrls = mutableListOf<String>()
            val pacemaker = Pacemaker(listOf("index.html", "subpage.01.a.html"))

            val crawler = Crawler(worker = worker,
                    documentHandler = { doc ->
                        pages += doc.resource
                        pacemaker.pace(doc.resource.url)
                    },
                    errorHandler = { url -> errorUrls += url },
                    resourceLoader = pageLoader,
                    linkFilter =linkFilter)

            val pendigPages = crawler.start(listOf("index.html"), pacemaker)
            val pagesProcessed = pages.map { it.url }
            val pagesTotal = (pendigPages + pagesProcessed).filter { url -> url != "notfound.html"}.toSet()
            assertEquals(
                    setOf("index.html", "subpage.01.a.html", "subpage.01.b.html", "subpage.02.a.html").sorted(),
                    pagesTotal.sorted())
        }
    }
}
