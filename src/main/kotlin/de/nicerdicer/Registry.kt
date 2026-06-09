package de.nicerdicer

import de.nicerdicer.functions.FunctionBase
import de.nicerdicer.functions.RollFunction
import de.nicerdicer.functions.ShutdownFunction
import dev.kord.core.Kord
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent

object Registry
{
    val commands = listOf(
        RollFunction,
        ShutdownFunction
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