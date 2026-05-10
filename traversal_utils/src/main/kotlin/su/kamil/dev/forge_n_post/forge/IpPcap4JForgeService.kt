package su.kamil.dev.forge_n_post.forge

import org.pcap4j.packet.IllegalRawDataException
import org.pcap4j.packet.IpV4Packet
import org.pcap4j.packet.IpV4Rfc791Tos
import org.pcap4j.packet.UnknownPacket
import org.pcap4j.packet.namednumber.IpNumber
import org.pcap4j.packet.namednumber.IpVersion
import java.net.Inet4Address

class IpPcap4JForgeService {
    companion object : ForgeService {
        private val pcap4jForge = IpPcap4JForgeService()

        override fun makePacket(header: ByteArray, data: ByteArray): ByteArray {
            return pcap4jForge.forgeIpPacketFromBytes(header, data).rawData
        }
    }

    /**
     * Forges an IPv4 packet from a pre-constructed raw RFC 791 header byte array
     * and a payload byte array. This is ideal when the header is generated bit-by-bit natively.
     *
     * @param rawHeader The raw IPv4 header bytes (must be at least 20 bytes according to RFC 791).
     * @param payload   The raw payload data bytes (can be null or empty).
     * @return Built IpV4Packet parsed from the combined raw bytes.
     * @throws IllegalRawDataException If the raw bytes do not form a valid IPv4 packet.
     * @throws IllegalArgumentException If the provided header is invalid (null or < 20 bytes).
     */
    fun forgeIpPacketFromBytes(rawHeader: ByteArray, payload: ByteArray?): IpV4Packet {
        require(rawHeader.size >= 20) { "Invalid IPv4 header: must be at least 20 bytes per RFC 791." }

        val payloadLen = payload?.size ?: 0
        val fullPacketData = ByteArray(rawHeader.size + payloadLen)

        // 1. Copy the raw IPv4 header into the packet buffer
        System.arraycopy(rawHeader, 0, fullPacketData, 0, rawHeader.size)

        // 2. Append the payload directly after the header
        if (payloadLen > 0) {
            System.arraycopy(payload, 0, fullPacketData, rawHeader.size, payloadLen)
        }

        // 3. Let Pcap4J deserialize the raw bytes into a full IpV4Packet object
        // Note: Ensure your rawHeader's "Total Length" field (bytes 2 and 3) correctly
        // reflects (header length + payload length) for proper routing!
        return IpV4Packet.newPacket(fullPacketData, 0, fullPacketData.size)
    }

    /**
     * Forges an IPv4 packet using explicit parameters and a raw byte payload.
     * Pcap4J builders automatically calculate the Header Checksum and Total Length.
     *
     * @param srcIp    Source IPv4 Address
     * @param dstIp    Destination IPv4 Address
     * @param protocol The Protocol number (e.g., IpNumber.TCP, IpNumber.UDP)
     * @param ttl      Time to Live
     * @param id       Identification
     * @param tos      Type of Service
     * @param fragOff  Fragment Offset
     * @param payload  Raw byte array for the payload (can be null or empty)
     * @return Built IpV4Packet ready for transmission
     */
    fun forgeIpPacket(
        srcIp: Inet4Address?, dstIp: Inet4Address?,
        protocol: IpNumber?, ttl: Byte, id: Short,
        tos: Byte, fragOff: Short, payload: ByteArray?
    ): IpV4Packet {
        val ipv4Builder: IpV4Packet.Builder = IpV4Packet.Builder()

        // Populate standard IPv4 header fields
        ipv4Builder
            .version(IpVersion.IPV4)
            .tos(IpV4Rfc791Tos.newInstance(tos))
            .identification(id)
            .reservedFlag(false)
            .dontFragmentFlag(false)
            .moreFragmentFlag(false)
            .fragmentOffset(fragOff)
            .ttl(ttl)
            .protocol(protocol)
            .srcAddr(srcIp)
            .dstAddr(dstIp) // Pcap4J handles the mathematical integrity automatically
            .correctChecksumAtBuild(true)
            .correctLengthAtBuild(true)

        // Attach Payload (Data)
        if (payload != null && payload.size > 0) {
            val payloadBuilder = UnknownPacket.Builder()
            payloadBuilder.rawData(payload)
            ipv4Builder.payloadBuilder(payloadBuilder)
        }

        return ipv4Builder.build()
    }

}