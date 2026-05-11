package su.kamil.dev.forge_n_post.transport

import org.pcap4j.core.NotOpenException
import org.pcap4j.core.PcapHandle
import org.pcap4j.core.PcapNativeException
import org.pcap4j.packet.IllegalRawDataException
import org.pcap4j.packet.Packet
import org.pcap4j.packet.TcpPacket
import org.pcap4j.packet.UnknownPacket
import org.pcap4j.packet.namednumber.TcpPort
import java.net.Inet4Address


/**
 * TCP Header Forge Utility using Pcap4J (RFC 793)
 * Provides capabilities to forge raw TCP segments.
 * Fully compatible with Kotlin via standard Java interoperability.
 */
class TcpPcap4JForgeAndPostService {
    /**
     * Forges a TCP segment from a pre-constructed raw RFC 793 header byte array
     * and a payload byte array.
     *
     * @param rawHeader The raw TCP header bytes (must be at least 20 bytes according to RFC 793).
     * @param payload   The raw payload data bytes (can be null or empty).
     * @return Built TcpPacket parsed from the combined raw bytes.
     * @throws IllegalRawDataException If the raw bytes do not form a valid TCP segment.
     * @throws IllegalArgumentException If the provided header is invalid (null or < 20 bytes).
     */
    @Throws(IllegalRawDataException::class)
    fun forgeTcpPacketFromBytes(rawHeader: ByteArray, payload: ByteArray?): TcpPacket {
        require(!(rawHeader == null || rawHeader.size < 20)) { "Invalid TCP header: must be at least 20 bytes per RFC 793." }

        val payloadLen = if (payload != null) payload.size else 0
        val fullPacketData = ByteArray(rawHeader.size + payloadLen)

        // 1. Copy the raw TCP header into the packet buffer
        System.arraycopy(rawHeader, 0, fullPacketData, 0, rawHeader.size)

        // 2. Append the payload directly after the header
        if (payloadLen > 0) {
            System.arraycopy(payload, 0, fullPacketData, rawHeader.size, payloadLen)
        }

        // 3. Let Pcap4J deserialize the raw bytes into a full TcpPacket object
        return TcpPacket.newPacket(fullPacketData, 0, fullPacketData.size)
    }

    /**
     * Forges a TCP segment using explicit parameters and a raw byte payload.
     * Pcap4J builders automatically calculate the Header Checksum using the required IP Pseudo-Header.
     *
     * @param srcIp    Source IPv4 Address (Required for Pseudo-Header Checksum)
     * @param dstIp    Destination IPv4 Address (Required for Pseudo-Header Checksum)
     * @param srcPort  Source Port
     * @param dstPort  Destination Port
     * @param seq      Sequence Number
     * @param ack      Acknowledgment Number
     * @param syn      SYN Flag (Handshake)
     * @param ackFlag  ACK Flag (Handshake)
     * @param fin      FIN Flag (Teardown)
     * @param rst      RST Flag (Reset)
     * @param psh      PSH Flag (Push Data)
     * @param urg      URG Flag (Urgent Data)
     * @param window   Window Size
     * @param payload  Raw byte array for the payload (can be null or empty)
     * @return Built TcpPacket ready to be embedded into an IpV4Packet
     */
    fun forgeTcpPacket(
        srcIp: Inet4Address?,
        dstIp: Inet4Address?,
        srcPort: Short,
        dstPort: Short,
        seq: Int,
        ack: Int,
        syn: Boolean,
        ackFlag: Boolean,
        fin: Boolean,
        rst: Boolean,
        psh: Boolean,
        urg: Boolean,
        window: Short,
        payload: ByteArray?
    ): TcpPacket {
        val tcpBuilder = TcpPacket.Builder()

        // Populate standard TCP header fields and Handshake Flags
        tcpBuilder
            .srcPort(TcpPort.getInstance(srcPort))
            .dstPort(TcpPort.getInstance(dstPort))
            .sequenceNumber(seq)
            .acknowledgmentNumber(ack)
            .syn(syn)
            .ack(ackFlag)
            .fin(fin)
            .rst(rst)
            .psh(psh)
            .urg(urg)
            .window(window) // URG pointer defaults to 0 unless specifically required
            .urgentPointer(0.toShort()) // --- CRITICAL: THE PSEUDO HEADER ---
            // TCP checksum calculation requires the Source and Destination IP addresses
            // from the IP layer. We pass them to the TCP builder here so Pcap4J can
            // calculate the checksum mathematically without needing the actual IpV4Packet yet.

            .srcAddr(srcIp)
            .dstAddr(dstIp)

            .correctChecksumAtBuild(true)
            .correctLengthAtBuild(true)

        // Attach Payload (Data)
        if (payload != null && payload.size > 0) {
            val payloadBuilder = UnknownPacket.Builder()
            payloadBuilder.rawData(payload)
            tcpBuilder.payloadBuilder(payloadBuilder)
        }

        return tcpBuilder.build()
    }

    /**
     * Sends the forged packet through a provided PcapHandle.
     * * * IMPORTANT USAGE NOTE FOR TCP:
     * A TCP Packet cannot travel over a network by itself.
     * In your Kotlin/Java app, you MUST embed the TcpPacket returned above into an
     * IpV4Packet's payload, and then (usually) wrap that IpV4Packet into an EthernetPacket
     * before passing it to this function.
     *
     * @param handle Opened PcapHandle bound to a specific Network Interface
     * @param packet The fully wrapped Packet (e.g., EthernetPacket -> IpV4Packet -> TcpPacket)
     * @throws PcapNativeException If a libpcap native error occurs during transmission
     * @throws NotOpenException    If the provided PcapHandle is not open
     */
    @Throws(PcapNativeException::class, NotOpenException::class)
    fun sendForgedPacket(handle: PcapHandle, packet: Packet) {
        require(!(handle == null || !handle.isOpen())) { "PcapHandle must be initialized and open." }

        handle.sendPacket(packet)
    }
}