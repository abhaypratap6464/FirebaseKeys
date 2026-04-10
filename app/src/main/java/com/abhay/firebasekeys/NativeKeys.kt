package com.abhay.firebasekeys

/**
 * Thin Kotlin bridge to the native keys library.
 *
 * All secrets live XOR-obfuscated in keys.cpp and are compiled into
 * libkeys.so. No plaintext key string exists in the Java/Kotlin layer
 * or in resources, so apktool / dex2jar / jadx extraction yields nothing.
 */
object NativeKeys {
    init {
        System.loadLibrary("keys")
    }

    external fun getGoogleApiKey(): String
    external fun getFirebaseProjectId(): String
    external fun getFirebaseMessagingSenderId(): String
    external fun getFirebaseDatabaseUrl(): String
    external fun getFirebaseAppId(): String
    external fun getFirebaseStorageBucket(): String
}
