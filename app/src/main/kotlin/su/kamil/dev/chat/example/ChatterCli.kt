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
        println("  /dial <ip> <p> <id>        - Connect to a remote node")
        println("  /register <publicPeerId>   - Register with a public node")
        println("  /list <publicPeerId>       - List registered peers")
        println("  /punch <pubId> <tgtId> <p> - Request hole punch")
        println("  /spoof <rIp> <rP> <sIp> <sP> [rId] - Direct spoofed connection")
        println("  /alias <name>              - Set chat name")
        println("  /bye                       - Quit")
        println()

        while (true) {
            print(">> ")
            val message = readln().trim()
            if (message.isEmpty()) continue

            if (message.startsWith("/")) {
                val parts = message.split("\\s+".toRegex())
                val command = parts[0].lowercase()

                when (command) {
                    "/dial" -> {
                        if (parts.size == 4) {
                            node.connectToNode(parts[1], parts[2].toInt(), parts[3])
                        } else {
                            println("Usage: /dial <ip> <port> <peerId>")
                        }
                    }
                    "/spoof" -> {
                        if (parts.size >= 5) {
                            val remotePeerId = if (parts.size > 5) parts[5] else null
                            node.connectSpoofed(parts[1], parts[2].toInt(), parts[3], parts[4].toInt(), remotePeerId)
                        } else {
                            println("Usage: /spoof <remoteIp> <remotePort> <spoofIp> <spoofPort> [remotePeerId]")
                        }
                    }
                    "/register" -> {
                        if (parts.size == 2) {
                            node.registerSelf(parts[1])
                        } else {
                            println("Usage: /register <publicPeerId>")
                        }
                    }
                    "/list" -> {
                        if (parts.size == 2) {
                            node.listPeers(parts[1])
                        } else {
                            println("Usage: /list <publicPeerId>")
                        }
                    }
                    "/punch" -> {
                        if (parts.size == 4) {
                            node.requestHolePunch(parts[1], parts[2], parts[3].toInt())
                        } else {
                            println("Usage: /punch <publicPeerId> <targetPeerId> <spoofedPort>")
                        }
                    }
                    "/alias" -> {
                        if (parts.size >= 2) {
                            node.send(message) // ChatNode handles /alias internally
                        } else {
                            println("Usage: /alias <name>")
                        }
                    }
                    "/bye" -> {
                        node.stop()
                        return
                    }
                    else -> println("Unknown command: $command")
                }
            } else {
                node.send(message)
            }
        }
    }
}