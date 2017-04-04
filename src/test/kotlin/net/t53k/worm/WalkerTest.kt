package net.t53k.worm

import org.junit.Test

class TestLoader(val base: String) : Loader {
  override fun load(url: String): String {
    return javaClass.getResourceAsStream("$base/$url").bufferedReader().use { it.readText() }
  }
}

class WalkerTest {

  @Test
  fun treeWalk() {
    val w = WalkerBuilder().seed("index.html").build()
    val loader = TestLoader("pages/tree")
    w.run(loader, { println(it.url) } )
  }

}
