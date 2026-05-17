package su.kamil.dev.chat.example

import io.libp2p.core.Discoverer
import io.libp2p.core.PeerId
import io.libp2p.core.PeerInfo
import io.libp2p.core.Stream
import io.libp2p.core.dsl.host
import io.libp2p.core.multiformats.Multiaddr
import io.libp2p.discovery.MDnsDiscovery
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

typealias OnMessage = (String) -> Unit

class ChatNode(
    private val printMsg: OnMessage,
    val listenAddr: String? = null,
    val listenPort: Int = 0
) {
    private data class Friend(
        var name: String,
        val controller: ChatController
    )

    private var currentAlias: String
    private val knownNodes = mutableSetOf<PeerId>()
    private val peerFinder: Discoverer
    private val peers = mutableMapOf<PeerId, Friend>()
    private val privateAddress: InetAddress = if (listenAddr != null) InetAddress.getByName(listenAddr) else privateNetworkAddress()
    
    // Registry state
    private val registeredPeers = mutableMapOf<String, String>()
    private val registryControllers = mutableMapOf<PeerId, RegistryController>()

    private val pcapTransport = su.kamil.dev.implementations.transport.udp.Pcap4JUdpTransport()
    
    // chatHost -- is chat server at client,
    // host {...} is equal to execute function: host({...}) -- block of code is just one of arguments of host function.
    //
    private val chatHost = host {
        protocols {
            +Chat(::messageReceived)
            +Registry(::registryMessageReceived, ::onRegistryControllerReady)
        }
        network {
            listen("/ip4/$address/tcp/$listenPort")
            listen("/ip4/$address/udp/$listenPort")
        }
        transports {
            + { upgrader: io.libp2p.transport.ConnectionUpgrader -> io.libp2p.transport.tcp.TcpTransport(upgrader) }
            + { _: io.libp2p.transport.ConnectionUpgrader -> pcapTransport }
        }
    }

    val peerId = chatHost.peerId
    val address: String
        get() {
            return privateAddress.hostAddress
        }

    init {
        chatHost.start().get()
        currentAlias = chatHost.peerId.toBase58()

        // Handle incoming protocol streams
        chatHost.addProtocolHandler(Chat(::messageReceived))
        chatHost.addProtocolHandler(Registry(::registryMessageReceived, ::onRegistryControllerReady))

        peerFinder = MDnsDiscovery(chatHost, address = privateAddress)
        peerFinder.newPeerFoundListeners += { peerFound(it) }
        peerFinder.start()
    } // init

    private fun onRegistryControllerReady(id: PeerId, controller: RegistryController) {
        registryControllers[id] = controller
        printMsg("Registry controller ready for $id")
    }

    fun connectToNode(ip: String, port: Int, remotePeerId: String) {
        val targetPeerId = PeerId.fromBase58(remotePeerId)
        val addr = Multiaddr("/ip4/$ip/tcp/$port")
        
        printMsg("Dialing $targetPeerId at $addr...")
        
        val chat = Chat(::messageReceived).dial(chatHost, targetPeerId, addr)
        chat.controller.thenAccept { controller ->
            chat.stream.thenAccept { stream ->
                peers[targetPeerId] = Friend(targetPeerId.toBase58(), controller)
                stream.closeFuture().thenAccept {
                    printMsg("Disconnected from $targetPeerId")
                    peers.remove(targetPeerId)
                    registryControllers.remove(targetPeerId)
                }
                controller.send("/who")
                printMsg("Connected to $targetPeerId")
            }
        }.exceptionally { e ->
            printMsg("Failed to connect to $targetPeerId: ${e.message}")
            null
        }

        Registry(::registryMessageReceived).dial(chatHost, targetPeerId, addr)
            .controller.thenAccept {
                registryControllers[targetPeerId] = it
                printMsg("Registry protocol active for $targetPeerId")
            }
    }

    fun connectSpoofed(remoteIp: String, remotePort: Int, spoofIp: String, spoofPort: Int, remotePeerId: String? = null) {
        val remoteAddr = Multiaddr("/ip4/$remoteIp/udp/$remotePort")
        pcapTransport.setSpoofedSource(remoteAddr, spoofIp, spoofPort)
        
        printMsg("Initiating spoofed connection to $remoteAddr (spoofing as $spoofIp:$spoofPort)")
        
        if (remotePeerId != null) {
             val peerId = PeerId.fromBase58(remotePeerId)
             Chat(::messageReceived).dial(chatHost, peerId, remoteAddr)
        } else {
             printMsg("No PeerId provided, only hole-punching packet will be sent if data is written.")
             // In libp2p, you can't easily dial without a PeerId unless you use a lower level
             // For the sake of the concept, we've set up the transport mapping.
        }
    }

    fun registerSelf(publicPeerId: String) {
        val id = PeerId.fromBase58(publicPeerId)
        val controller = registryControllers[id]
        if (controller != null) {
            // Find our local UDP port
            val localUdpPort = chatHost.listenAddresses().find { 
                it.components.any { c -> c.protocol == io.libp2p.core.multiformats.Protocol.UDP } 
            }?.components?.find { it.protocol == io.libp2p.core.multiformats.Protocol.UDP }?.stringValue?.toInt() ?: 0
            
            controller.sendRegister(address, localUdpPort)
            printMsg("Sent registration to $publicPeerId as $address:$localUdpPort")
        } else {
            printMsg("Not connected to node $publicPeerId over Registry protocol.")
        }
    }

    fun listPeers(publicPeerId: String) {
        val id = PeerId.fromBase58(publicPeerId)
        val controller = registryControllers[id]
        if (controller != null) {
            controller.sendListRequest()
            printMsg("Requested peer list from $publicPeerId")
        } else {
            printMsg("Not connected to node $publicPeerId over Registry protocol.")
        }
    }

    fun requestHolePunch(publicPeerId: String, targetPeerId: String, spoofedPort: Int) {
        val id = PeerId.fromBase58(publicPeerId)
        val controller = registryControllers[id]
        if (controller != null) {
            controller.sendPunchRequest(targetPeerId, spoofedPort)
            printMsg("Sent punch request for $targetPeerId to $publicPeerId with port $spoofedPort")
        } else {
            printMsg("Not connected to node $publicPeerId over Registry protocol.")
        }
    }

    fun send(message: String) {
        peers.values.forEach { it.controller.send(message) }

        if (message.startsWith("alias ")) {
            currentAlias = message.substring(6).trim()
        }
    } // send

    fun stop() {
        peerFinder.stop()
        chatHost.stop()
    } // stop

    private fun registryMessageReceived(id: PeerId, msg: String) {
        if (msg.startsWith("REG ")) {
            val addr = msg.substring(4)
            registeredPeers[id.toBase58()] = addr
            printMsg("Node ${id.toBase58()} registered at $addr")
        } else if (msg == "REQ_LIST") {
            val controller = registryControllers[id]
            if (controller != null) {
                controller.sendListResponse(registeredPeers)
                printMsg("Sent peer list to ${id.toBase58()}")
            }
        } else if (msg.startsWith("RES_LIST ")) {
            val list = msg.substring(9)
            printMsg("--- Registered Peers ---")
            list.split(",").forEach { 
                if (it.isNotBlank()) {
                    printMsg(it)
                }
            }
            printMsg("------------------------")
        } else if (msg.startsWith("REQ_PUNCH ")) {
            val parts = msg.substring(10).split(" ")
            if (parts.size == 2) {
                val targetId = parts[0]
                val port = parts[1].toInt()
                val targetPeerId = PeerId.fromBase58(targetId)
                val controller = registryControllers[targetPeerId]
                if (controller != null) {
                    controller.sendIncomingPunch(id.toBase58(), port)
                    printMsg("Relaying punch request from ${id.toBase58()} to $targetId")
                } else {
                    printMsg("Cannot relay punch: Target $targetId not connected.")
                }
            }
        } else if (msg.startsWith("INCOMING_PUNCH ")) {
            val parts = msg.substring(15).split(" ")
            if (parts.size == 2) {
                val sourceId = parts[0]
                val port = parts[1]
                val publicIp = registryControllers[id]?.let {
                    // Try to get the IP from the stream's connection
                    // Since we don't have direct access easily, we might just print what we know
                    "PublicNodeIP" 
                } ?: "PublicNodeIP"
                printMsg("!!! INCOMING PUNCH ALERT !!!")
                printMsg("Peer $sourceId is about to spoof $publicIp:$port to reach you.")
                printMsg("Please use 'spoof' command when B initiates connection if needed.")
            }
        }
    }

    private fun messageReceived(id: PeerId, msg: String) {
        if (msg == "/who") {
            peers[id]?.controller?.send("alias $currentAlias")
            return
        }
        if (msg.startsWith("alias ")) {
            val friend = peers[id] ?: return
            val previousAlias = friend.name
            val newAlias = msg.substring(6).trim()
            if (previousAlias != newAlias) {
                friend.name = newAlias
                printMsg("$previousAlias is now $newAlias")
            }
            return
        }

        val alias = peers[id]?.name ?: id.toBase58()
        printMsg("$alias > $msg")
    } // messageReceived

    private fun peerFound(info: PeerInfo) {
        if (
            info.peerId == chatHost.peerId ||
            knownNodes.contains(info.peerId)
        ) {
            return
        }

        knownNodes.add(info.peerId)

        val chatConnection = connectChat(info) ?: return // actual connection with peer
        // code to execute when peer disconnected
        chatConnection.first.closeFuture().thenAccept {
            printMsg("${peers[info.peerId]?.name} disconnected.")
            peers.remove(info.peerId)
            knownNodes.remove(info.peerId)
            registryControllers.remove(info.peerId)
        }
        printMsg("Connected to new peer ${info.peerId}")
        chatConnection.second.send("/who")
        peers[info.peerId] = Friend(
            info.peerId.toBase58(),
            chatConnection.second
        )

        // Also connect to Registry protocol
        Registry(::registryMessageReceived).dial(chatHost, info.peerId, info.addresses[0])
            .controller.thenAccept {
                registryControllers[info.peerId] = it
                printMsg("Registry protocol active for ${info.peerId}")
            }
    } // peerFound

    @Suppress("SwallowedException")
    private fun connectChat(info: PeerInfo): Pair<Stream, ChatController>? {
        try {
            val addr = info.addresses.find { it.components.any { it.protocol == io.libp2p.core.multiformats.Protocol.UDP } }
                ?: info.addresses[0]
            val chat = Chat(::messageReceived).dial(
                chatHost,
                info.peerId,
                addr
            )
            return Pair(
                chat.stream.get(),
                chat.controller.get()
            )
        } catch (e: Exception) {
            return null
        }
    } // connectChat

    companion object {
        private fun privateNetworkAddress(): InetAddress {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            val addresses = interfaces.flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .filter { it.isSiteLocalAddress }
                .sortedBy { it.hostAddress }
            return if (addresses.isNotEmpty()) {
                addresses[0]
            } else {
                InetAddress.getLoopbackAddress()
            }
        }
    }
} // class ChatNode