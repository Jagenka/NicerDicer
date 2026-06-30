package de.nicerdicer.db

data class PerkEntry(
    val card: String,
    val name: String,
    val text: String,
    val meaning: String
)

data class AugmentEntry(
    val card: String,
    val general: String,
    val blaster: String,
    val breaker: String,
    val brute: String,
    val changer: String,
    val master: String,
    val mover: String,
    val shaker: String,
    val stranger: String,
    val striker: String,
    val tinker: String,
    val thinker: String,
    val trump: String
)

data class WoundEntry(
    val woundType: String,
    val woundSeverity: String,
    val woundLocation: String,
    val woundName: String,
    val woundDescription: String
)

data class TagEntry(
    val name: String,
    val owner: String,              // user id as string
    val content: String
)

data class TerritoryEntry(
    val id: Int,
    val name: String,
    val owner: String?,             // user id as string, null if unowned
    val color: String?              // territory color: "White" (unclaimed), "Yellow" (Good), "Purple" (Evil), "Turquoise" (Quest), "DarkGray" (Challenged)
)

data class AlignmentEntry(
    val guildId: String,
    val userId: String,             // user id as string
    val alignmentOrder: String,     // "Lawful", "Neutral", or "Chaotic"
    val intent: String              // "Good", "Neutral", or "Evil"
)

data class ReputationEntry(
    val guildId: String,
    val userId: String,
    val amount: Int
)

data class FactionEntry(
    val guildId: String,
    val factionOwnerId: String,      // user id as string
    val factionRoleId: String,       // role id as string (role that defines faction color and appearance)
    val name: String,
    val description: String,
    val image: String?,              // URL or path to faction image
    val color: String,               // hex color code for role and territories
    val memberList: String,           // comma-separated list of user IDs
    val alignment: String
)
