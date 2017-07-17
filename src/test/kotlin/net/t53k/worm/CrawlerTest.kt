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
    val pageLoader: (String) -> ByteArray = { url -> testClass.getResourceAsStream("$base/$url").use { it.readBytes() } }
    val linkFilter: (String) -> Boolean = { l -> !l.contains("filterthis") }

    @Test
    fun crawler() {
        val worker = 4
        val pages = mutableSetOf<Page>()
        val errorUrls = mutableListOf<String>()

        val crawler = CrawlerBuilder().worker(worker)
                .onNode { node -> pages += node.page }
                .onError { url -> errorUrls += url  }
                .pageLoader(pageLoader)
                .withLinkFilter(linkFilter)
                .build()
        val pendigPages = crawler.start(listOf("index.html"))

        assertEquals(listOf<String>(), pendigPages)
        assertEquals(
                listOf("index.html", "subpage.01.a.html", "subpage.01.b.html", "subpage.02.a.html").sorted(),
                pages.map { it.url }.sorted())
        assertEquals(listOf("notfound.html"), errorUrls.sorted())
    }

    @Test
    fun crawlerTimeout() {
        (1..50).forEach { _ ->
            val worker = 4
            val pages = mutableSetOf<Page>()
            val errorUrls = mutableListOf<String>()
            val pacemaker = Pacemaker(listOf("index.html", "subpage.01.a.html"))

            val crawler = CrawlerBuilder().worker(worker)
                    .onNode { node ->
                        pages += node.page
                        pacemaker.pace(node.page.url)
                    }
                    .onError { url -> errorUrls += url }
                    .pageLoader { url -> pageLoader(url) }
                    .withLinkFilter(linkFilter)
                    .build()
            val pendigPages = crawler.start(listOf("index.html"), pacemaker)
            val pagesProcessed = pages.map { it.url }
            val pagesTotal = (pendigPages + pagesProcessed).filter { url -> url != "notfound.html"}.toSet()
            assertEquals(
                    setOf("index.html", "subpage.01.a.html", "subpage.01.b.html", "subpage.02.a.html").sorted(),
                    pagesTotal.sorted())
        }
    }
}
