package net.t53k.worm

import org.junit.Assert.assertEquals
import org.junit.Test

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
}
