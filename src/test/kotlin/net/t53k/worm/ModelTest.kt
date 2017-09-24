package net.t53k.worm

import org.junit.Assert.*
import org.junit.Test
import java.nio.charset.Charset

class ModelTest {
    @Test
    fun stringRepresentations() {
        val body = Body("text".toByteArray(Charset.forName("UTF-8")), ContentType(ContentType.DEFAULT_CONTENT_TYPE))
        val bodyRepresentation = body.toString()
        assertEquals("Body(contentType=ContentType(contentType=application/octet-stream), bytes=4)", bodyRepresentation)
        val document = Document(Resource("http://example.com", body), listOf())
        assertEquals("Document(resource='Resource(url=http://example.com, body=$body)', linkCount=0)", document.toString())
    }
}