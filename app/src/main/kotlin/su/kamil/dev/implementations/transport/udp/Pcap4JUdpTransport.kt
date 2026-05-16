package su.kamil.dev.implementations.transport.udp

import io.libp2p.core.ChannelVisitor
import io.libp2p.core.Connection
import io.libp2p.core.ConnectionHandler
import io.libp2p.core.P2PChannel
import io.libp2p.core.multiformats.Multiaddr
import io.libp2p.core.multiformats.Protocol
import io.libp2p.core.transport.Transport
import org.pcap4j.packet.IpV4Packet
import org.pcap4j.packet.UdpPacket
import org.pcap4j.packet.namednumber.IpNumber
import su.kamil.dev.forge_n_post.post.IpPcap4JPostService
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

class Pcap4JUdpTransport : Transport {

    private val connections = ConcurrentHashMap<Multiaddr, Pcap4JConnection>()
    private val listeners = ConcurrentHashMap<Multiaddr, CompletableFuture<Unit>>()
    private var listeningThread: Thread? = null

    override val activeConnections: Int
        get() = connections.size
    override val activeListeners: Int
        get() = listeners.size

    override fun close(): CompletableFuture<Unit> {
        connections.values.forEach { it.close() }
        listeningThread?.interrupt()
        return CompletableFuture.completedFuture(Unit)
    }

    override fun dial(
        addr: Multiaddr,
        connHandler: ConnectionHandler,
        preHandler: ChannelVisitor<P2PChannel>?
    ): CompletableFuture<Connection> {
        val conn = Pcap4JConnection(
            this,
            listeners.keys.firstOrNull() ?: Multiaddr("/ip4/127.0.0.1/udp/0"),
            addr,
            true
        )
        connections[addr] = conn

        val future = CompletableFuture<Connection>()

        preHandler?.visit(conn)
        connHandler.handleConnection(conn)
        future.complete(conn)

        return future
    }

    override fun handles(addr: Multiaddr): Boolean {
        return addr.components.any { it.protocol == Protocol.UDP }
    }

    override fun initialize() {
        if (listeningThread == null) {
            listeningThread = thread(isDaemon = true, name = "PcapSniffer") {
                try {
                    val handle = IpPcap4JPostService.handle
                    handle.loop(-1, org.pcap4j.core.PacketListener { packet ->
                        val ipPacket = packet.get(IpV4Packet::class.java)
                        if (ipPacket != null && ipPacket.header.protocol == IpNumber.UDP) {
                            val udpPacket = ipPacket.get(UdpPacket::class.java)
                            if (udpPacket != null) {
                                val srcIp = ipPacket.header.srcAddr.hostAddress
                                val srcPort = udpPacket.header.srcPort.valueAsInt()
                                val remoteAddr = Multiaddr("/ip4/$srcIp/udp/$srcPort")
                                
                                val conn = connections[remoteAddr]
                                if (conn != null) {
                                    val payload = udpPacket.payload
                                    if (payload != null) {
                                        conn.receiveRaw(payload.rawData)
                                    }
                                } else {
                                    // Potential new incoming connection
                                    // For simplicity in this example, we don't auto-accept yet
                                }
                            }
                        }
                    })
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun listen(
        addr: Multiaddr,
        connHandler: ConnectionHandler,
        preHandler: ChannelVisitor<P2PChannel>?
    ): CompletableFuture<Unit> {
        val future = CompletableFuture<Unit>()
        listeners[addr] = future
        initialize()
        future.complete(Unit)
        return future
    }

    override fun listenAddresses(): List<Multiaddr> {
        return listeners.keys().toList()
    }

    override fun unlisten(addr: Multiaddr): CompletableFuture<Unit> {
        listeners.remove(addr)
        return CompletableFuture.completedFuture(Unit)
    }
}
