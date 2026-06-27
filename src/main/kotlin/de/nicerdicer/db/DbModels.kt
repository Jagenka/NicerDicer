package de.nicerdicer.db

// Lightweight models used by function classes to represent DB rows.
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

// model for normalized wounds extracted from wounds.json
data class WoundEntry(
    val woundType: String,
    val woundSeverity: String,
    val woundLocation: String,
    val woundName: String,
    val woundDescription: String
)

// new: tag model
data class TagEntry(
    val name: String,
    val owner: String,   // user id as string
    val content: String
)

// new: territory model
data class TerritoryEntry(
    val id: Int,
    val name: String,
    val owner: String?,   // user id as string, null if unowned
    val color: String?    // territory color: "White" (unclaimed), "Yellow" (Good), "Purple" (Evil), "Turquoise" (Quest), "DarkGray" (Challenged)
)

// new: alignment model
data class AlignmentEntry(
    val guildId: String,
    val userId: String,   // user id as string
    val alignmentOrder: String,    // "Lawful", "Neutral", or "Chaotic"
    val intent: String    // "Good", "Neutral", or "Evil"
)
