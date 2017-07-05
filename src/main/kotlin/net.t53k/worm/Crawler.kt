package net.t53k.worm

import net.t53k.alkali.ActorSystemBuilder
import net.t53k.alkali.PoisonPill
import net.t53k.worm.actors.*
import java.net.URL

class Crawler(val onPage: (Page) -> Unit,
              val worker: Int,
              val pageLoader: (String) -> String,
              val linkFilter: (String) -> Boolean,
              val errorHandler: (String) -> Unit) {

    fun start(urls: List<String>, timeout: Timeout = InfinityTimeout): List<String> {
        val pendingPages = mutableListOf<String>()
        val system = ActorSystemBuilder().onDefaultActorMessage { message ->
            when(message) {
                is Done -> {
                    pendingPages += message.pagesPending
                    shutdown()
                }
            }
        }.build()
        val dispatcher = system.actor("worm/dispatcher", WorkDispatcher(onPage = onPage, worker = worker, pageLoader = pageLoader,
                linkFilter = linkFilter, errorHandler = errorHandler))
        dispatcher send Start(urls)
        timeout.start { dispatcher send PoisonPill }
        system.waitForShutdown()
        return pendingPages.toList()
    }
}

interface Timeout {
    fun start(callback: () -> Unit): Unit
}

object InfinityTimeout : Timeout {
    override fun start(callback: () -> Unit) { /* this will never run the callback */ }
}

class MilliSecondsTimeout(val durationMs: Long) : Timeout {
    override fun start(callback: () -> Unit) {
        Thread.sleep(durationMs)
        callback()
    }
}

class CrawlerBuilder {
    private var onPage: (Page) -> Unit = { _ -> }
    private var worker: Int = 4
    private var pageLoader: (String) -> String = { url -> URL(url).readText(Charsets.UTF_8) }
    private var linkFilter: (String) -> Boolean = { _ -> true }
    private var errorHandler: (String) -> Unit = { _ -> }

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

    fun withLinkFilter(handler: (String) -> Boolean): CrawlerBuilder {
        linkFilter = handler
        return this
    }

    fun onError(handler: (String) -> Unit): CrawlerBuilder {
        errorHandler = handler
        return this
    }

    fun build() = Crawler(onPage = onPage, worker = worker, pageLoader = pageLoader,
            linkFilter = linkFilter, errorHandler = errorHandler)
}