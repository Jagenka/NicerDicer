package de.nicerdicer.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlin.random.Random

class Wounds
{
    val jsonResource = this.javaClass.getResourceAsStream("/wounds.json")?.bufferedReader()?.readText()
    val woundCatalogue = Json.decodeFromString<Map<WoundType, Map<WoundSeverity, Map<WoundLocation, Map<String, String>>>>>(jsonResource ?: "")
    var processedWoundCatalogue = mutableMapOf<WoundType, MutableMap<WoundSeverity, MutableMap<WoundLocation, MutableList<WoundEffect>>>>()

    init
    {
        woundCatalogue.forEach { (type, map) ->
            processedWoundCatalogue[type] = mutableMapOf()
            map.forEach { (severity, map) ->
                processedWoundCatalogue[type]?.set(severity, mutableMapOf())
                processedWoundCatalogue[type]?.get(severity)?.set(WoundLocation.HEAD, mutableListOf())
                processedWoundCatalogue[type]?.get(severity)?.set(WoundLocation.TORSO, mutableListOf())
                processedWoundCatalogue[type]?.get(severity)?.set(WoundLocation.ARMS, mutableListOf())
                processedWoundCatalogue[type]?.get(severity)?.set(WoundLocation.LEGS, mutableListOf())
                processedWoundCatalogue[type]?.get(severity)?.get(WoundLocation.HEAD)?.addAll(map[WoundLocation.ANY]?.entries?.map { WoundEffect(it.key, it.value) } ?: emptyList())
                processedWoundCatalogue[type]?.get(severity)?.get(WoundLocation.HEAD)?.addAll(map[WoundLocation.HEAD]?.entries?.map { WoundEffect(it.key, it.value) } ?: emptyList())
                processedWoundCatalogue[type]?.get(severity)?.get(WoundLocation.TORSO)?.addAll(map[WoundLocation.ANY]?.entries?.map { WoundEffect(it.key, it.value) } ?: emptyList())
                processedWoundCatalogue[type]?.get(severity)?.get(WoundLocation.TORSO)?.addAll(map[WoundLocation.TORSO]?.entries?.map { WoundEffect(it.key, it.value) } ?: emptyList())
                processedWoundCatalogue[type]?.get(severity)?.get(WoundLocation.ARMS)?.addAll(map[WoundLocation.ANY]?.entries?.map { WoundEffect(it.key, it.value) } ?: emptyList())
                processedWoundCatalogue[type]?.get(severity)?.get(WoundLocation.ARMS)?.addAll(map[WoundLocation.ARMS]?.entries?.map { WoundEffect(it.key, it.value) } ?: emptyList())
                processedWoundCatalogue[type]?.get(severity)?.get(WoundLocation.LEGS)?.addAll(map[WoundLocation.ANY]?.entries?.map { WoundEffect(it.key, it.value) } ?: emptyList())
                processedWoundCatalogue[type]?.get(severity)?.get(WoundLocation.LEGS)?.addAll(map[WoundLocation.LEGS]?.entries?.map { WoundEffect(it.key, it.value) } ?: emptyList())
            }
        }
    }

    fun roll(c: Int = 0, m: Int = 0, l: Int = 0, type: WoundType, location: WoundLocation? = null): List<WoundEffect>
    {
        val woundEffects = mutableListOf<WoundEffect>()
        for (i in 1..c)
        {
            processedWoundCatalogue[type]?.get(WoundSeverity.CRITICAL)?.get(location ?: WoundLocation.roll())?.toList()?.first()?.let { woundEffects.add(it) }
        }
        for (i in 1..m)
        {
            val woundRoll = Random.nextInt(1, 5)
            processedWoundCatalogue[type]?.get(WoundSeverity.MODERATE)?.get(location ?: WoundLocation.roll())?.toList()?.let { woundEffects.add(it[woundRoll]) }
        }
        for (i in 1..l)
        {
            val woundRoll = Random.nextInt(1, 5)
            processedWoundCatalogue[type]?.get(WoundSeverity.LESSER)?.get(location ?: WoundLocation.roll())?.toList()?.let { woundEffects.add(it[woundRoll]) }
        }
        return woundEffects
    }
}

@Serializable
enum class WoundSeverity
{
    @SerialName("Lesser")
    LESSER,

    @SerialName("Moderate")
    MODERATE,

    @SerialName("Critical")
    CRITICAL;
}

@Serializable
enum class WoundType
{
    @SerialName("Cut")
    CUT,

    @SerialName("Bash")
    BASH,

    @SerialName("Pierce")
    PIERCE,

    @SerialName("Rend")
    REND,

    @SerialName("Burn")
    BURN,

    @SerialName("Freeze")
    FREEZE,

    @SerialName("Shock")
    SHOCK,

    @SerialName("Poison")
    POISON;
}

@Serializable
enum class WoundLocation
{
    @SerialName("Any")
    ANY,

    @SerialName("Head")
    HEAD,

    @SerialName("Torso")
    TORSO,

    @SerialName("Arm")
    ARMS,

    @SerialName("Legs")
    LEGS;

    companion object
    {
        fun roll(): WoundLocation
        {
            return when (Random.nextInt(1, 7))
            {
                1 ->
                {
                    LEGS
                }

                2 ->
                {
                    TORSO
                }

                3 ->
                {
                    TORSO
                }

                4 ->
                {
                    TORSO
                }

                5 ->
                {
                    ARMS
                }

                else ->
                {
                    HEAD
                }
            }
        }
    }
}

data class WoundEffect(val name: String, val description: String)