package su.kamil.dev.forge_n_post.network

import su.kamil.dev.forge_n_post.ForgeAndPostService
import su.kamil.dev.forge_n_post.forge.IpPcap4JForgeService
import su.kamil.dev.forge_n_post.post.IpPcap4JPostService
import su.kamil.dev.forge_n_post.post.PostService

/**
 * IP Header Forge Utility using Pcap4J
 * Provides capabilities to forge raw IPv4 headers and inject them over the network.
 * Fully compatible with Kotlin via standard Java interoperability.
 */
class IpPcap4JForgeAndPostService {
    companion object : ForgeAndPostService {
        val forge = IpPcap4JForgeService

        override fun makePacket(header: ByteArray, data: ByteArray): ByteArray = forge.makePacket(header, data)

        override fun sendPacket(packet: ByteArray, offset: Int, length: Int) =
            IpPcap4JPostService.sendPacket(packet, offset, length)
    }

}

