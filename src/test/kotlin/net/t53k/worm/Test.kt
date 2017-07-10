package net.t53k.worm

/**
 * Created by thomas on 10.07.17.
 */


fun main(args: Array<String>) {
    val pages = mutableSetOf<Page>()
    val crawler = CrawlerBuilder()
            .worker(4)
            .onPage { page -> pages += page }
            .withLinkFilter { it.startsWith("http://example.com") }
            .build()
    val pendigPages = crawler.start(listOf("http://example.com"), MilliSecondsTimeout(5000))
    println(pages)
}