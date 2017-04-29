package net.t53k.worm

import net.t53k.alkali.Actor
import net.t53k.alkali.ActorReference
import net.t53k.alkali.router.Broadcast
import net.t53k.alkali.router.RoundRobinRouter
import java.net.URL
import java.nio.charset.Charset

data class LoadUrl(var url: String)
data class ProcessPage(var page: Page)
object Stop

class WorkDispatcher(val pageHandler: ActorReference, val pageLoaderWorker: List<ActorReference>): Actor() {
    private lateinit var router: ActorReference
    private val pagesPending = mutableSetOf<String>()

    override fun before() {
        router = actor("workerRouter", RoundRobinRouter(pageLoaderWorker))
    }

    override fun receive(message: Any) {
        when(message) {
            is LoadUrl -> {
                pagesPending += message.url
                router send message
            }
            is ProcessPage -> {
                pagesPending -= message.page.url
                val page = message.page
                pageHandler send page
                page.links.forEach{
                    if(!pagesPending.contains(it)) {
                        pagesPending += it
                        router send LoadUrl(it)
                    }
                }
                if(pagesPending.isEmpty()) {
                    router send Broadcast(Stop)
                    pageHandler send Stop
                    stop()
                }
            }
        }
    }
}

open class PagerLoader(val charset: Charset = Charsets.UTF_8): Actor() {
    override fun receive(message: Any) {
        when(message) {
            is LoadUrl -> sender()!! send ProcessPage(createPage(message))
            Stop -> stop()
        }
    }

    open protected fun createPage(message: LoadUrl) = Page.parse(message.url, load(message.url))

    open protected fun load(url: String) = URL(url).readText(charset)

}
