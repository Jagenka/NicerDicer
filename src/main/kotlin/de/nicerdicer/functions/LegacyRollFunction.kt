package de.nicerdicer.functions

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.string
import kotlin.math.absoluteValue
import kotlin.random.Random

object LegacyRollFunction : FunctionBase("r", "Roll function via a string.")
{
    override suspend fun prepare(kord: Kord)
    {
        kord.createGlobalChatInputCommand(name, description) {
            string("roll_string", "What to roll. Format: 3d20+4") {
                required = false
            }
        }
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
    {
        val response = event.interaction.deferPublicResponse()
        val rs = event.interaction.command.strings["roll_string"] ?: "3d20+4"

        val amount = Regex("\\d+[dD]").find(rs)?.value?.dropLast(1)?.toInt() ?: 3
        val dieType = Regex("[dD]\\d+").find(rs)?.value?.drop(1)?.toInt() ?: 20
        val modifier = Regex("[+-]\\d+").find(rs)?.value?.toInt() ?: 4

        val dieResults: MutableList<Int> = mutableListOf()
        var isCrit = false

        for (i in 1..amount)
        {
            val roll = Random.nextInt(1, dieType + 1)
            if (roll == dieType) isCrit = true
            dieResults.add(roll)
        }

        val finalValue = dieResults.maxOrNull()?.let { it + modifier }

        val sb = StringBuilder()
        sb.append("Rolled for $amount D$dieType ")
        if (modifier >= 0) sb.append("+ $modifier: ")
        else sb.append("- $modifier: ")
        var firstResult = true
        for (roll in dieResults)
        {
            if (!firstResult) sb.append(" + ")
            if (roll == dieResults.max()) sb.append(roll)
            else sb.append("~~${roll}~~")
            firstResult = false
        }
        if (modifier >= 0) sb.append(" (+ ${modifier.absoluteValue})")
        else sb.append(" (- ${modifier.absoluteValue})")
        sb.append(" = ")
        if (isCrit) sb.append("__**$finalValue**__")
        else sb.append("$finalValue")

        response.respond {
            content = sb.toString()
        }
    }
}