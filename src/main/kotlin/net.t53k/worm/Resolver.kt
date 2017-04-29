package net.t53k.worm

import java.net.URL
import java.nio.charset.Charset

interface Resolver {
    fun resolve(url: String): String
}

class URLResolver(val charset: Charset = Charsets.UTF_8): Resolver {
    override fun resolve(url: String): String = URL(url).readText(charset)
}