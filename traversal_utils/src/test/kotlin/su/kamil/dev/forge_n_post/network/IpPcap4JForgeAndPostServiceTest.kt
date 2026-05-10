package su.kamil.dev.forge_n_post.network

import org.pcap4j.packet.namednumber.IpNumber
import su.kamil.dev.forge_n_post.forge.IpPcap4JForgeService
import java.net.Inet4Address
import java.net.InetAddress
import kotlin.test.Test


class IpPcap4JForgeAndPostServiceTest {

    @Test
    fun testForgingFromByte() {
        val forge = IpPcap4JForgeService()
        val srcIp = InetAddress.getByName("192.168.1.100") as Inet4Address?
        val dstIp = InetAddress.getByName("10.0.0.5") as Inet4Address?

        val packetHR = forge.forgeIpPacket(
            srcIp,
            dstIp,
            IpNumber.UDP,// Setting protocol to UDP (17) for example purposes
            64.toByte(),
            1234.toShort(),
            0.toByte(),
            0.toShort(),
            ByteArray(0)
        )
        val headerRaw = packetHR.header.rawData
        val payloadRaw = ByteArray(0)

        val packetFromByte = forge.forgeIpPacketFromBytes(headerRaw, payloadRaw)

        println(packetHR)
        println(packetFromByte)


    }

}