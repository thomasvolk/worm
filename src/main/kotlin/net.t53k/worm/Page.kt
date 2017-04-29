package net.t53k.worm

import org.jsoup.Jsoup

data class Page(val url: String, val body: String, val links: Collection<String>) {
  companion object Parser {
    fun parse(url: String, body: String): Page {
      val links = Jsoup.parse(body).select("a").map { it.attr("href") }
      return Page(url, body, links)
    }
  }
}

