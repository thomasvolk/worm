package net.t53k.worm

import net.t53k.alkali.test.actorTest
import org.junit.Assert.assertEquals
import org.junit.Test




class CrawlerTest {

    @Test
    fun treeWalk() {
        val worker = 4
        val base = "pages/tree"
        val pages = mutableSetOf<Page>()
        val errorUrls = mutableListOf<String>()
        val testClass = javaClass
        actorTest { _ ->
                val dispatcher = actor("dispatcher", WorkDispatcher({ page -> pages += page }, worker,
                        {url -> testClass.getResourceAsStream("$base/$url").bufferedReader().use { it.readText() }},
                        linkFilter = { l -> !l.contains("filterthis")},
                        errorHandler = { e -> errorUrls += e.url }))
                dispatcher send Start("index.html")

                onMessage {
                    assertEquals(Done, it)
                    assertEquals(
                            listOf("index.html", "subpage.01.a.html", "subpage.01.b.html", "subpage.02.a.html").sorted(),
                            pages.map { it.url }.sorted())
                    assertEquals(listOf("notfound.html"), errorUrls.sorted())
                }
        }
    }
}
