package su.kamil.dev.implementations.transport.udp

import io.libp2p.core.ChannelVisitor
import io.libp2p.core.Connection
import io.libp2p.core.ConnectionHandler
import io.libp2p.core.P2PChannel
import io.libp2p.core.multiformats.Multiaddr
import io.libp2p.core.transport.Transport
import java.util.concurrent.CompletableFuture

class Pcap4JUdpTransport: Transport {

    // contains information about addresses, which are reach each other
    // prefer first is destination addr
    // second is source addr
    // so, at any moment you just UdpPcap4JForge&Post(udpHoles[dest],dest,data..etc)
    private val updHoles = mapOf<Multiaddr, Multiaddr>()

    // as UDP is stateless and connectionless (but are active holes maybe consider as connections?)
    override val activeConnections: Int
        get() = 0
    override val activeListeners: Int
        get() = 0

    override fun close(): CompletableFuture<Unit> {
        TODO("Not yet implemented")
    }

    override fun dial(
        addr: Multiaddr,
        connHandler: ConnectionHandler,
        preHandler: ChannelVisitor<P2PChannel>?
    ): CompletableFuture<Connection> {
        TODO("Not yet implemented")
    }

    override fun handles(addr: Multiaddr): Boolean {
        TODO("Not yet implemented")
    }

    override fun initialize() {
        TODO("Not yet implemented")
    }

    override fun listen(
        addr: Multiaddr,
        connHandler: ConnectionHandler,
        preHandler: ChannelVisitor<P2PChannel>?
    ): CompletableFuture<Unit> {
        TODO("Not yet implemented")
    }

    override fun listenAddresses(): List<Multiaddr> {
        TODO("Not yet implemented")
    }

    override fun unlisten(addr: Multiaddr): CompletableFuture<Unit> {
        TODO("Not yet implemented")
    }
}