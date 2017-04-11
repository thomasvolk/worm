package net.t53k.worm

import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread
import kotlin.reflect.KClass

object PoisonPill

class ActorSystem {
    private val _actors = mutableMapOf<String, ActorReference>()
    private val _currentActor = ThreadLocal<ActorReference>()

    fun <T> actor(name: String, actorClass: KClass<T>): ActorReference where T : Actor {
        if(_actors.contains(name)) { throw IllegalArgumentException("actor $name already exists") }
        val actorRef = actorClass.java.newInstance().start(this)
        _actors.put(name, actorRef)
        return actorRef
    }

    fun get(name: String): ActorReference { return _actors.get(name)!! }

    fun current(): ActorReference { return _currentActor.get() }

    fun current(actor: ActorReference) {
        _currentActor.set(actor)
    }

    fun waitForShutdown() {
        _actors.forEach { (name, actor) -> actor.waitForShutdown() }
    }
}

data class ActorMessageWrapper(val message: Any, val sender: ActorReference)

interface ActorReference {
    fun send(message: Any)

    fun waitForShutdown()
}

class ThreadActorReference(val system: ActorSystem, val actor: Actor): ActorReference {
    override fun send(message: Any) {
        actor.send(message, system.current())
    }

    override fun waitForShutdown() {
        actor.waitForShutdown()
    }
}

abstract class Actor {
    private val _inbox = LinkedBlockingQueue<ActorMessageWrapper>()
    private var _running = true
    private var _self: ThreadActorReference? = null
    private var _sender: ActorReference? = null
    private var _thread: Thread? = null

    fun start(system: ActorSystem): ActorReference {
        _sender = null
        _self = ThreadActorReference(system, this)
        _thread = thread(start = true) {
            system().current(self())
            while(_running) {
                val (message, sender) = _inbox.take()
                _sender = sender
                when(message) {
                    PoisonPill -> stop()
                    else -> receive(message)
                }
            }
        }
        return self()
    }

    fun waitForShutdown() { _thread!!.join() }

    protected fun stop() {
        _running = false
    }

    fun send(message: Any, sender: ActorReference) {
        _inbox.offer(ActorMessageWrapper(message, sender))
    }

    protected fun system(): ActorSystem { return _self!!.system }

    protected fun sender(): ActorReference { return _sender!! }

    protected fun self(): ActorReference {return _self!! }

    protected abstract fun receive(message: Any)
}

