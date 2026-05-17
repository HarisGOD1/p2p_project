package su.kamil.dev.chat.example

import io.libp2p.core.PeerId
import io.libp2p.core.Stream
import io.libp2p.core.multistream.ProtocolId
import io.libp2p.core.multistream.StrictProtocolBinding
import io.libp2p.etc.types.toByteBuf
import io.libp2p.protocol.ProtocolHandler
import io.libp2p.protocol.ProtocolMessageHandler
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset
import java.util.concurrent.CompletableFuture

interface RegistryController {
    fun sendRegister(ip: String, port: Int)
    fun sendListRequest()
    fun sendListResponse(peers: Map<String, String>)
    fun sendPunchRequest(targetPeerId: String, spoofedPort: Int)
    fun sendIncomingPunch(sourcePeerId: String, spoofedPort: Int)
}

typealias OnRegistryMessage = (PeerId, String) -> Unit

class Registry(registryCallback: OnRegistryMessage) : RegistryBinding(RegistryProtocol(registryCallback))

const val REGISTRY_PROTOCOL_ID: ProtocolId = "/example/registry/0.1.0"

open class RegistryBinding(protocol: RegistryProtocol) : StrictProtocolBinding<RegistryController>(REGISTRY_PROTOCOL_ID, protocol)

open class RegistryProtocol(
    private val registryCallback: OnRegistryMessage
) : ProtocolHandler<RegistryController>(Long.MAX_VALUE, Long.MAX_VALUE) {

    override fun onStartInitiator(stream: Stream) = onStart(stream)
    override fun onStartResponder(stream: Stream) = onStart(stream)

    private fun onStart(stream: Stream): CompletableFuture<RegistryController> {
        val ready = CompletableFuture<Void>()
        val handler = RegistryHandler(registryCallback, ready)
        stream.pushHandler(handler)
        return ready.thenApply { handler }
    }

    open inner class RegistryHandler(
        private val registryCallback: OnRegistryMessage,
        val ready: CompletableFuture<Void>
    ) : ProtocolMessageHandler<ByteBuf>, RegistryController {
        lateinit var stream: Stream

        override fun onActivated(stream: Stream) {
            this.stream = stream
            ready.complete(null)
        }

        override fun onMessage(stream: Stream, msg: ByteBuf) {
            val msgStr = msg.toString(Charset.defaultCharset())
            registryCallback(stream.remotePeerId(), msgStr)
        }

        override fun sendRegister(ip: String, port: Int) {
            val data = "REG $ip:$port".toByteArray(Charset.defaultCharset())
            stream.writeAndFlush(data.toByteBuf())
        }

        override fun sendListRequest() {
            val data = "REQ_LIST".toByteArray(Charset.defaultCharset())
            stream.writeAndFlush(data.toByteBuf())
        }

        override fun sendListResponse(peers: Map<String, String>) {
            val payload = peers.entries.joinToString(",") { "${it.key}=${it.value}" }
            val data = "RES_LIST $payload".toByteArray(Charset.defaultCharset())
            stream.writeAndFlush(data.toByteBuf())
        }

        override fun sendPunchRequest(targetPeerId: String, spoofedPort: Int) {
            val data = "REQ_PUNCH $targetPeerId $spoofedPort".toByteArray(Charset.defaultCharset())
            stream.writeAndFlush(data.toByteBuf())
        }

        override fun sendIncomingPunch(sourcePeerId: String, spoofedPort: Int) {
            val data = "INCOMING_PUNCH $sourcePeerId $spoofedPort".toByteArray(Charset.defaultCharset())
            stream.writeAndFlush(data.toByteBuf())
        }
    }
}
