package com.example.tutkdemo

fun byteArrayToHexStr(byteArray: ByteArray): String {
    val hexArray = "0123456789ABCDEF".toCharArray()
    val hexChars = CharArray(byteArray.size * 2)
    for (j in byteArray.indices) {
        val v: Int = byteArray[j].toInt() and 0xFF
        hexChars[j * 2] = hexArray[v ushr 4]
        hexChars[j * 2 + 1] = hexArray[v and 0x0F]
    }
    return String(hexChars)
}