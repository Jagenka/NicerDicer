package de.nicerdicer.functions

import de.nicerdicer.db.Database
import de.nicerdicer.util.bold
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.subCommand
import dev.kord.rest.builder.interaction.string

object AlignmentFunction : FunctionBase("alignment", "Everything to do with alignments.")
{
    val validOrders = listOf("Lawful", "Neutral", "Chaotic")
    val validIntents = listOf("Good", "Neutral", "Evil")

    override suspend fun prepare(kord: Kord)
    {
        kord.createGlobalChatInputCommand(name, description) {
            subCommand("set", "Set your alignment") {
                string("order", "Lawful, Neutral, or Chaotic") { 
                    required = true
                    validOrders.forEach {
                        choice(it, it)
                    }
                }
                string("intent", "Good, Neutral, or Evil") { 
                    required = true
                    validIntents.forEach {
                        choice(it, it)
                    }
                }
            }
            subCommand("show", "Show your alignment") { }
        }
        
        Database.init()
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
    {
        val guildIdVal = event.interaction.data.guildId.value?.toString()
        if (guildIdVal == null)
        {
            event.interaction.respondPublic { content = "This command can only be used in a guild." }
            return
        }

        val response = event.interaction.deferPublicResponse()
        try
        {
            val subCommand = event.interaction.command.data.options.value?.map { it.name }?.first() ?: "show"
            
            when (subCommand)
            {
                "set" ->
                {
                    val order = event.interaction.command.strings["order"]?.trim().orEmpty()
                    val intent = event.interaction.command.strings["intent"]?.trim().orEmpty()
                    
                    if (order.isBlank() || intent.isBlank())
                    {
                        response.respond { content = "Usage: /alignment set order:<Lawful|Neutral|Chaotic> intent:<Good|Neutral|Evil>" }
                        return
                    }

                    if (order !in validOrders || intent !in validIntents)
                    {
                        response.respond { content = "Invalid order or intent. Order must be Lawful, Neutral, or Chaotic. Intent must be Good, Neutral, or Evil." }
                        return
                    }
                    
                    val userId = event.interaction.user.id.toString()
                    val ok = Database.setAlignment(guildIdVal, userId, order, intent)
                    if (ok)
                    {
                        response.respond { content = "Your alignment has been set to ${"$order $intent".bold()}" }
                    }
                    else
                    {
                        response.respond { content = "Failed to set alignment." }
                    }
                }
                
                "show" ->
                {
                    val userId = event.interaction.user.id.toString()
                    val alignment = Database.getAlignment(guildIdVal, userId)
                    if (alignment != null)
                    {
                        val alignmentStr = "${alignment.alignmentOrder} ${alignment.intent}"
                        response.respond { content = "Your alignment is ${"$alignmentStr".bold()}" }
                    }
                    else
                    {
                        response.respond { content = "You have not set an alignment yet. Use /alignment set to set your alignment." }
                    }
                }
                
                else ->
                {
                    response.respond { content = "Unknown subcommand. Use set or show." }
                }
            }
        } catch (e: Exception)
        {
            println("AlignmentFunction.execute: unexpected error: ${e.message}")
            e.printStackTrace()
            response.respond { content = "An internal error occurred while handling the alignment command." }
        }
    }
}