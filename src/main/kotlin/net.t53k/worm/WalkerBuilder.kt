package net.t53k.worm

import net.t53k.worm.walker.SimpleWalker

class WalkerBuilder {
    val seeds = mutableListOf<String>()

    fun seed(url: String): WalkerBuilder {
        seeds.add(url)
        return this
    }

    fun build(): Walker {
        return SimpleWalker(seeds)
    }
}