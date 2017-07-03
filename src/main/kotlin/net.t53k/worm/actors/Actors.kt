package net.t53k.worm.actors

import net.t53k.alkali.Actor
import net.t53k.alkali.ActorReference
import net.t53k.alkali.router.RoundRobinRouter
import net.t53k.worm.Page
import java.net.URL

data class LoadPage(var url: String)
data class ProcessPage(var page: Page)
object Done
data class Start(val url: String)
data class LoadPageError(val url: String, val type: Class<out Exception>)

class PageHandler(val onPage: (Page) -> Unit) : Actor() {
    override fun receive(message: Any) {
        when (message) {
            is Page -> onPage(message)
        }
    }
}

class PagerLoader(val pageLoader: (String) -> String): Actor() {
    override fun receive(message: Any) {
        when(message) {
            is LoadPage -> {
                try {
                    val cnt = pageLoader(message.url)
                    sender() send ProcessPage(Page.parse(message.url, cnt))
                } catch (e: Exception) {
                    sender() send LoadPageError(message.url, e.javaClass)
                }
            }
        }
    }
}

class WorkDispatcher(val onPage: (Page) -> Unit,
                     val worker: Int,
                     val pageLoader: (String) -> String,
                     val linkFilter: (String) -> Boolean,
                     val errorHandler: (LoadPageError) -> Unit): Actor() {
    private lateinit var pageLoaderWorker: List<ActorReference>
    private lateinit var pageHandler: ActorReference
    private lateinit var router: ActorReference
    private lateinit var starter: ActorReference
    private val pagesPending = mutableSetOf<String>()

    override fun before() {
        pageHandler = actor("worm/pageHandler", PageHandler(onPage))
        pageLoaderWorker = (1..worker).map { actor("worm/worker$it", PagerLoader(pageLoader)) }
        router = actor("worm/workerRouter", RoundRobinRouter(pageLoaderWorker))
    }

    override fun receive(message: Any) {
        when(message) {
            is Start -> {
                starter = sender()
                pagesPending += message.url
                router send LoadPage(message.url)
            }
            is ProcessPage -> {
                pagesPending -= message.page.url
                val page = message.page
                pageHandler send page
                page.links.filter(linkFilter).filter { !pagesPending.contains(it) }.forEach{
                    pagesPending += it
                    router send LoadPage(it)
                }
            }
            is LoadPageError -> {
                pagesPending -= message.url
                errorHandler(message)
            }
        }
        if(pagesPending.isEmpty()) {
            starter send Done
        }
    }
}
