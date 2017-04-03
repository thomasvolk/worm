package net.t53k.worm

import net.t53k.worm.walker.UrlWalker

class WalkerBuilder {
    fun seed(url: String): WalkerBuilder {
        return this
    }
    fun onPage(predicate: (Page) -> Void): WalkerBuilder {
        return this
    }

    fun build(): Walker {
        return UrlWalker()
    }
}