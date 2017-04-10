package net.t53k.worm


import org.junit.Test
import org.junit.Assert.*

/**
 * Created by thomas on 10.04.17.
 */

object Start
object Stop
object Ping
data class Pong(val id: Int)

class PingActor: Actor() {
    private var lastPongId = 0
    private var starter: ActorReference? = null
    override fun receive(message: Any) {
        when(message) {
            Start -> {
                system().actor("pong").send(Ping, self())
                starter = sender()
            }
            Stop -> {
                sender().send(PoisonPill, self())
                self().send(PoisonPill, self())
                starter!!.send(lastPongId, self())
            }
            is Pong -> {
                lastPongId = message.id
                sender().send(Ping, self())
            }
        }
    }
}

class PongActor: Actor() {
    private var count = 0
    override fun receive(message: Any) {
        when(message) {
            Ping -> {
                count++
                if(count < 100) sender().send(Pong(count), self())
                else sender().send(Stop, self())
            }
        }
    }
}


class ActorTest {
    @Test
    fun pingPong() {
        val system = ActorSystem()
        val ping = system.newActor("ping", PingActor::class)
        system.newActor("pong", PongActor::class)
        ping.send(Start, object: ActorReference {
            override fun send(message: Any, sender: ActorReference) {
                when(message) {
                    is Int -> assertEquals(message, 99)
                    else -> fail("wrong message format: $message")
                }
            }
        })
        system.waitForAllActorsForShutdown()
    }
}