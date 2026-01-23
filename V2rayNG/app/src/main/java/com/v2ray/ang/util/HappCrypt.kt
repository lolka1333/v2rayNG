package com.v2ray.ang.util

import android.util.Base64
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

object HappCrypt {
    private const val PREFIX = "happ://"

    private class RsaKey(nHex: String, dHex: String) {
        val n: BigInteger = BigInteger(nHex, 16)
        val d: BigInteger = BigInteger(dHex, 16)
        val k: Int = (n.bitLength() + 7) / 8
    }

    private val crypt4Keys: List<RsaKey> by lazy {
        listOf(
            RsaKey(
                "dd46743372f82be5a3337be46d09f3a331e0fdc45b117bd0ea2e00f1154de0e337acaf64534d45763ca8220cb0bdef0e10ab059d5c044590106754ebbfad0186668cefa410104f8452520e2fd129c0a586b532f99580b5b13f5724c34fdff088d203f9c30408f3c718ff179da4e1015bbd8498e392847526fdbff79be1cf9aac1ce2e80a8f9cd65794b2893f36f5a140e43c528e6208141026b3b0da7e64aa2c54d82102f9712843636bbefecab2030f726483a8a96d931488081e12cf59e93fa1c0d4ef3c8eae5bb51344a671041b2c5a87ab4d9fb271fa21ca93bf737901d6ff0a46430d0e87bd05419aa6ca64a7fb65d5e4aac73f670dbbc611bb39f5c2b54996b222fbcc0f8aa137776afc8e87ddd0f63bb8510b07b612f71ace2f10260e092f5db175d21c463fd67814fe18ce58517c40413fad857a9c0e0f4f252fa3946c72d4902fff43257ed7361df39e0be27fc2ec08dd8c3efc7a5feccba2e057ce006443d0c57d90062d67fd4d1eca563642ff736cb94083a0039ca6637ca443416de074a6e88a30c592be5977ebed8c611d57b343204fc63d4f0844dbe4e79d324e8739878bf071894958ff0d84d2ce1ac7d1870265f4c32178cde57b90391cbc7b8475cd1deef0e6770d28a82a968f45f0abce877ce46eead9b503cb910138dbe12ce34ca7252aea049550156b9268a21fee64ca330bae52983c3e379a7a4aef",
                "4b960c8da2a159c568d96661e2497afb6d477108491f90cf0d9fc75c932d763bc7c1ea6bf7f4b65cac5db6bbe5080c881e737d4882ad46e1ee688ddeb62c30102c29ca969d9224c78ba3520b4d84d8b4f26e254a92a253ee87378a6c5975a2e1e9f3c7b6b143299b0253e94a458f42fa86a3abe259237c2d6492d5cf5ef49ede5a43bc861706b945539b253cfdd62c4e5168b5bec38804ea7b7df854bac6424740784fd68744fb3f01d0457f1dd86f2930a75b3e1b1a3e1ace590f84d765dc428b4ba6b14e5b62fe1abdc93e9dee8c25bb8b0a9e4a86175f1d79e8bc868278796bc7a3b6b5d817d01ee289249e9081bd63cd0bfeb66f57119eea0b168cb701da6eb8e7203339ef1bc900ee1f297e4bdff84df99c7cab8745b0784e3a727502cd7c6028646095c5f73714184ceda220c151024022b3091d61493adc63efc7cfd7c2d83a36e150bd47dc874a2cbaf576e5fd68633d94080957aea5feb5a5c9e3e1fa904cae24db3809d712c40556f8f3cf3c816739c1658089a30fca9993b759c8856d2113aaadc5aa81371f0bf1b2395a0b88291d6a9ae34946b6316f3e72074ba31b1fb2f055c025743ae3baa34931de655a13c09d8563ec9604476df45f61d0dc106bac067b65a7b596b25f750b34ce3622c8774e0264c4b569e4b9269d92779c1eb2f8458a19b5d4eb8994f8d8dfad71b2406f918baf714e57057a22110459"
            )
        )
    }

    fun tryDecrypt(input: String?): String? {
        if (input.isNullOrBlank()) return null

        val raw = input.trim()
        if (!raw.startsWith(PREFIX)) return null

        val slash = raw.indexOf('/', PREFIX.length)
        if (slash <= 0) return null

        val host = raw.substring(PREFIX.length, slash)
        val payload = raw.substring(slash + 1)

        val keys = when (host) {
            "crypt4" -> crypt4Keys
            "crypt3" -> emptyList()
            "crypt2" -> emptyList()
            "crypt" -> emptyList()
            else -> emptyList()
        }
        if (keys.isEmpty()) return null

        val cleaned = payload
            .replace(Regex("[^A-Za-z0-9+/=_-]"), "")
            .replace('-', '+')
            .replace('_', '/')
            .trimEnd('=')

        val cipherBytes = findCipherBytes(cleaned) ?: return null
        val cipherInt = BigInteger(1, cipherBytes)

        for (key in keys) {
            val plainInt = cipherInt.modPow(key.d, key.n)
            val plainBytes = toFixedLength(plainInt, key.k)
            val msg = pkcs1v15Unpad(plainBytes) ?: continue

            val decoder = Charsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.IGNORE)
                .onUnmappableCharacter(CodingErrorAction.IGNORE)

            return decoder.decode(ByteBuffer.wrap(msg)).toString()
        }

        return null
    }

    private fun pkcs1v15Unpad(block: ByteArray): ByteArray? {
        if (block.size < 11) return null
        if (block[0] != 0.toByte() || block[1] != 2.toByte()) return null

        var sep = -1
        for (i in 2 until block.size) {
            if (block[i] == 0.toByte()) {
                sep = i
                break
            }
        }
        if (sep < 10) return null
        for (i in 2 until sep) {
            if (block[i] == 0.toByte()) return null
        }

        return block.copyOfRange(sep + 1, block.size)
    }

    private fun findCipherBytes(body: String): ByteArray? {
        for (c in 0..8) {
            var t = if (c == 0) body else body.dropLast(c)
            while (t.length % 4 == 1 && t.isNotEmpty()) {
                t = t.dropLast(1)
            }
            if (t.isEmpty()) continue

            val padLen = (4 - (t.length % 4)) % 4
            val padded = t + "=".repeat(padLen)

            val decoded = try {
                Base64.decode(padded, Base64.NO_WRAP)
            } catch (_: Exception) {
                continue
            }

            if (decoded.size == 512) {
                return decoded
            }
        }
        return null
    }

    private fun toFixedLength(value: BigInteger, length: Int): ByteArray {
        val raw = value.toByteArray()

        if (raw.size == length) return raw
        if (raw.size == length + 1 && raw[0] == 0.toByte()) {
            return raw.copyOfRange(1, raw.size)
        }
        if (raw.size < length) {
            return ByteArray(length - raw.size) + raw
        }
        return raw.copyOfRange(raw.size - length, raw.size)
    }
}
