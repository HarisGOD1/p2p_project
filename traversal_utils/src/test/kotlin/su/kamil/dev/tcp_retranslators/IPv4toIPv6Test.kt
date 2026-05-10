package su.kamil.dev.tcp_retranslators

import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket


class IPv4toIPv6Test {


    @Test
    fun test(){

        val senderIP = "127.0.0.1"
        val senderPort = 35253
        val receiverIP = "0:0:0:0:0:0:0:1"
        val receiverPort = 23456

        val thIPv4 = Thread({
            val ipv4Address = Inet4Address.getByName(senderIP)
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(ipv4Address,senderPort),50)
                while (true){
                    println("ipv4 sender sends message")
                    socket.getOutputStream().write("Hello World from ipv4 sender".toByteArray())
                    socket.getOutputStream().flush()
                    Thread.sleep(300)
//                socket.getOutputStream().write("Hello World from ipv4 sender".toByteArray())
                }
            }
            catch (e: IOException){
                throw e
            }

        }
        )

        val thIPv6 = Thread({

            val ipv6ReceiverSocket = ServerSocket(receiverPort)
            val ipv6SenderSocket = ipv6ReceiverSocket.accept()
            while (true){
                println("ipv6 receiver receives message")
                val byteReceived = ipv6SenderSocket.getInputStream().readAllBytes()
                println("catch messages: ${String(byteReceived, Charsets.UTF_8)}")
            }
        })
//        val util = IPv4toIPv6()
//        val thIPv4toIPv6 = Thread({
//            util.run(senderIP, senderPort, receiverIP, receiverPort)
//        })
        thIPv6.start()
        Thread.sleep(1000L)
//        thIPv4toIPv6.start()
        Thread.sleep(1000L)
        thIPv4.start()
        Thread.sleep(1000L)

    }
}