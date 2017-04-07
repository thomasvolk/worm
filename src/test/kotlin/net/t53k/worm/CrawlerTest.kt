package net.t53k.worm

import org.junit.Test
import org.junit.Assert.*

class TestResolver(val base: String) : UrlResolver {
  override fun resolve(url: String): String {
    return javaClass.getResourceAsStream("$base/$url").bufferedReader().use { it.readText() }
  }
}

class TestPageReceiver {
  private val pages = mutableListOf<Page>()
  fun receive(page: Page): Unit {
    pages.add(page)
  }

  fun getPages(): List<Page> {
    return pages.toList()
  }
}

class CrawlerTest {

  @Test
  fun treeWalk() {
    val w = CrawlerBuilder().seed("index.html").build()
    val loader = TestResolver("pages/tree")
    val receiver = TestPageReceiver()
    w.run(loader, receiver::receive)
    val result = receiver.getPages().sortedBy { it.url }
    assertEquals(result.map { it.url }.toList(),
            listOf("index.html", "subpage.01.a.html", "subpage.01.b.html", "subpage.02.a.html", "subpage.02.a.html"))
  }
}
