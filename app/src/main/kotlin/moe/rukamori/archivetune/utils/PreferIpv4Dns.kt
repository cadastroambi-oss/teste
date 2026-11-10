package moe.rukamori.archivetune.utils

import java.net.Inet4Address
import java.net.InetAddress
import moe.rukamori.archivetune.innertube.YouTube
import okhttp3.Dns

/**
 * Os endereços de áudio do YouTube ficam presos ao IP de quem pediu. Nos dados móveis (4G/5G) o
 * celular tem IPv4 e IPv6 ao mesmo tempo e o OkHttp "aposta corrida" entre os dois: o pedido do
 * endereço podia sair por um e o download do áudio pelo outro, e o YouTube respondia 403
 * ("No stream available"). Aqui todas as conexões preferem IPv4, então o IP é o mesmo nas duas
 * pontas. Se não houver endereço IPv4, usa o que vier.
 */
class PreferIpv4Dns(
    private val delegate: () -> Dns,
) : Dns {
    constructor(dns: Dns) : this({ dns })

    override fun lookup(hostname: String): List<InetAddress> {
        val all = delegate().lookup(hostname)
        val ipv4 = all.filterIsInstance<Inet4Address>()
        return ipv4.ifEmpty { all }
    }

    companion object {
        /** Para os clientes de áudio: segue o DNS configurado no YouTube (sistema ou DNS seguro). */
        val forYouTubeMedia: Dns = PreferIpv4Dns { YouTube.dns }
    }
}
