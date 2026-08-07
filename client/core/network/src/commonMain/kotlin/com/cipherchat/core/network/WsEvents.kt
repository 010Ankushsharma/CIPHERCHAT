package com.cipherchat.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Events the CLIENT sends to the gateway over the persistent
 * WebSocket. Discriminated by [type] for JSON wire compatibility
 * (polymorphic kotlinx.serialization needs an explicit discriminator
 * when the server is written in a different language/framework than
 * the client, which is the case here: Ktor client <-> Ktor server,
 * but the wire format should remain language-agnostic since other
 * future clients — e.g. a hypothetical web client — must speak it too).
 */
@Serializable
sealed class WsClientEvent {
    abstract val type: String

    @Serializable
    @SerialName("send_encrypted_message")
    data class SendEncryptedMessage(
        val chatId: String,
        val recipientDeviceIds: List<String>,
        /** One ciphertext blob PER recipient device — Signal Protocol's "sealed sender" / per-device fan-out, never one shared ciphertext. */
        val perDeviceCiphertext: Map<String, String>, // deviceId -> base64 ciphertext
        val clientMessageId: String, // client-generated idempotency key
        override val type: String = "send_encrypted_message",
    ) : WsClientEvent()

    @Serializable
    @SerialName("typing_indicator")
    data class TypingIndicator(
        val chatId: String,
        val isTyping: Boolean,
        override val type: String = "typing_indicator",
    ) : WsClientEvent()

    @Serializable
    @SerialName("presence_update")
    data class PresenceUpdate(
        val presenceLabel: String, // serialized form of domain Presence, mapped in core:data
        override val type: String = "presence_update",
    ) : WsClientEvent()

    @Serializable
    @SerialName("read_receipt")
    data class ReadReceipt(
        val chatId: String,
        val messageId: String,
        override val type: String = "read_receipt",
    ) : WsClientEvent()

    @Serializable
    @SerialName("call_signal")
    data class CallSignal(
        val callId: String,
        val targetUserId: String,
        /** Opaque SDP offer/answer/ICE-candidate JSON, opaque to the gateway — it only relays. */
        val signalPayload: String,
        override val type: String = "call_signal",
    ) : WsClientEvent()

    @Serializable
    @SerialName("ghost_mode_toggle")
    data class GhostModeToggle(
        val enabled: Boolean,
        override val type: String = "ghost_mode_toggle",
    ) : WsClientEvent()
}

/**
 * Events the SERVER pushes to the client. Same discriminator pattern
 * as [WsClientEvent]. Every field here is metadata or ciphertext —
 * never plaintext message content, per the server's metadata-only
 * storage/transport guarantee.
 */
@Serializable
sealed class WsServerEvent {
    abstract val type: String

    @Serializable
    @SerialName("encrypted_message")
    data class EncryptedMessage(
        val chatId: String,
        val senderId: String,
        val senderDeviceId: String,
        val ciphertext: String, // base64 — decrypted by core:data via core:crypto, never here
        val serverMessageId: String,
        val sentAtEpochMs: Long,
        override val type: String = "encrypted_message",
    ) : WsServerEvent()

    @Serializable
    @SerialName("typing_indicator")
    data class TypingIndicator(
        val chatId: String,
        val userId: String,
        val isTyping: Boolean,
        override val type: String = "typing_indicator",
    ) : WsServerEvent()

    @Serializable
    @SerialName("presence_update")
    data class PresenceUpdate(
        val userId: String,
        val presenceLabel: String,
        override val type: String = "presence_update",
    ) : WsServerEvent()

    @Serializable
    @SerialName("delivery_receipt")
    data class DeliveryReceipt(
        val messageId: String,
        val deviceId: String,
        val status: String, // "delivered" | "read"
        val atEpochMs: Long,
        override val type: String = "delivery_receipt",
    ) : WsServerEvent()

    @Serializable
    @SerialName("call_signal")
    data class CallSignal(
        val callId: String,
        val fromUserId: String,
        val signalPayload: String,
        override val type: String = "call_signal",
    ) : WsServerEvent()

    @Serializable
    @SerialName("incoming_call")
    data class IncomingCall(
        val callId: String,
        val chatId: String,
        val fromUserId: String,
        val isVideo: Boolean,
        override val type: String = "incoming_call",
    ) : WsServerEvent()

    /** New device linked, identity key rotated, or device revoked — drives Unknown Device Detection + login alerts. */
    @Serializable
    @SerialName("device_security_event")
    data class DeviceSecurityEvent(
        val deviceId: String,
        val eventKind: String, // "linked" | "identity_changed" | "revoked"
        val approximateLocation: String? = null,
        override val type: String = "device_security_event",
    ) : WsServerEvent()

    @Serializable
    @SerialName("prekey_pool_low")
    data class PrekeyPoolLow(
        val remainingCount: Int,
        override val type: String = "prekey_pool_low",
    ) : WsServerEvent()
}
