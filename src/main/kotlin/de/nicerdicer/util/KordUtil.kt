package de.nicerdicer.util

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.User
import dev.kord.core.entity.effectiveName

object KordUtil {
    /**
     * Returns the guild member's effective name when possible, otherwise falls back to user.effectiveName.
     * Logs errors verbosely and never throws.
     */
    suspend fun getMemberName(kord: Kord?, guildId: Snowflake?, user: User): String {
        guildId?.let {
            try {
                val guild = kord?.getGuild(it)
                val member = guild?.getMember(user.id)
                member?.effectiveName?.let { memberName ->
                    return memberName
                }
            } catch (e: Exception) {
                println("KordUtil.getMemberName: failed to fetch member name for user=${user.id} guild=$guildId: ${e.message}")
                e.printStackTrace()
            }
        }
        return user.effectiveName
    }

    suspend fun getMemberName(kord: Kord?, guildId: Snowflake?, userId: Snowflake): String {
        guildId?.let {
            try {
                val guild = kord?.getGuild(it)
                val member = guild?.getMember(userId)
                member?.effectiveName?.let { memberName ->
                    return memberName
                }
            } catch (e: Exception) {
                println("KordUtil.getMemberName: failed to fetch member name for user=$userId guild=$guildId: ${e.message}")
                e.printStackTrace()
            }
        }
        return "Unknown User"
    }
}
