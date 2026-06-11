package de.nicerdicer.functions

import de.nicerdicer.util.WoundLocation
import de.nicerdicer.util.WoundType
import de.nicerdicer.util.Wounds
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.string

object WoundFunction : FunctionBase("wounds", "Everything concerning Wounds.")
{
    val severityPattern = Regex("\\d+[cml]")
    var wounds = Wounds()

    override suspend fun prepare(kord: Kord)
    {
        kord.createGlobalChatInputCommand(name, description) {
            string("amount", "e.g. 1m1l") {
                required = true
            }
            string("type", "e.g. bash") {
                required = true
            }
            string("location", "e.g. head, default is random") {
                required = false
            }
        }
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
    {
        val response = event.interaction.deferPublicResponse()
        val amountString = event.interaction.command.strings["amount"]!!
        var c = 0; var m = 0; var l = 0
        severityPattern.findAll(amountString).forEach { matchResult ->
            val woundAmount = matchResult.value.dropLast(1).toInt()
            when (matchResult.value.last())
            {
                'c' -> c += woundAmount
                'm' -> m += woundAmount
                'l' -> l += woundAmount
            }
        }

        val type = WoundType.valueOf(event.interaction.command.strings["type"]!!.uppercase())
        val location = event.interaction.command.strings["location"]?.uppercase()?.let { WoundLocation.valueOf(it) }

        val wounds = wounds.roll(c, m, l, type, location)

        // TODO: do this with embeds
        response.respond {
            content = "Rolled wounds: $wounds"
        }
    }
}