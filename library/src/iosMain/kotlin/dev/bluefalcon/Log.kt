package dev.bluefalcon

actual fun log(message: String) = if (isDebug) println( "BTBF $message" ) else {}