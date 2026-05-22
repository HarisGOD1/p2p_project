package su.kamil.dev.forge_n_post.post

import org.pcap4j.core.*
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode
import org.pcap4j.packet.IpV4Packet

class IpPcap4JPostService {
    companion object : PostService {

        private val allInterfaces: MutableList<PcapNetworkInterface>? = Pcaps.findAllDevs()

        private val networkInterface: PcapNetworkInterface = allInterfaces!!.get(0)
        private val snapLen: Int = 65536
        private val timeoutMillis: Int = 10
        var handle: PcapHandle = networkInterface.openLive(snapLen, PromiscuousMode.PROMISCUOUS, timeoutMillis)

        private val postService = IpPcap4JPostService()

        // obv -- send IP packet
        override fun sendPacket(packet: ByteArray, offset: Int, length: Int) {
            postService.sendForgedPacket(
                handle,
                IpV4Packet.newPacket(packet, offset, length)
            )
        }
        fun closeHandle(){
            handle.close();
        }

        fun selectHandleByInterfaceName(interfaceName: String){
            val newNetworkInterface = allInterfaces!!.find { it.name == interfaceName }
            if(newNetworkInterface != null) {
                handle = newNetworkInterface.openLive(snapLen, PromiscuousMode.PROMISCUOUS, timeoutMillis)
            }
            else{
                throw Exception("selected interface $interfaceName was not found")
            }
        }

    }

    /**
     * Sends the forged IPv4 packet through a provided PcapHandle.
     * * IMPORTANT NOTE FOR LAYER 2:
     * If your PcapHandle is bound to a standard Ethernet interface (Wi-Fi/LAN),
     * you will typically need to wrap the returned IpV4Packet inside an EthernetPacket
     * before calling handle.sendPacket(). This function assumes either a TUN/TAP/RAW
     * interface or that you have already wrapped the packet if required.
     *
     * @param handle Opened PcapHandle bound to a specific Network Interface
     * @param packet The forged IpV4Packet (or wrapped EthernetPacket)
     * @throws PcapNativeException If a libpcap native error occurs during transmission
     * @throws NotOpenException    If the provided PcapHandle is not open
     */
    private fun sendForgedPacket(handle: PcapHandle, packet: org.pcap4j.packet.Packet?) {
        require(handle.isOpen) { "PcapHandle must be initialized and open." }

        handle.sendPacket(packet)
    }

}