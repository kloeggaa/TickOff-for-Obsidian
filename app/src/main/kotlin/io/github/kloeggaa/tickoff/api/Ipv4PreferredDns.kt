package io.github.kloeggaa.tickoff.api

import okhttp3.Dns
import java.net.Inet4Address
import java.net.InetAddress

/** Tries IPv4 addresses first; some mobile networks stall on IPv6 connects to the AI APIs. */
object Ipv4PreferredDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val addresses = Dns.SYSTEM.lookup(hostname)
        return addresses.sortedBy { if (it is Inet4Address) 0 else 1 }
    }
}
