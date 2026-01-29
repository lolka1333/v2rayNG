package com.v2ray.devicekit

object Compat {

    fun decryptSubscriptionUrl(rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) return rawUrl
        return HappDecryptor.tryDecrypt(rawUrl) ?: rawUrl
    }

    fun expandHappLinksInText(text: String?): String? {
        if (text.isNullOrEmpty()) return text

        val out = ArrayList<String>()
        text.lines().forEach { line ->
            val decrypted = HappDecryptor.tryDecrypt(line)
            if (decrypted.isNullOrEmpty()) {
                out.add(line)
            } else {
                decrypted.lines().forEach { out.add(it) }
            }
        }
        return out.joinToString("\n")
    }
}
