package net.t53k.worm.crawler

import net.t53k.alkali.Actor
import net.t53k.alkali.ActorReference
import net.t53k.alkali.ActorSystem
import net.t53k.alkali.actors.Reaper
import net.t53k.alkali.router.Broadcast
import net.t53k.alkali.router.RoundRobinRouter
import net.t53k.worm.Page
import net.t53k.worm.Resolver

data class LoadUrl(var url: String)
data class ProcessPage(var page: Page)
object Stop

class WorkDispatcher(val pageHandler: (Page) -> Unit, val pageLoaderWorker: List<ActorReference>): Actor() {
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
                pageHandler.invoke(page)
                page.links.forEach{
                    if(!pagesPending.contains(it)) {
                        pagesPending += it
                        router send LoadUrl(it)
                    }
                }
                if(pagesPending.isEmpty()) {
                    router send Broadcast(Stop)
                    stop()
                }
            }
        }
    }
}

class PagerLoaderWorker(val loader: Resolver): Actor() {
    override fun receive(message: Any) {
        when(message) {
            is LoadUrl -> sender()!! send ProcessPage(Page.parse(message.url, loader.resolve(message.url)))
            Stop -> stop()
        }
    }

}

class Crawler(val seeds: Collection<String>, val worker: Int) {

    fun run(loader: Resolver, pageHandler: (Page) -> Unit) {
        val actorSystem = ActorSystem()
        actorSystem.actor("reaper", Reaper({
            val pageLoaderWorker = (1..worker).map { actor("worker$it", PagerLoaderWorker(loader)) }
            val dispatcher = actor("dispatcher", WorkDispatcher(pageHandler, pageLoaderWorker))
            seeds.forEach{ dispatcher send LoadUrl(it) }
        }))
        actorSystem.waitForShutdown()
    }

}