package de.nicerdicer.util

import de.nicerdicer.db.Database
import de.nicerdicer.functions.RolePermissionsFunction.kord
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

    /**
     * Check whether the given user is a moderator for the guild (has the configured mod role).
     */
    suspend fun isModerator(kord: Kord?, guildId: String, user: User): Boolean {
        val modRole = Database.getModRole(guildId) ?: return false
        val g = kord?.getGuild(Snowflake(guildId)) ?: return false
        val member = g.getMember(user.id)
        return member.roleIds.contains(Snowflake(modRole))
    }
}
