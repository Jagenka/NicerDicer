package de.nicerdicer.functions

import dev.kord.core.Kord
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent

abstract class FunctionBase(public val name: String, public val description: String)
{
    abstract suspend fun prepare(kord: Kord)
    abstract suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
}