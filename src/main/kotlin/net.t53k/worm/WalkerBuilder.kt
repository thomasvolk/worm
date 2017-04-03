package net.t53k.worm

import net.t53k.worm.walker.UrlWalker

class WalkerBuilder {
    fun seed(url: String) {

    }
    fun onPage(predicate: (Page) -> Void) {

    }

    fun build(): Walker {
        return UrlWalker()
    }
}