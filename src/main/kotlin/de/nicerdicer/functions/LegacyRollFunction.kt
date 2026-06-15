package de.nicerdicer.functions

import de.nicerdicer.util.RollResult
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.string

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

        val result = RollResult(dieType, amount, modifier)

        if (!result.roll())
        {
            response.respond {
                content = "Dice type and amount have to be greater than 0!"
            }
            return
        }

        val sb = StringBuilder()
        sb.append(result.getRollString())

        response.respond {
            content = sb.toString()
        }
    }
}