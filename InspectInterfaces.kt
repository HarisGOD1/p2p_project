import io.libp2p.core.Connection
import io.libp2p.core.P2PChannel
import io.libp2p.core.transport.Transport

fun main() {
    println("--- Connection ---")
    Connection::class.java.methods.forEach { println(it) }
    println("\n--- P2PChannel ---")
    P2PChannel::class.java.methods.forEach { println(it) }
    println("\n--- Transport ---")
    Transport::class.java.methods.forEach { println(it) }
}
