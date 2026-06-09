package de.nicerdicer.functions

import de.nicerdicer.util.DiceType
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.integer
import dev.kord.rest.builder.interaction.string

object RollFunction : FunctionBase("roll", "Roll a function")
{
    override suspend fun prepare(kord: Kord)
    {
        kord.createGlobalChatInputCommand("roll", "Rolls the dice!") {
            string("roll_string", "What type of die to use; e.g. d6") {
                required = true
                for (diceType in DiceType.entries)
                {
                    choice(diceType.name, diceType.name)
                }
            }
            integer(name = "roll_amount", "How many dice to throw; e.g. 3") {
                required = true
            }
            integer(name = "roll_modifier", "Value to add to the result; e.g. 4 or -3") {
                required = true
            }
        }
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
    {
        val response = event.interaction.deferPublicResponse()
        val rollString = event.interaction.command.strings["roll_string"]!!
        val diceType = try {
            DiceType.asDiceType(rollString)
        } catch (e: IllegalArgumentException) {
            return
        }
        val diceAmount = event.interaction.command.integers["roll_amount"]!!
        val modifier = event.interaction.command.integers["roll_modifier"]!!
        var finalValue = 0

        for (i in 1..diceAmount)
        {
            finalValue += diceType.roll()
        }
        finalValue += modifier.toInt()

        response.respond {
            content = "Result from $diceAmount$rollString, Modifier $modifier: $finalValue!"
        }
    }
}