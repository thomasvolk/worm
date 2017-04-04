package net.t53k.worm.walker

import net.t53k.worm.Loader
import net.t53k.worm.Page
import net.t53k.worm.Walker
import net.t53k.worm.parse

class SimpleWalker(val seeds: Collection<String>) : Walker {
    override fun run(loader: Loader, pageHandler: (Page) -> Unit) {
        seeds.forEach{ processPage(it, loader, pageHandler) }
    }

    private fun  processPage(url: String, loader: Loader, pageHandler: (Page) -> Unit) {
        val page = Page.parse(url, loader.load(url))
        pageHandler.invoke(page)
        page.links.forEach{ processPage(it, loader, pageHandler) }
    }
}