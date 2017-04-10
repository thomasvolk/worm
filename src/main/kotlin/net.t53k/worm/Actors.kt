package net.t53k.worm

import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread
import kotlin.reflect.KClass

/**
 * Created by thomas on 07.04.17.
 */

object PoisonPill

class ActorSystem {
    private val actors = mutableMapOf<String, Actor>()

    fun <T> newActor(name: String, actorClass: KClass<T>): ActorReference where T : Actor {
        val actor = actorClass.java.newInstance()
        if(actors.contains(name)) { throw IllegalArgumentException("actor $name already exists") }
        actors.put(name, actor)
        actor.init(this)
        return actor.self()
    }

    fun actor(name: String): ActorReference { return actors.get(name)!!.self() }

    fun waitForAllActorsForShutdown() {
        actors.forEach { (name, actor) -> actor.waitForShutdown() }
    }
}

data class ActorMessageWrapper(val message: Any, val sender: ActorReference)

interface ActorReference {
    fun send(message: Any, sender: ActorReference)
}

object DummyActorReference: ActorReference {
    override fun send(message: Any, sender: ActorReference) {}
}

class ThreadActorReference(val actor: Actor): ActorReference {
    override fun send(message: Any, sender: ActorReference) {
        actor.send(message, sender)
    }
}

abstract class Actor {
    private val inbox = LinkedBlockingQueue<ActorMessageWrapper>(1000)
    private var running = true
    private val _self = ThreadActorReference(this)
    private var _sender: ActorReference? = null
    private var _thread: Thread? = null
    private var _system: ActorSystem? = null

    fun init(system: ActorSystem) {
        _system = system
        _sender = null
        _thread = thread(start = true) {
            while(running) {
                val (message, sender) = inbox.take()
                _sender = sender
                when(message) {
                    PoisonPill -> stop()
                    else -> receive(message)
                }
            }
        }
    }

    fun waitForShutdown() { _thread!!.join() }

    protected fun stop() {
        running = false
    }

    fun send(message: Any, sender: ActorReference) {
        inbox.offer(ActorMessageWrapper(message, sender))
    }

    protected fun system(): ActorSystem { return _system!! }

    protected fun sender(): ActorReference { return _sender!! }

    fun self(): ActorReference {return _self }

    abstract fun receive(message: Any)
}

