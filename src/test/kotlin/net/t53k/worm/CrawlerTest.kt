package net.t53k.worm

import net.t53k.alkali.Actor
import net.t53k.alkali.ActorReference
import net.t53k.alkali.actors.Reaper
import net.t53k.alkali.test.actorTest
import org.junit.Assert.assertEquals
import org.junit.Test




class CrawlerTest {

    class TestPagerLoader(val base: String) : PagerLoader() {
        override fun load(url: String): String {
            return javaClass.getResourceAsStream("$base/$url").bufferedReader().use { it.readText() }
        }
    }

    class PageHandler(val testResultReceiver: ActorReference) : Actor() {
        private val pages = mutableListOf<Page>()

        override fun receive(message: Any) {
            when (message) {
                is Page -> pages += message
                Done -> testResultReceiver send pages.map { it.url }.toList().sorted()
            }
        }
    }

    @Test
    fun treeWalk() {
        val worker = 4
        actorTest { testActor ->

                val pageHandler = actor("pageHandler", PageHandler(testActor))
                val pageLoaderWorker = (1..worker).map { actor("worker$it", TestPagerLoader("pages/tree")) }
                val dispatcher = actor("dispatcher", WorkDispatcher(pageHandler, pageLoaderWorker))
                dispatcher send Start("index.html")

                onMessage {
                    assertEquals(
                            listOf("index.html", "subpage.01.a.html", "subpage.01.b.html", "subpage.02.a.html").sorted(),
                            it)
                }
        }
    }
}
