package su.kamil.dev.implementations.transport.udp

import io.libp2p.core.Connection
import io.libp2p.core.multiformats.Multiaddr
import io.libp2p.core.multiformats.Protocol
import io.libp2p.core.mux.StreamMuxer
import io.libp2p.core.security.SecureChannel
import io.libp2p.core.transport.Transport
import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.channel.embedded.EmbeddedChannel
import org.pcap4j.packet.namednumber.IpNumber
import su.kamil.dev.forge_n_post.forge.IpPcap4JForgeService
import su.kamil.dev.forge_n_post.post.IpPcap4JPostService
import su.kamil.dev.forge_n_post.transport.UdpPcap4JForgeAndPostService
import java.net.Inet4Address
import java.util.concurrent.CompletableFuture

class Pcap4JConnection(
    private val transport: Transport,
    private val localAddr: Multiaddr,
    private val remoteAddr: Multiaddr,
    override val isInitiator: Boolean
) : Connection {
    val channel = EmbeddedChannel()
    private val udpForge = UdpPcap4JForgeAndPostService()
    private val ipForge = IpPcap4JForgeService()

    var spoofedSrcIp: String? = null
    var spoofedSrcPort: Int? = null

    private val localHost = localAddr.components.find { it.protocol == Protocol.IP4 }?.stringValue ?: "127.0.0.1"
    private val localPort = localAddr.components.find { it.protocol == Protocol.UDP }?.stringValue?.toInt() ?: 0
    private val remoteHost = remoteAddr.components.find { it.protocol == Protocol.IP4 }?.stringValue ?: "127.0.0.1"
    private val remotePort = remoteAddr.components.find { it.protocol == Protocol.UDP }?.stringValue?.toInt() ?: 0

    init {
        channel.pipeline().addLast(object : ChannelOutboundHandlerAdapter() {
            override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
                if (msg is ByteBuf) {
                    val bytes = ByteArray(msg.readableBytes())
                    msg.readBytes(bytes)
                    sendRaw(bytes)
                    msg.release()
                    promise.setSuccess()
                } else {
                    super.write(ctx, msg, promise)
                }
            }
        })
    }

    private fun sendRaw(data: ByteArray) {
        try {
            val srcIpHost = spoofedSrcIp ?: localHost
            val srcPortValue = spoofedSrcPort ?: localPort
            val srcIp = Inet4Address.getByName(srcIpHost) as Inet4Address
            val dstIp = Inet4Address.getByName(remoteHost) as Inet4Address
            
            val udpPacket = udpForge.forgeUdpPacket(
                srcIp, dstIp, srcPortValue.toShort(), remotePort.toShort(), data
            )
            val ipPacket = ipForge.forgeIpPacket(
                srcIp, dstIp, IpNumber.UDP, 64.toByte(), 0.toShort(), 0.toByte(), 0.toShort(), udpPacket.rawData
            )
            
            // Note: If on Ethernet, this might need wrapping in an EthernetPacket.
            // For now, we follow the existing pattern in traversal_utils.
            IpPcap4JPostService.sendPacket(ipPacket.rawData, 0, ipPacket.rawData.size)
        } catch (e: Exception) {
            System.err.println("Failed to send spoofed packet: ${e.message}")
        }
    }

    fun receiveRaw(data: ByteArray) {
        val buf = channel.alloc().buffer(data.size)
        buf.writeBytes(data)
        channel.writeInbound(buf)
    }

    override fun addHandlerBefore(baseName: String, name: String, handler: ChannelHandler) {
        channel.pipeline().addBefore(baseName, name, handler)
    }

    override fun pushHandler(handler: ChannelHandler) {
        channel.pipeline().addLast(handler)
    }

    override fun pushHandler(name: String, handler: ChannelHandler) {
        channel.pipeline().addLast(name, handler)
    }

    override fun close(): CompletableFuture<Unit> {
        channel.close()
        return CompletableFuture.completedFuture(Unit)
    }

    override fun closeFuture(): CompletableFuture<Unit> {
        val future = CompletableFuture<Unit>()
        channel.closeFuture().addListener { future.complete(Unit) }
        return future
    }

    override fun localAddress(): Multiaddr = localAddr
    override fun remoteAddress(): Multiaddr = remoteAddr
    override fun transport(): Transport = transport

    override fun muxerSession(): StreamMuxer.Session {
        TODO("Muxer session not implemented for Pcap transport")
    }

    override fun secureSession(): SecureChannel.Session {
        TODO("Secure session not implemented for Pcap transport")
    }
}
