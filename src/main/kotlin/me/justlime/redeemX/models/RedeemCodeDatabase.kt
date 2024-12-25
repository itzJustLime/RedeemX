package me.justlime.redeemX.models

import java.sql.Timestamp

data class RedeemCodeDatabase(
    val code: String,
    val enabled: Boolean,
    val template: String,
    val locked: Boolean,
    val duration: String,
    val cooldown: String,
    val permission: String,
    val pin: Int,
    val redemption: Int,
    val playerLimit: Int,
    val usedBy: String,
    val validFrom: Timestamp,
    val lastRedeemed: String,
    val target: String,
    val commands: String,
    val created_at: Timestamp = Timestamp(System.currentTimeMillis()),
    val last_modified: Timestamp = Timestamp(System.currentTimeMillis())
)