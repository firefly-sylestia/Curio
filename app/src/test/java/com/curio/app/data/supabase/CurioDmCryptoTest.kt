package com.curio.app.data.supabase

import java.security.KeyPairGenerator
import java.util.Base64
import javax.crypto.AEADBadTagException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CurioDmCryptoTest {
    private val bob = keyPair()
    private val mallory = keyPair()
    private val conversationId = dmConversationId("alice-id", "bob-id")
    private val keyV1 = ByteArray(32) { it.toByte() }
    private val keyV2 = ByteArray(32) { (it + 1).toByte() }

    @Test fun encryptDecrypt_sameDevice() {
        val encrypted = CurioDmCrypto.encryptWithKey(conversationId, keyV1, 1, "hello")
        assertEquals("hello", CurioDmCrypto.decryptWithKey(conversationId, keyV1, encrypted))
        assertEquals(12, Base64.getDecoder().decode(encrypted.nonce).size)
        assertEquals("${CurioDmCrypto.VERSION}:1", encrypted.version)
    }

    @Test fun messageVersionMustBeCanonicalAndPositive() {
        assertEquals(1, CurioDmCrypto.messageKeyVersion("${CurioDmCrypto.VERSION}:1"))
        assertEquals(null, CurioDmCrypto.messageKeyVersion("${CurioDmCrypto.VERSION}:01"))
        assertEquals(null, CurioDmCrypto.messageKeyVersion("${CurioDmCrypto.VERSION}:0"))
    }

    @Test fun senderEnvelopeUnwrapsAndDecryptsOnReceiver() {
        val envelope = CurioDmCrypto.wrapConversationKey(keyV1, identity("bob-device", bob), 1)
        val receiverKey = CurioDmCrypto.unwrapConversationKey(envelope, bob.private)
        assertArrayEquals(keyV1, receiverKey)
        val encrypted = CurioDmCrypto.encryptWithKey(conversationId, keyV1, 1, "between devices")
        assertEquals("between devices", CurioDmCrypto.decryptWithKey(conversationId, receiverKey, encrypted))
    }

    @Test fun wrongRsaPrivateKeyFails() {
        val envelope = CurioDmCrypto.wrapConversationKey(keyV1, identity("bob-device", bob), 1)
        assertThrows(Exception::class.java) { CurioDmCrypto.unwrapConversationKey(envelope, mallory.private) }
    }

    @Test fun wrongKeyNonceAadAndCiphertextFailAuthentication() {
        val encrypted = CurioDmCrypto.encryptWithKey(conversationId, keyV1, 1, "authenticated")
        assertAuthenticationFails { CurioDmCrypto.decryptWithKey(conversationId, keyV2, encrypted) }
        val badNonce = encrypted.copy(nonce = Base64.getEncoder().encodeToString(ByteArray(12) { 7 }))
        assertAuthenticationFails { CurioDmCrypto.decryptWithKey(conversationId, keyV1, badNonce) }
        assertAuthenticationFails { CurioDmCrypto.decryptWithKey("alice-id:bob-other", keyV1, encrypted) }
        val bytes = Base64.getDecoder().decode(encrypted.ciphertext).also { it[0] = (it[0].toInt() xor 1).toByte() }
        val corrupted = encrypted.copy(ciphertext = Base64.getEncoder().encodeToString(bytes))
        assertAuthenticationFails { CurioDmCrypto.decryptWithKey(conversationId, keyV1, corrupted) }
    }

    @Test fun multipleVersionsAndRestartedKeyMaterialDecryptCorrectly() {
        val oldMessage = CurioDmCrypto.encryptWithKey(conversationId, keyV1, 1, "old")
        val newMessage = CurioDmCrypto.encryptWithKey(conversationId, keyV2, 2, "new")
        // A restarted process reads the same durable key material for each exact version.
        val restoredKeys = mapOf(1 to keyV1.copyOf(), 2 to keyV2.copyOf())
        assertEquals("old", CurioDmCrypto.decryptWithKey(conversationId, restoredKeys.getValue(1), oldMessage))
        assertEquals("new", CurioDmCrypto.decryptWithKey(conversationId, restoredKeys.getValue(2), newMessage))
    }

    @Test fun eachRecipientDeviceGetsItsOwnEnvelope() {
        val bobPhone = keyPair()
        val bobTablet = keyPair()
        val phoneEnvelope = CurioDmCrypto.wrapConversationKey(keyV1, identity("bob-phone", bobPhone), 1)
        val tabletEnvelope = CurioDmCrypto.wrapConversationKey(keyV1, identity("bob-tablet", bobTablet), 1)
        assertArrayEquals(keyV1, CurioDmCrypto.unwrapConversationKey(phoneEnvelope, bobPhone.private))
        assertArrayEquals(keyV1, CurioDmCrypto.unwrapConversationKey(tabletEnvelope, bobTablet.private))
    }

    private fun assertAuthenticationFails(block: () -> Unit) {
        assertThrows(AEADBadTagException::class.java, block)
    }

    private fun keyPair() = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    private fun identity(deviceId: String, pair: java.security.KeyPair) =
        CurioDmIdentity(deviceId, Base64.getEncoder().encodeToString(pair.public.encoded))
}
