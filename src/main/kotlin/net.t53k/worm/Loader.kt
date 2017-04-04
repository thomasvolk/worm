package net.t53k.worm

interface Loader {
    fun load(url: String): String
}