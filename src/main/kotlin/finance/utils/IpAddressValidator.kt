package finance.utils

import io.micronaut.http.HttpRequest
import org.slf4j.LoggerFactory

object IpAddressValidator {
    private val logger = LoggerFactory.getLogger(IpAddressValidator::class.java)

    fun getClientIpAddress(request: HttpRequest<*>): String {
        val clientIp = request.remoteAddress.address.hostAddress ?: "unknown"

        return if (isFromTrustedProxy(clientIp)) {
            val xForwardedFor = request.headers.get("X-Forwarded-For")
            val xRealIp = request.headers.get("X-Real-IP")
            when {
                !xForwardedFor.isNullOrBlank() -> {
                    val forwardedIp = xForwardedFor.split(",")[0].trim()
                    if (isValidIpAddress(forwardedIp)) forwardedIp else clientIp
                }
                !xRealIp.isNullOrBlank() -> {
                    if (isValidIpAddress(xRealIp)) xRealIp else clientIp
                }
                else -> clientIp
            }
        } else {
            logger.debug("Ignoring proxy headers from untrusted IP: {}", clientIp)
            clientIp
        }
    }

    private fun isFromTrustedProxy(clientIp: String): Boolean {
        if (clientIp == "unknown") return false
        if (clientIp == "::1" || clientIp == "0:0:0:0:0:0:0:1") return true
        val trustedNetworks = listOf("10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16", "127.0.0.0/8")
        return trustedNetworks.any { isIpInNetwork(clientIp, it) }
    }

    private fun isValidIpAddress(ip: String): Boolean {
        val ipv4Regex = """^(\d{1,3}\.){3}\d{1,3}$""".toRegex()
        if (ipv4Regex.matches(ip)) {
            return ip.split(".").all { it.toIntOrNull()?.let { n -> n in 0..255 } ?: false }
        }
        val ipv6Regex = """^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$""".toRegex()
        return ipv6Regex.matches(ip)
    }

    private fun isIpInNetwork(ip: String, cidr: String): Boolean {
        return try {
            val (networkAddress, prefixStr) = cidr.split("/")
            val prefixLength = prefixStr.toInt()
            val ipBytes = ipToBytes(ip)
            val networkBytes = ipToBytes(networkAddress)
            if (ipBytes.size != networkBytes.size) return false
            val fullBytes = prefixLength / 8
            val remainingBits = prefixLength % 8
            for (i in 0 until fullBytes) {
                if (ipBytes[i] != networkBytes[i]) return false
            }
            if (remainingBits > 0 && fullBytes < ipBytes.size) {
                val mask = (0xFF shl (8 - remainingBits)) and 0xFF
                if ((ipBytes[fullBytes].toInt() and mask) != (networkBytes[fullBytes].toInt() and mask)) return false
            }
            true
        } catch (e: Exception) {
            logger.warn("Failed to parse IP/CIDR: ip={}, cidr={}", ip, cidr, e)
            false
        }
    }

    private fun ipToBytes(ip: String): ByteArray =
        ip.split(".").map { it.toInt().toByte() }.toByteArray()
}
