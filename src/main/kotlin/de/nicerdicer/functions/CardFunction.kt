package de.nicerdicer.functions

import de.nicerdicer.util.CardDeck
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent

object CardFunction : FunctionBase("draw", "Represents a deck of cards.") {
    val deck = CardDeck()

    override suspend fun prepare(kord: Kord) {
        kord.createGlobalChatInputCommand(name, description)
        deck.shuffle()
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent) {
        val response = event.interaction.deferPublicResponse()
        val card = deck.draw()

        response.respond {
            content = card?.let { "You drew a ${card.toShortString()}!" } ?: "The deck is empty!"
        }
    }
}