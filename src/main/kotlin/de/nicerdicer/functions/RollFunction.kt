package de.nicerdicer.functions

import de.nicerdicer.util.DiceType
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.integer
import dev.kord.rest.builder.interaction.string

object RollFunction : FunctionBase("roll", "Rolls the dice!") {
    override suspend fun prepare(kord: Kord) {
        kord.createGlobalChatInputCommand(name, description) {
            integer(name = "roll_amount", "How many dice to throw; e.g. 3") {
                required = false
            }
            integer(name = "roll_modifier", "Value to add to the result; e.g. 4 or -3") {
                required = false
            }
            string("dice_type", "What type of die to use; e.g. d6") {
                required = false
                for (diceType in DiceType.entries) {
                    choice(diceType.name, diceType.name)
                }
            }
            string("note", "e.g. Attack! or DC14 Guts Save") {
                required = false
            }
        }
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent) {
        val response = event.interaction.deferPublicResponse()
        val diceType = try {
            event.interaction.command.strings["dice_type"]?.let {
                DiceType.asDiceType(it)
            } ?: DiceType.D20
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            response.respond {
                content = "Something went wrong"
            }
            return
        }
        val diceAmount = event.interaction.command.integers["roll_amount"] ?: 3
        val modifier = event.interaction.command.integers["roll_modifier"] ?: 4
        val results = mutableListOf<Int>()

        for (i in 1..diceAmount) {
            results.add(diceType.roll())
        }

        val sb = StringBuilder()
        val operator = if (modifier >= 0) '+' else '-'
        sb.append("Result from $diceAmount ${diceType.name} $operator $modifier => ")
        var first = true
        var hasCrit = false
        for (result in results) {
            if (first)  first = false
            else sb.append(" + ")
            if (result == results.max()) sb.append("$result")
            else sb.append("~~$result~~")
            if (result == diceType.maxRoll()) hasCrit = true
        }
        val finalValue = results.max() + modifier.toInt()
        if (hasCrit) sb.append(" ( $operator $modifier) = __**$finalValue**__")
        else sb.append(" ( $operator $modifier) = $finalValue")

        event.interaction.command.strings["note"]?.let {
            sb.append(": $it")
        }

        response.respond {
            content = sb.toString()
        }
    }
}