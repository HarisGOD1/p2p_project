package su.kamil.dev.ip_spuffers

import su.kamil.dev.forge_n_post.network.IpForgeAndPostService
import su.kamil.dev.forge_n_post.transport.TcpForgeAndPostService
import su.kamil.dev.forge_n_post.transport.UdpForgeAndPostService
import java.net.InetAddress

class PortIpSpoofer {
    val tcpForge = TcpForgeAndPostService()
    val udpForge = UdpForgeAndPostService()
    val ipForge = IpForgeAndPostService()
    // we want to get what?
    // to send data
    // to some user
    //
    fun send(data: ByteArray,
             destinationIP: InetAddress,
             destinationPort: Int,
             sourceSpoofedIP: InetAddress,
             sourceSpoofedPort: Int,
             protocol: String)
    {
        when(protocol) {
            "tcp" -> tcpForge.sendPacket(
                tcpForge.makePacket(TODO(), data)
            )
            "udp" -> udpForge.sendPacket(
                udpForge.makePacket(TODO(),data))
            "ip" -> ipForge.sendPacket(
                ipForge.makePacket(TODO(),
                    tcpForge.makePacket(TODO(),
                        data)
                )
            )
        }


    }
}