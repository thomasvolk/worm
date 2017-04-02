package net.t53k.worm

interface Walker {
    fun walk(seed: Collection<Page>, predicate: (Page) -> Boolean)
}