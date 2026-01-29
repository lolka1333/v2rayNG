package com.v2ray.hwidkit

object V2rayNgCompat {

    fun normalizeSubscriptionUrl(rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) return rawUrl
        return HappCrypt.tryDecrypt(rawUrl) ?: rawUrl
    }

    fun expandHappLinksInText(text: String?): String? {
        if (text.isNullOrEmpty()) return text

        val out = ArrayList<String>()
        text.lines().forEach { line ->
            val decrypted = HappCrypt.tryDecrypt(line)
            if (decrypted.isNullOrEmpty()) {
                out.add(line)
            } else {
                decrypted.lines().forEach { out.add(it) }
            }
        }
        return out.joinToString("\n")
    }
}
