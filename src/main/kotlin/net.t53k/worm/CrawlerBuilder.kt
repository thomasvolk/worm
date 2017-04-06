package net.t53k.worm

class CrawlerBuilder {
    val seeds = mutableListOf<String>()

    fun seed(url: String): CrawlerBuilder {
        seeds.add(url)
        return this
    }

    fun build(): Crawler {
        return Crawler(seeds)
    }
}