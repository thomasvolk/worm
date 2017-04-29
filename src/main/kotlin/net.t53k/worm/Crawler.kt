package net.t53k.worm

import net.t53k.alkali.Actor
import net.t53k.alkali.ActorReference
import net.t53k.alkali.router.Broadcast
import net.t53k.alkali.router.RoundRobinRouter
import java.net.URL
import java.nio.charset.Charset

data class LoadPage(var url: String)
data class ProcessPage(var page: Page)
object Done
data class Start(val url: String)

class WorkDispatcher(val pageHandler: ActorReference, val pageLoaderWorker: List<ActorReference>): Actor() {
    private lateinit var router: ActorReference
    private val pagesPending = mutableSetOf<String>()

    override fun before() {
        router = actor("workerRouter", RoundRobinRouter(pageLoaderWorker))
    }

    override fun receive(message: Any) {
        when(message) {
            is Start -> {
                pagesPending += message.url
                router send LoadPage(message.url)
            }
            is ProcessPage -> {
                pagesPending -= message.page.url
                val page = message.page
                pageHandler send page
                page.links.forEach{
                    if(!pagesPending.contains(it)) {
                        pagesPending += it
                        router send LoadPage(it)
                    }
                }
                if(pagesPending.isEmpty()) {
                    pageHandler send Done
                }
            }
        }
    }
}

open class PagerLoader(val charset: Charset = Charsets.UTF_8): Actor() {
    override fun receive(message: Any) {
        when(message) {
            is LoadPage -> sender()!! send ProcessPage(createPage(message))
        }
    }

    open protected fun createPage(message: LoadPage) = Page.parse(message.url, load(message.url))

    open protected fun load(url: String) = URL(url).readText(charset)

}
