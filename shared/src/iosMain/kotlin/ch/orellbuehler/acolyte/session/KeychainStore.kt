package ch.orellbuehler.acolyte.session

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class KeychainStore : SecureStore {
    private val service = "ch.orellbuehler.acolyte"

    override fun write(
        key: String,
        value: String,
    ) {
        remove(key)
        val data = value.encodeToByteArray().toNSData()
        val retainedService = CFBridgingRetain(service)
        val retainedKey = CFBridgingRetain(key)
        val retainedData = CFBridgingRetain(data)
        val query = CFDictionaryCreateMutable(null, 5, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, retainedService)
        CFDictionaryAddValue(query, kSecAttrAccount, retainedKey)
        CFDictionaryAddValue(query, kSecValueData, retainedData)
        CFDictionaryAddValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        SecItemAdd(query, null)
        CFBridgingRelease(query)
        CFBridgingRelease(retainedService)
        CFBridgingRelease(retainedKey)
        CFBridgingRelease(retainedData)
    }

    override fun read(key: String): String? {
        val retainedService = CFBridgingRetain(service)
        val retainedKey = CFBridgingRetain(key)
        val query = CFDictionaryCreateMutable(null, 5, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, retainedService)
        CFDictionaryAddValue(query, kSecAttrAccount, retainedKey)
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)
        val result =
            memScoped {
                val found = alloc<CFTypeRefVar>()
                if (SecItemCopyMatching(query, found.ptr) == errSecSuccess) {
                    CFBridgingRelease(found.value) as? NSData
                } else {
                    null
                }
            }
        CFBridgingRelease(query)
        CFBridgingRelease(retainedService)
        CFBridgingRelease(retainedKey)
        return result?.toByteArray()?.decodeToString()
    }

    override fun remove(key: String) {
        val retainedService = CFBridgingRetain(service)
        val retainedKey = CFBridgingRetain(key)
        val query = CFDictionaryCreateMutable(null, 3, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, retainedService)
        CFDictionaryAddValue(query, kSecAttrAccount, retainedKey)
        SecItemDelete(query)
        CFBridgingRelease(query)
        CFBridgingRelease(retainedService)
        CFBridgingRelease(retainedKey)
    }

    private fun ByteArray.toNSData(): NSData {
        if (isEmpty()) return NSData()
        return usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = size.toULong()) }
    }

    private fun NSData.toByteArray(): ByteArray {
        val out = ByteArray(length.toInt())
        if (out.isEmpty()) return out
        out.usePinned { pinned -> platform.posix.memcpy(pinned.addressOf(0), bytes, length) }
        return out
    }
}
