package su.kamil.dev.forge_n_post

import su.kamil.dev.forge_n_post.forge.ForgeService
import su.kamil.dev.forge_n_post.post.PostService

interface ForgeAndPostService: ForgeService, PostService {
    override fun makePacket(header: ByteArray,data: ByteArray): ByteArray
    override fun sendPacket(packet: ByteArray)
}