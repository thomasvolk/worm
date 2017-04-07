package net.t53k.worm

import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

/**
 * Created by thomas on 07.04.17.
 */

object PoisonPill

object Actors {
    fun <T> actor(actorClass: Class<T>): ActorReference where T : Actor {
        return actorClass.newInstance().self()
    }
}

class ActorReference(val inbox: LinkedBlockingQueue<Any>) {
    fun send(message: Any) {
        inbox.put(message)
    }
}

abstract class Actor {
    private val inbox = LinkedBlockingQueue<Any>()
    private var running = true
    private val reference = ActorReference(inbox)
    init {
        val thread = thread(start = true) {
            while(running) {
                val message = inbox.take()
                when(message) {
                    PoisonPill -> stop()
                    else -> receive(message)
                }
            }
        }
    }

    protected fun stop() {
        running = false
    }

    fun self(): ActorReference {return reference}

    abstract fun receive(message: Any)
}

