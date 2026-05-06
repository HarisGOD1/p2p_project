package su.kamil.dev.forge_n_post.transport

import su.kamil.dev.forge_n_post.ForgeAndPostService

class TcpForgeAndPostService : ForgeAndPostService {
    override fun makePacket(header: ByteArray, data: ByteArray): ByteArray {
        TODO()
    }
    override fun sendPacket(packet: ByteArray){
        TODO()
    }
}