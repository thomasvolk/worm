package net.t53k.worm

interface UrlResolver {
    fun resolve(url: String): String
}