package de.nicerdicer.functions

import de.nicerdicer.util.Augment
import de.nicerdicer.util.CsvParserAugments
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.EmbedBuilder

object AugmentFunction : FunctionBase("augment", "Rolls an augment.")
{
    var augmentList = emptyList<Augment>()

    override suspend fun prepare(kord: Kord)
    {
        println("Preparing augments...")
        augmentList = CsvParserAugments().parseCsv("/Perklist - Augments.csv")
        kord.createGlobalChatInputCommand(name, description) {
            string("category", "e.g. Breaker or Thinker") {
                required = true
                for (entry in Classification.entries)
                {
                    val caseCorrectedName = entry.name.lowercase().replaceFirstChar { it.uppercase() }
                    choice(caseCorrectedName, caseCorrectedName)
                }
            }
        }
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
    {
        val response = event.interaction.deferPublicResponse()
        val specifiedCategory = event.interaction.command.strings["category"]!!

        if (!Classification.entries.map { entry -> entry.name }.contains(specifiedCategory.uppercase()))
        {
            response.respond {
                content = "Category $specifiedCategory not found."
            }
            return
        }

        val categoryAsClassification = Classification.valueOf(specifiedCategory.uppercase())

        val rolledAugment = augmentList.random()

        val embedBuilder = EmbedBuilder()
        val footerBuilder = EmbedBuilder.Footer()
        embedBuilder.title = rolledAugment.card.lowercase().replaceFirstChar { it.uppercase() }
        embedBuilder.description = pickFromAugment(rolledAugment, categoryAsClassification)
        footerBuilder.text = specifiedCategory.lowercase().replaceFirstChar { it.uppercase() }
        embedBuilder.footer = footerBuilder

        response.respond {
            content = "Rolling an augment..."
            embeds = mutableListOf(embedBuilder)
        }
    }

    private fun pickFromAugment(augment: Augment, classification: Classification): String
    {
        return when (classification)
        {
            Classification.BLASTER -> augment.blaster
            Classification.BREAKER -> augment.breaker
            Classification.BRUTE -> augment.brute
            Classification.CHANGER -> augment.changer
            Classification.MASTER -> augment.master
            Classification.MOVER -> augment.mover
            Classification.SHAKER -> augment.shaker
            Classification.STRANGER -> augment.stranger
            Classification.STRIKER -> augment.striker
            Classification.TINKER -> augment.tinker
            Classification.THINKER -> augment.thinker
            Classification.TRUMP -> augment.trump
        }
    }
}

enum class Classification
{
    BLASTER,
    BREAKER,
    BRUTE,
    CHANGER,
    MASTER,
    MOVER,
    SHAKER,
    STRANGER,
    STRIKER,
    TINKER,
    THINKER,
    TRUMP;
}