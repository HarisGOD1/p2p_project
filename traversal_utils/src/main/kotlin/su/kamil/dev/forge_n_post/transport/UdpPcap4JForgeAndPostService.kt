package su.kamil.dev.forge_n_post.transport

import org.pcap4j.core.NotOpenException
import org.pcap4j.core.PcapHandle
import org.pcap4j.core.PcapNativeException
import org.pcap4j.packet.IllegalRawDataException
import org.pcap4j.packet.Packet
import org.pcap4j.packet.UdpPacket
import org.pcap4j.packet.UnknownPacket
import org.pcap4j.packet.namednumber.UdpPort
import java.net.Inet4Address


/**
 * UDP Header Forge Utility using Pcap4J (RFC 768)
 * Provides capabilities to forge raw UDP datagrams.
 * Fully compatible with Kotlin via standard Java interoperability.
 */
class UdpPcap4JForgeAndPostService {
    /**
     * Forges a UDP datagram from a pre-constructed raw RFC 768 header byte array
     * and a payload byte array.
     *
     * @param rawHeader The raw UDP header bytes (must be exactly 8 bytes according to RFC 768).
     * @param payload   The raw payload data bytes (can be null or empty).
     * @return Built UdpPacket parsed from the combined raw bytes.
     * @throws IllegalRawDataException If the raw bytes do not form a valid UDP datagram.
     * @throws IllegalArgumentException If the provided header is invalid (null or < 8 bytes).
     */
    @Throws(IllegalRawDataException::class)
    fun forgeUdpPacketFromBytes(rawHeader: ByteArray, payload: ByteArray?): UdpPacket {
        require(rawHeader.size >= 8) { "Invalid UDP header: must be at least 8 bytes per RFC 768." }

        val payloadLen = if (payload != null) payload.size else 0
        val fullPacketData = ByteArray(rawHeader.size + payloadLen)

        // 1. Copy the raw UDP header into the packet buffer
        System.arraycopy(rawHeader, 0, fullPacketData, 0, rawHeader.size)

        // 2. Append the payload directly after the header
        if (payloadLen > 0) {
            System.arraycopy(payload, 0, fullPacketData, rawHeader.size, payloadLen)
        }

        // 3. Let Pcap4J deserialize the raw bytes into a full UdpPacket object
        return UdpPacket.newPacket(fullPacketData, 0, fullPacketData.size)
    }

    /**
     * Forges a UDP datagram using explicit parameters and a raw byte payload.
     * Pcap4J builders automatically calculate the Length and the Header Checksum
     * using the required IP Pseudo-Header.
     *
     * @param srcIp    Source IPv4 Address (Required for Pseudo-Header Checksum)
     * @param dstIp    Destination IPv4 Address (Required for Pseudo-Header Checksum)
     * @param srcPort  Source Port
     * @param dstPort  Destination Port
     * @param payload  Raw byte array for the payload (can be null or empty)
     * @return Built UdpPacket ready to be embedded into an IpV4Packet
     */
    fun forgeUdpPacket(
        srcIp: Inet4Address?,
        dstIp: Inet4Address?,
        srcPort: Short,
        dstPort: Short,
        payload: ByteArray?
    ): UdpPacket {
        val udpBuilder = UdpPacket.Builder()

        // Populate standard UDP header fields
        udpBuilder
            .srcPort(UdpPort.getInstance(srcPort))
            .dstPort(UdpPort.getInstance(dstPort)) // --- CRITICAL: THE PSEUDO HEADER ---
            // Like TCP, UDP checksum calculation requires the Source and Destination IP
            // addresses from the IP layer. We pass them to the UDP builder here so Pcap4J
            // can calculate the checksum mathematically.

            .srcAddr(srcIp)
            .dstAddr(dstIp)

            .correctChecksumAtBuild(true)
            .correctLengthAtBuild(true)

        // Attach Payload (Data)
        if (payload != null && payload.size > 0) {
            val payloadBuilder = UnknownPacket.Builder()
            payloadBuilder.rawData(payload)
            udpBuilder.payloadBuilder(payloadBuilder)
        }

        return udpBuilder.build()
    }

    /**
     * Sends the forged packet through a provided PcapHandle.
     * * * IMPORTANT USAGE NOTE FOR UDP:
     * A UDP Packet is a Layer 4 protocol. To send it, you MUST embed the UdpPacket
     * returned above into an IpV4Packet's payload, and then (if on a standard NIC)
     * wrap that IpV4Packet into an EthernetPacket before passing it to this function.
     *
     * @param handle Opened PcapHandle bound to a specific Network Interface
     * @param packet The fully wrapped Packet (e.g., EthernetPacket -> IpV4Packet -> UdpPacket)
     * @throws PcapNativeException If a libpcap native error occurs during transmission
     * @throws NotOpenException    If the provided PcapHandle is not open
     */
    @Throws(PcapNativeException::class, NotOpenException::class)
    fun sendForgedPacket(handle: PcapHandle, packet: Packet) {
        require(handle.isOpen()) { "PcapHandle must be initialized and open." }

        handle.sendPacket(packet)
    }
}