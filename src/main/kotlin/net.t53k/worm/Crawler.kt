package net.t53k.worm

import net.t53k.alkali.ActorSystemBuilder
import net.t53k.worm.actors.*
import java.net.URL

class Crawler(val onPage: (Page) -> Unit,
              val worker: Int,
              val pageLoader: (String) -> String,
              val linkFilter: (String) -> Boolean,
              val errorHandler: (LoadPageError) -> Unit) {

    fun start(url: String) {
        val system = ActorSystemBuilder().onDefaultActorMessage { message ->
            when(message) {
                is Done -> shutdown()
            }
        }.build()
        val dispatcher = system.actor("worm/dispatcher", WorkDispatcher(onPage = onPage, worker = worker, pageLoader = pageLoader,
                linkFilter = linkFilter, errorHandler = errorHandler))
        dispatcher send Start(url)
        system.waitForShutdown()
    }
}

class CrawlerBuilder {
    private var onPage: (Page) -> Unit = { page -> }
    private var worker: Int = 4
    private var pageLoader: (String) -> String = { url -> URL(url).readText(Charsets.UTF_8) }
    private var linkFilter: (String) -> Boolean = { link -> true }
    private var errorHandler: (LoadPageError) -> Unit = { err -> }

    fun onPage(handler: (Page) -> Unit): CrawlerBuilder {
        onPage = handler
        return this
    }

    fun worker(count: Int): CrawlerBuilder {
        worker = count
        return this
    }

    fun pageLoader(handler: (String) -> String): CrawlerBuilder {
        pageLoader = handler
        return this
    }

    fun linkFilter(handler: (String) -> Boolean): CrawlerBuilder {
        linkFilter = handler
        return this
    }

    fun onLoadPageError(handler: (LoadPageError) -> Unit): CrawlerBuilder {
        errorHandler = handler
        return this
    }

    fun build() = Crawler(onPage = onPage, worker = worker, pageLoader = pageLoader,
            linkFilter = linkFilter, errorHandler = errorHandler)
}