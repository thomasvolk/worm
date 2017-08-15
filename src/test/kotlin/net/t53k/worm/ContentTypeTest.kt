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

import org.junit.Test
import org.junit.Assert.*

class ContentTypeTest {
    @Test
    fun noEncoding() {
        val ct = ContentType.create("text/html  ")
        assertEquals("text/html", ct.mimeType)
        assertNull(ct.encoding)
    }

    @Test
    fun empty() {
        val ct = ContentType.create("   ")
        assertEquals("", ct.mimeType)
        assertNull(ct.encoding)
    }

    @Test
    fun fromNull() {
        val ct = ContentType.create(null)
        assertEquals("application/octet-stream", ct.mimeType)
        assertNull(ct.encoding)
    }

    @Test
    fun withCharset() {
        val ct = ContentType.create("text/html; charset=ascii")
        assertEquals("text/html", ct.mimeType)
        assertEquals("ascii", ct.encoding)
    }

    @Test
    fun withCharsetAndParameter() {
        val ct = ContentType.create("text/html; charset=ascii xxx=5555")
        assertEquals("text/html", ct.mimeType)
        assertEquals("ascii", ct.encoding)
    }
}