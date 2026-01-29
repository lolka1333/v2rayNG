package com.v2ray.ang.util

object HappCrypt {
    fun tryDecrypt(input: String?): String? = com.v2ray.hwidkit.HappCrypt.tryDecrypt(input)
}
