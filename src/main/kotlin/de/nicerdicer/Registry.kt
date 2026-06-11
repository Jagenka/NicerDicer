package de.nicerdicer

import de.nicerdicer.functions.AugmentFunction
import de.nicerdicer.functions.CardFunction
import de.nicerdicer.functions.FlawFunction
import de.nicerdicer.functions.FunctionBase
import de.nicerdicer.functions.PerkFunction
import de.nicerdicer.functions.RollFunction
import de.nicerdicer.functions.ShutdownFunction
import de.nicerdicer.functions.WoundFunction
import dev.kord.core.Kord
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent

object Registry
{
    val commands = listOf(
        RollFunction,
        ShutdownFunction,
        CardFunction,
        WoundFunction,
        PerkFunction,
        FlawFunction,
        AugmentFunction
    )

    val commandMap = mutableMapOf<String, FunctionBase>()

    suspend fun prepareCommands(kord: Kord)
    {
        commands.forEach {
            commandMap[it.name] = it
            it.prepare(kord)
        }
    }

    suspend fun handleCommand(event: ChatInputCommandInteractionCreateEvent)
    {
        with(event)
        {
            val cmd = commandMap[interaction.invokedCommandName]
            cmd?.execute(this)
        }
    }
}