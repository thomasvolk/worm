package net.t53k.worm

class CrawlerBuilder {
    val _seeds = mutableListOf<String>()
    var _worker = 1

    fun seed(url: String): CrawlerBuilder {
        _seeds.add(url)
        return this
    }

    fun worker(workerCount: Int): CrawlerBuilder {
        if(workerCount < 1) { throw IllegalArgumentException("threadCount must be >= 1") }
        this._worker = workerCount
        return this
    }

    fun build(): Crawler {
        return Crawler(_seeds, _worker)
    }
}