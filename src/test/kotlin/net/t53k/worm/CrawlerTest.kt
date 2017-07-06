package net.t53k.worm

import org.junit.Assert.assertEquals
import org.junit.Test

class Pacemaker(urls: List<String>) : Timeout {
    private val expectedUrls = urls.toMutableList()
    private lateinit var callback: () -> Unit

    @Synchronized
    fun pace(url: String) {
        if(expectedUrls.remove(url)) {
            if(expectedUrls.isEmpty()) {
                callback()
            }
        }
    }

    override fun start(callback: () -> Unit) {
        this.callback = callback
    }

}

class CrawlerTest {
    val base = "pages/tree"
    val testClass = javaClass
    val pageLoader: (String) -> String = { url -> testClass.getResourceAsStream("$base/$url").bufferedReader().use { it.readText() } }
    val linkFilter: (String) -> Boolean = { l -> !l.contains("filterthis") }

    @Test
    fun crawler() {
        val worker = 4
        val pages = mutableSetOf<Page>()
        val errorUrls = mutableListOf<String>()

        val crawler = CrawlerBuilder().worker(worker)
                .onPage { page -> pages += page }
                .onError { url -> errorUrls += url  }
                .pageLoader(pageLoader)
                .withLinkFilter(linkFilter)
                .build()
        val pendigPages = crawler.start(listOf("index.html"))

        assertEquals(listOf<String>(), pendigPages)
        assertEquals(
                listOf("index.html", "subpage.01.a.html", "subpage.01.b.html", "subpage.02.a.html").sorted(),
                pages.map { it.url }.sorted())
        assertEquals(listOf("notfound.html"), errorUrls.sorted())
    }

    @Test
    fun crawlerTimeout() {
        (1..50).forEach { _ ->
            val worker = 4
            val pages = mutableSetOf<Page>()
            val errorUrls = mutableListOf<String>()
            val pacemaker = Pacemaker(listOf("index.html", "subpage.01.a.html"))

            val crawler = CrawlerBuilder().worker(worker)
                    .onPage { page ->
                        pages += page
                        pacemaker.pace(page.url)
                    }
                    .onError { url -> errorUrls += url }
                    .pageLoader { url -> pageLoader(url) }
                    .withLinkFilter(linkFilter)
                    .build()
            val pendigPages = crawler.start(listOf("index.html"), pacemaker)

            //assertEquals(listOf("subpage.01.b.html", "subpage.01.a.html", "notfound.html").sorted(), pendigPages.sorted())
            val pagesProcessed = pages.map { it.url }
            val pagesTotal = (pendigPages + pagesProcessed).filter { url -> url != "notfound.html"}.toSet()
            assertEquals(
                    setOf("index.html", "subpage.01.a.html", "subpage.01.b.html", "subpage.02.a.html").sorted(),
                    pagesTotal.sorted())
        }
    }
}
