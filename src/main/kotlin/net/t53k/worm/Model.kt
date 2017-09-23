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

import java.nio.charset.Charset

data class ContentType(val contentType: String) {
  companion object {
      val DEFAULT_CONTENT_TYPE = "application/octet-stream"
      fun create(contentType: String?): ContentType {
        return ContentType(contentType ?: DEFAULT_CONTENT_TYPE)
      }
  }
  val encoding: String? get() {
    val m = "charset=(\\S+)".toRegex().find(contentType)
    return m?.groups?.get(1)?.value?.trim()
  }
  val mimeType: String get() = contentType.substringBeforeLast(';').trim()
}

data class Body(val content: ByteArray, val contentType: ContentType) {
  val DEFAULT_ENCODING = "utf-8"

  override fun toString(): String {
    return "Body(contentType=${contentType}, bytes=${content.size})"
  }

  fun  text(): String {
    val charset = Charset.forName(contentType.encoding ?: DEFAULT_ENCODING)
    return content.toString(charset)
  }
}

data class Resource(val url: String, val body: Body)

data class Document(val resource: Resource, val links: List<String>) {
  override fun toString(): String {
    return "Node(resource='$resource', linkCount=${links.size})"
  }
}
