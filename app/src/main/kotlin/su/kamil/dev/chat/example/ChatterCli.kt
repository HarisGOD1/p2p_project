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
        val node = ChatNode(::printAndLn)

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

            node.send(message)
        } while ("bye" != message)

        node.stop()
    }
}