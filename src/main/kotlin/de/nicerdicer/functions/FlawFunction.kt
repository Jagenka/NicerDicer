package de.nicerdicer.functions

import de.nicerdicer.util.CsvParserPerks
import de.nicerdicer.util.Perk
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.subCommand
import dev.kord.rest.builder.message.EmbedBuilder

object FlawFunction : FunctionBase("flaw", "Rolls flaws.")
{
    var flaws = emptyList<Perk>()
    var lifeFlaws = emptyList<Perk>()

    override suspend fun prepare(kord: Kord)
    {
        println("Preparing flaws...")
        flaws = CsvParserPerks().parseCsv("/Perklist - Power Flaws.csv")
        lifeFlaws = CsvParserPerks().parseCsv("/Perklist - Life Flaws.csv")
        kord.createGlobalChatInputCommand("flaw", "Rolls flaws.") {
            subCommand("power", "Rolls a power flaw.")
            subCommand("life", "Rolls a power flaw.")
        }
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
    {
        val response = event.interaction.deferPublicResponse()

        val embedBuilder = EmbedBuilder()
        val footerBuilder = EmbedBuilder.Footer()
        val perkType = event.interaction.command.data.options.value?.map { it.name }?.first()
        var chosenType: String?
        val rolledPerk: Perk = when (perkType)
        {
            "life" ->
            {
                chosenType = "Life"
                lifeFlaws.random()
            }
            else ->
            {
                chosenType = "Power"
                flaws.random()
            }
        }

        embedBuilder.title = rolledPerk.name
        embedBuilder.description = rolledPerk.text
        footerBuilder.text = "${rolledPerk.card}: ${rolledPerk.meaning}"
        embedBuilder.footer = footerBuilder

        response.respond {
            content = "Rolling for a $chosenType flaw..."
            embeds = mutableListOf(embedBuilder)
        }
    }
}