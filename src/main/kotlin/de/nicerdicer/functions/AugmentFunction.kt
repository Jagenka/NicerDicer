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
                for (entry in Category.entries)
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

        if (!Category.entries.map { entry -> entry.name }.contains(specifiedCategory.uppercase()))
        {
            response.respond {
                content = "Category $specifiedCategory not found."
            }
            return
        }

        val categoryAsCategory = Category.valueOf(specifiedCategory.uppercase())

        val rolledAugment = augmentList.random()

        val embedBuilder = EmbedBuilder()
        val footerBuilder = EmbedBuilder.Footer()
        embedBuilder.title = rolledAugment.card.lowercase().replaceFirstChar { it.uppercase() }
        embedBuilder.description = pickFromAugment(rolledAugment, categoryAsCategory)
        footerBuilder.text = specifiedCategory.lowercase().replaceFirstChar { it.uppercase() }
        embedBuilder.footer = footerBuilder

        response.respond {
            content = "Rolling an augment..."
            embeds = mutableListOf(embedBuilder)
        }
    }

    private fun pickFromAugment(augment: Augment, category: Category): String
    {
        return when (category)
        {
            Category.BLASTER -> augment.blaster
            Category.BREAKER -> augment.breaker
            Category.BRUTE -> augment.brute
            Category.CHANGER -> augment.changer
            Category.MASTER -> augment.master
            Category.MOVER -> augment.mover
            Category.SHAKER -> augment.shaker
            Category.STRANGER -> augment.stranger
            Category.STRIKER -> augment.striker
            Category.TINKER -> augment.tinker
            Category.THINKER -> augment.thinker
            Category.TRUMP -> augment.trump
        }
    }
}

enum class Category
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