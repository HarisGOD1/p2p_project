package su.kamil.dev.tcp_retranslators

import java.io.IOException
import java.net.Inet6Address
import java.net.ServerSocket
import java.net.Socket


class IPv4toIPv6 {



    fun run(ipv4:String,port4:Int,ipv6: String,port6: Int){

        // while True:
        // receive packet from ipv4:port4
        // send to ipv6:port6
        try {
            // Force IPv4
            val ipv4Rec = ServerSocket(port4)
            println("Using IPv4: " + ipv4Rec.getInetAddress())


            // Force IPv6
            val ipv6Address = Inet6Address.getByName(ipv6)
            val ipv6Socket = Socket(ipv6Address, port6)
            println("Using IPv6: " + ipv6Socket.getInetAddress())
            val ipv4sender = ipv4Rec.accept()
            println("accepted " + ipv4sender.getInetAddress())
            while (true) {
                val readed = ipv4sender.getInputStream().readAllBytes()

                println("ipv4 util get: ${String(readed)}")
                ipv6Socket.getOutputStream().write(readed)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
