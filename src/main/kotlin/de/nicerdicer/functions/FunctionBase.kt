package de.nicerdicer.functions

import dev.kord.core.Kord
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent

abstract class FunctionBase(val name: String, val description: String)
{
    abstract suspend fun prepare(kord: Kord)
    abstract suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
}