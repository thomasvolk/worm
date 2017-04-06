package net.t53k.worm

class Crawler(val seeds: Collection<String>) {
    fun run(loader: UrlResolver, pageHandler: (Page) -> Unit) {
        seeds.forEach{ processPage(it, loader, pageHandler) }
    }

    private fun  processPage(url: String, loader: UrlResolver, pageHandler: (Page) -> Unit) {
        val page = Page.parse(url, loader.resolve(url))
        pageHandler.invoke(page)
        page.links.forEach{ processPage(it, loader, pageHandler) }
    }
}