package net.t53k.worm

class CrawlerBuilder {
    val seeds = mutableListOf<String>()
    var workerCount = 1

    fun seed(url: String): CrawlerBuilder {
        seeds.add(url)
        return this
    }

    fun threads(workerCount: Int): CrawlerBuilder {
        if(workerCount < 1) { throw IllegalArgumentException("threadCount must be >= 1") }
        this.workerCount = workerCount
        return this
    }

    fun build(): Crawler {
        return Crawler(seeds, workerCount)
    }
}