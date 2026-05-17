package su.kamil.dev.chat.example

fun main(args: Array<String>) {
    val listenAddr = if (args.size >= 1) args[0] else null
    val listenPort = if (args.size >= 2) args[1].toInt() else 0
    ChatterCli().runChat(listenAddr, listenPort)
}

class ChatterCli {
    private fun printAndLn(text: String){
        println(text)
        print(">> ")
    }
    fun runChat(listenAddr: String? = null, listenPort: Int = 0) {
        val node = ChatNode(::printAndLn, listenAddr, listenPort) // setup output stream

        println()
        println("Libp2p Chatter!")
        println("===============")
        println()
        println("This node is ${node.peerId}")
        println("Listening on ${node.address}:${node.listenPort}")
        println()
        println("Commands:")
        println("  dial <ip> <port> <peerId>  - Connect to a remote node")
        println("  register <publicPeerId>    - Register with a public node")
        println("  list <publicPeerId>        - List registered peers")
        println("  punch <pubId> <tgtId> <p>  - Request hole punch")
        println("  spoof <rIp> <rP> <sIp> <sP> [rId] - Direct spoofed connection")
        println("  alias <name>               - Set chat name")
        println("  bye                        - Quit")
        println()


        do {
            var message: String?

            print(">> ")
            message = readln().trim()

            if (message.startsWith("dial ")) {
                val parts = message.split("\\s+".toRegex())
                if (parts.size == 4) {
                    val ip = parts[1]
                    val port = parts[2].toInt()
                    val peerId = parts[3]
                    node.connectToNode(ip, port, peerId)
                } else {
                    println("Usage: dial <ip> <port> <peerId>")
                }
            } else if (message.startsWith("spoof ")) {
                val parts = message.split("\\s+".toRegex())
                if (parts.size >= 5) {
                    val remoteIp = parts[1]
                    val remotePort = parts[2].toInt()
                    val spoofIp = parts[3]
                    val spoofPort = parts[4].toInt()
                    val remotePeerId = if (parts.size > 5) parts[5] else null
                    node.connectSpoofed(remoteIp, remotePort, spoofIp, spoofPort, remotePeerId)
                } else {
                    println("Usage: spoof <remoteIp> <remotePort> <spoofIp> <spoofPort> [remotePeerId]")
                }
            } else if (message.startsWith("register ")) {
                val parts = message.split("\\s+".toRegex())
                if (parts.size == 2) {
                    node.registerSelf(parts[1])
                } else {
                    println("Usage: register <publicPeerId>")
                }
            } else if (message.startsWith("list ")) {
                val parts = message.split("\\s+".toRegex())
                if (parts.size == 2) {
                    node.listPeers(parts[1])
                } else {
                    println("Usage: list <publicPeerId>")
                }
            } else if (message.startsWith("punch ")) {
                val parts = message.split("\\s+".toRegex())
                if (parts.size == 4) {
                    val publicPeerId = parts[1]
                    val targetPeerId = parts[2]
                    val port = parts[3].toInt()
                    node.requestHolePunch(publicPeerId, targetPeerId, port)
                } else {
                    println("Usage: punch <publicPeerId> <targetPeerId> <spoofedPort>")
                }
            } else {
                node.send(message)
            }
        } while ("bye" != message)

        node.stop()
    }
}