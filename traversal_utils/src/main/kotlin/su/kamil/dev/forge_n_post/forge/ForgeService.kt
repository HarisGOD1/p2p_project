package su.kamil.dev.forge_n_post.forge

interface ForgeService {
    fun makePacket(header: ByteArray,data: ByteArray): ByteArray

}