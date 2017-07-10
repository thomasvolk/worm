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

import org.jsoup.Jsoup
import java.net.URI

data class Page(val url: String, val body: String, val links: Collection<String>) {
  companion object Parser {
    fun parse(url: String, body: String): Page {
      val baseUrl = URI(url)
      val links = Jsoup.parse(body).select("a").map { it.attr("href") }.map { baseUrl.resolve(it).toString() }
      return Page(url, body, links)
    }
  }

  override fun toString(): String {
    return "Page(url='$url', links=$links, bodySize=${body.length})"
  }


}

