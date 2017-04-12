package net.t53k.worm


import org.junit.Test
import org.junit.Assert.*

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
                system().get("pong").send(Ping)
                starter = sender()
            }
            Stop -> {
                sender().send(PoisonPill)
                self().send(PoisonPill)
                starter!!.send(lastPongId)
            }
            is Pong -> {
                lastPongId = message.id
                sender().send(Ping)
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
                if(count < 100) sender().send(Pong(count))
                else sender().send(Stop)
            }
        }
    }
}


class ActorTest {
    @Test
    fun pingPong() {
        var success = false
        val system = ActorSystem()
        val ping = system.actor("ping", PingActor::class)
        system.actor("pong", PongActor::class)
        system.current(object: ActorReference {
            override fun send(message: Any) {
                when(message) {
                    is Int -> {
                        assertEquals(message, 99)
                        success = true
                    }
                    else -> fail("wrong message format: $message")
                }
            }
        })
        ping.send(Start)
        system.waitForShutdown()
        assertTrue(success)
    }
}