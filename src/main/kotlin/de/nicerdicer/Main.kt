package de.nicerdicer

import dev.kord.core.Kord
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object Main
{
    val token = this.javaClass.getResourceAsStream("/token.txt")?.bufferedReader()?.readText()
    var kord: Kord? = null
        private set

    @JvmStatic
    fun main(args: Array<String>)
    {
        runBlocking {
            launch { bot() }
        }
    }

    private suspend fun bot()
    {
        if (token == null) return
        kord = Kord(token)

        kord?.let { kord ->
            kord.getGlobalApplicationCommands().collect {
                it.delete()
            }
            println("Old commands were deleted!")

            Registry.prepareCommands(kord)
            println("New commands were registered!")

            kord.on<ChatInputCommandInteractionCreateEvent> {
                Registry.handleCommand(this)
            }
            println("Event handlers set!")

            kord.login()
        }
    }
}