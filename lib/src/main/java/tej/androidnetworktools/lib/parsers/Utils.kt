package tej.androidnetworktools.lib.parsers

import org.apache.commons.lang3.ArrayUtils
import java.math.BigInteger
import java.net.InetAddress
import java.net.UnknownHostException

object Utils {
    @JvmStatic
    fun parseIpAddress(ip: Long): String? {
        try {
            val byteAddress = BigInteger.valueOf(ip).toByteArray()
            ArrayUtils.reverse(byteAddress)
            return InetAddress.getByAddress(byteAddress).hostAddress
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        }
        return null
    }
}
