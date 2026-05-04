package su.kamil.dev.tcp_retranslators

import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class IPv4Test {

    val senderIP = "127.0.0.1"
    val senderPort = 35255

    @Test
    fun testServerIPv4TCP() {
        val serverThread = Thread({

            // Force IPv4
            val ipv4Rec = ServerSocket(senderPort)
            println("Using IPv4: " + ipv4Rec.getInetAddress())


            // Force IPv6
            val ipv4sender = ipv4Rec.accept()
            println("accepted " + ipv4sender.getInetAddress())
            Thread.sleep(100)
            while (true) {
                try {
                    println("trying")
                    val message = ipv4sender.getInputStream().readAllBytes()
                    println("ipv4 util get: ${String(message)}")
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

        })



        serverThread.start()
        Thread.sleep(10000)
    }

    @Test
    fun testClientIPv4TCP() {

        val clientThread = Thread({
            val ipv4Address = Inet4Address.getByName(senderIP)

            val socket = Socket()
            socket.connect(InetSocketAddress(ipv4Address, senderPort), 50)
            while (true) {
                try {
                    println("ipv4 sender sends message")
                    socket.getOutputStream().write("Hello World from ipv4 sender".toByteArray())
                    Thread.sleep(300)
                } catch (e: IOException) {
                    throw e
                }

            }
        })
        clientThread.start()
    }


}