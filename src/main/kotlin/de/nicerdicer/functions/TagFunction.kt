package de.nicerdicer.functions

import de.nicerdicer.db.Database
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.subCommand

object TagFunction : FunctionBase("tag", "Show given tag.")
{
    override suspend fun prepare(kord: Kord)
    {
        // register command with subcommands: create, edit, delete, get, list
        kord.createGlobalChatInputCommand(name, description) {
            subCommand("create", "Create a new tag") {
                string("name", "Tag name") { required = true }
                string("content", "Tag content") { required = true }
            }
            subCommand("edit", "Edit an existing tag (owner only)") {
                string("name", "Tag name") { required = true }
                string("content", "New tag content") { required = true }
            }
            subCommand("delete", "Delete a tag (owner only)") {
                string("name", "Tag name") { required = true }
            }
            subCommand("get", "Show a tag") {
                string("name", "Tag name") { required = true }
            }
            subCommand("list", "List your tags") {
                // no options
            }
        }
        // ensure DB ready
        Database.init()
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
    {
        val response = event.interaction.deferPublicResponse()
        try
        {
            val subCommand = event.interaction.command.data.options.value?.map { it.name }?.first() ?: "get"

            when (subCommand)
            {
                "create" ->
                {
                    val name = (event.interaction.command.strings["name"] ?: "").trim()
                    val responseContent = event.interaction.command.strings["content"] ?: ""
                    if (name.isBlank() || responseContent.isBlank())
                    {
                        response.respond { content = "Usage: /tag create name:<name> content:<text>" }
                        return
                    }
                    val ownerId = event.interaction.user.id.toString()
                    val ok = Database.createTag(name, ownerId, responseContent)
                    if (ok) response.respond { content = "Tag '$name' created." }
                    else response.respond { content = "Failed to create tag '$name' — it may already exist (case-insensitive) or an error occurred." }
                }

                "edit" ->
                {
                    val name = (event.interaction.command.strings["name"] ?: "").trim()
                    val responseContent = event.interaction.command.strings["content"] ?: ""
                    if (name.isBlank() || responseContent.isBlank())
                    {
                        response.respond { content = "Usage: /tag edit name:<name> content:<text>" }
                        return
                    }
                    val ownerId = event.interaction.user.id.toString()
                    val ok = Database.updateTag(name, ownerId, responseContent)
                    if (ok) response.respond { content = "Tag '$name' updated." }
                    else response.respond { content = "Failed to update tag '$name' — it might not exist or you might not be the owner." }
                }

                "delete" ->
                {
                    val name = (event.interaction.command.strings["name"] ?: "").trim()
                    if (name.isBlank())
                    {
                        response.respond { content = "Usage: /tag delete name:<name>" }
                        return
                    }
                    val ownerId = event.interaction.user.id.toString()
                    val ok = Database.deleteTag(name, ownerId)
                    if (ok) response.respond { content = "Tag '$name' deleted." }
                    else response.respond { content = "Failed to delete tag '$name' — it might not exist or you might not be the owner." }
                }

                "list" ->
                {
                    val ownerId = event.interaction.user.id.toString()
                    val tags = Database.listTagsByOwner(ownerId)
                    if (tags.isEmpty()) response.respond { content = "You have no tags." }
                    else
                    {
                        val names = tags.joinToString(", ") { it.name }
                        response.respond { content = "Your tags (${tags.size}): $names" }
                    }
                }

                else ->
                { // default / get <name>
                    val nameInput = (event.interaction.command.strings["name"] ?: subCommand).trim()
                    if (nameInput.isBlank()) {
                        response.respond { content = "Usage: /tag get name:<name>" }
                        return
                    }
                    Database.getTag(nameInput)?.let {
                        response.respond { content = it.content }
                        return
                    }
                    response.respond { content = "Tag '$nameInput' not found." }
                }
            }
        } catch (e: Exception)
        {
            println("TagFunction.execute: unexpected error: ${e.message}")
            e.printStackTrace()
            response.respond { content = "An internal error occurred while handling the tag command." }
        }
    }
}