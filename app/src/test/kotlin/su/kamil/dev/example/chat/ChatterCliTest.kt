package su.kamil.dev.example.chat

import kotlin.test.Test

class ChatterCliTest {
    @Test
    fun testChat() {
        val node = ChatNode(::println)

        println()
        println("Libp2p Chatter!")
        println("===============")
        println()
        println("This node is ${node.peerId}, listening on ${node.address}")
        println()
        println("Enter 'bye' to quit, enter 'alias <name>' to set chat name")
        println()

        var message: String?

        print(">> ")
        message = readln().trim()

        node.send(message)


        node.stop()
    }
}