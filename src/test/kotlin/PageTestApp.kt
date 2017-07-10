import net.t53k.worm.CrawlerBuilder
import net.t53k.worm.MilliSecondsTimeout
import net.t53k.worm.Page

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

fun main(args: Array<String>) {
    println("=== PageTestApp::start ===")
    val seed = args.getOrElse(0) { "http://example.com" }
    val timeout = 10000L
    println("seed: $seed")
    val pages = mutableSetOf<Page>()
    val crawler = CrawlerBuilder()
            .worker(4)
            .onPage { page -> pages += page }
            .withLinkFilter { it.startsWith(seed) }
            .build()
    val pendigPages = crawler.start(listOf(seed), MilliSecondsTimeout(timeout))
    println(pages)
    println("=== PageTestApp::done ===")

}