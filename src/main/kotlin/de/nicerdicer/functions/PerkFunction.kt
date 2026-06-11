package de.nicerdicer.functions

import de.nicerdicer.util.CsvParserPerks
import de.nicerdicer.util.Perk
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.subCommand
import dev.kord.rest.builder.message.EmbedBuilder

object PerkFunction : FunctionBase("perk", "Rolls perks.")
{
    var perks = emptyList<Perk>()
    var lifePerks = emptyList<Perk>()

    override suspend fun prepare(kord: Kord)
    {
        println("Preparing perks...")
        perks = CsvParserPerks().parseCsv("/Perklist - Power Perks.csv")
        lifePerks = CsvParserPerks().parseCsv("/Perklist - Life Perks.csv")
        kord.createGlobalChatInputCommand(name, description)
        {
            subCommand("power", "Rolls a power perk.")
            subCommand("life", "Rolls a power perk.")
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
                lifePerks.random()
            }
            else ->
            {
                chosenType = "Power"
                perks.random()
            }
        }

        embedBuilder.title = rolledPerk.name
        embedBuilder.description = rolledPerk.text
        footerBuilder.text = "${rolledPerk.card}: ${rolledPerk.meaning}"
        embedBuilder.footer = footerBuilder

        response.respond {
            content = "Rolling for a $chosenType perk..."
            embeds = mutableListOf(embedBuilder)
        }
    }
}