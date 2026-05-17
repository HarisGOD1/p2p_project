package su.kamil.dev.chat.example

fun main() {
    ChatterCli().runChat()
}
class ChatterCli {
    private fun printAndLn(text: String){
        println(text)
        print(">> ")
    }
    fun runChat() {
        val node = ChatNode(::printAndLn) // setup output stream, which are a function type: string -> Unit(aka java's Void)

        println()
        println("Libp2p Chatter!")
        println("===============")
        println()
        println("This node is ${node.peerId}, listening on ${node.address}")
        println()
        println("Enter 'bye' to quit, enter 'alias <name>' to set chat name")
        println()


        do {
            var message: String?

            print(">> ")
            message = readln().trim()

            if (message.startsWith("spoof ")) {
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