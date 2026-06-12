package de.nicerdicer.functions

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.Channel
import dev.kord.core.entity.effectiveName
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.integer
import dev.kord.rest.builder.interaction.subCommand
import dev.kord.rest.builder.interaction.user
import java.util.LinkedList
import kotlin.random.Random

object CombatFunction : FunctionBase("combat", "Everything relating to combat.")
{
    var kord: Kord? = null
    val trackedCombats = mutableMapOf<Channel, Combat>()

    override suspend fun prepare(kord: Kord)
    {
        this.kord = kord
        kord.createGlobalChatInputCommand(name, description) {
            subCommand("start", "Starts combat!")
            subCommand("finish", "Finish combat!")
            subCommand("leave", "Leave combat.")
            subCommand("initiative", "Rolls init!") {
                integer("modifier", "Modifier to add to your initiative roll.") {
                    required = true
                }
            }
            subCommand("end", "Ends your turn.")
            subCommand("down", "Removes you from combat.")
            subCommand("list", "Lists everyone in combat.")
            subCommand("delay", "Delays your turn until after the given person's") {
                user("user", "User to delay your turn after.") {
                    required = false
                    autocomplete = true
                }
            }
        }
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
    {
        val channel = event.interaction.getChannel()
        val user = event.interaction.user

        if (!trackedCombats.containsKey(channel)) trackedCombats[channel] = Combat()

        val combat = trackedCombats[channel]!!
        val subCommand = event.interaction.command.data.options.value?.map { it.name } ?: emptyList()

        val response = event.interaction.deferPublicResponse()

        when (subCommand.firstOrNull())
        {
            "start" ->
            {
                if (combat.isRunning)
                {
                    response.respond {
                        content = "Combat has already started!"
                    }
                    return
                }

                val userToGo = combat.startCombat()

                userToGo?.let {
                    response.respond {
                        content = "Combat started! It is ${it.user.mention}'s turn!"
                    }
                } ?: response.respond {
                    content = "There are not enough people in combat! You need at least two!"
                }
            }

            "finish" ->
            {
                if (!combat.isRunning)
                {
                    response.respond {
                        content = "No combat has started yet!"
                    }
                    return
                }

                val winners = combat.finishCombat()

                if (winners.isEmpty())
                {
                    response.respond { content = "In war, there are no winners. (Just like in this combat, what the fuck did you do?)" }
                    return
                }

                response.respond {
                    content = "Combat over! Winners are: ${winners.joinToString(", ") { winner -> winner.user.mention }}"
                }
            }

            "leave" ->
            {
                val removed = combat.initiativeOrder.removeIf { it.user == user }

                if (removed)
                {
                    response.respond {
                        content = "${user.mention} left the combat!"
                    }
                    return
                }

                response.respond {
                    content = "You are not in combat!"
                }
            }

            "initiative" ->
            {
                if (combat.isRunning)
                {
                    response.respond {
                        content = "Combat is already running!"
                    }
                    return
                }

                if (combat.initiativeOrder.map { it.user }.contains(user))
                {
                    response.respond {
                        content = "You are already in combat!"
                    }
                    return
                }

                val modifier = event.interaction.command.integers["modifier"]!!
                val rolledInit = combat.rollInitiative(user, modifier.toInt())

                response.respond {
                    content = "${user.mention} rolled a $rolledInit for initiative!"
                }
            }

            "end" ->
            {
                if (!combat.isRunning)
                {
                    response.respond {
                        content = "Combat hasn't started yet!"
                    }
                    return
                }

                if (combat.combatantToGo!!.user != user)
                {
                    response.respond {
                        content = "Not your turn! It's ${combat.combatantToGo!!.user.mention}'s turn!"
                    }
                    return
                }

                if (combat.endTurn())
                {
                    response.respond {
                        content = "Round ${combat.roundTracker}! ${combat.combatantToGo!!.user.mention}'s turn!"
                    }
                }
                else response.respond {
                    content = "${combat.combatantToGo!!.user.mention}'s turn!"
                }
            }

            "down" ->
            {
                if (!combat.isRunning)
                {
                    response.respond {
                        content = "Combat hasn't started yet!"
                    }
                    return
                }

                val removed = combat.combatOrder.removeIf { it.user == user }

                if (removed)
                {
                    response.respond {
                        content = "${user.mention} got downed!"
                    }
                    return
                }

                response.respond {
                    content = "You are not in combat!"
                }
            }

            "list" ->
            {
                var list = combat.initiativeOrder.map { getMemberName(event.interaction.data.guildId.value, it.user) }
                if (combat.isRunning) list = combat.combatOrder.map { it.user.effectiveName }

                response.respond {
                    content = "Current participants:\n${list.joinToString("\n")}"
                }
            }

            "delay" ->
            {
                val targetUser = event.interaction.command.users["user"]!!

                if (!combat.delayAfter(targetUser))
                {
                    response.respond {
                        content = "Delay unsuccessful! To delay, you have to delay after someone lower in initiative than you!"
                    }
                    return
                }
                else response.respond {
                    content = "${user.mention} now takes their turn after ${targetUser.mention}!\nIt is ${combat.combatantToGo!!.user.mention}'s turn!"
                }
            }
        }
    }

    private suspend fun getMemberName(guildId: Snowflake?, user: User): String
    {
        guildId?.let {
            kord?.getGuild(it)?.getMember(user.id)?.effectiveName?.let { memberName ->
                return memberName
            }
        }
        return user.effectiveName
    }
}

class Combat(val combatOrder: MutableList<Combatant> = mutableListOf())
{
    var isRunning = false
    var turnTracker = 0
    var roundTracker = 1
    var combatantToGo: Combatant? = null
    var initiativeOrder: MutableList<Combatant> = mutableListOf()

    fun rollInitiative(user: User, modifier: Int): Int
    {
        val roll = Random.nextInt(1, 21) + modifier
        initiativeOrder.add(Combatant(roll, user, modifier))
        return roll
    }

    fun prepareList()
    {
        val groupedOrder = initiativeOrder.groupBy { it.trackedInitiative }.toList().sortedByDescending { it.first }.toMutableList()
        while (groupedOrder.isNotEmpty())
        {
            val (_, currentCombatants) = groupedOrder.removeFirst()
            if (currentCombatants.size == 1) combatOrder.add(currentCombatants.first())
            else combatOrder.addAll(solveCombatantOrder(currentCombatants))
        }
    }

    fun startCombat(): Combatant?
    {
        prepareList()
        if (combatOrder.size <= 1) return null
        isRunning = true
        combatantToGo = combatOrder.first()
        return combatantToGo
    }

    fun finishCombat(): List<Combatant>
    {
        isRunning = false
        val winners = combatOrder.toList()
        combatOrder.clear()
        initiativeOrder.clear()
        return winners
    }

    /**
     * Returns true, if delay was successful, false otherwise.
     */
    fun delayAfter(targetUser: User): Boolean
    {
        combatantToGo?.let { ctg ->
            val ownIndex = combatOrder.map { it.user }.indexOf(ctg.user)
            val indexToInsertAt = combatOrder.map { it.user }.indexOf(targetUser)
            if (ownIndex >= indexToInsertAt) return false
            if (!combatOrder.remove(ctg)) return false
            combatOrder.add(indexToInsertAt, ctg)

            combatantToGo = combatOrder[turnTracker]
            return true
        }
        return false
    }

    /**
     * Returns true, if this also was the round end, false otherwise.
     */
    fun endTurn(): Boolean
    {
        turnTracker++
        if (turnTracker >= combatOrder.size)
        {
            endRound()
            return true
        }
        combatantToGo = combatOrder[turnTracker]
        return false
    }

    fun endRound()
    {
        roundTracker++
        turnTracker = 0
        combatantToGo = combatOrder[turnTracker]
    }

    /**
     * Expects a list of combatants with the SAME trackedInitiative.
     */
    private fun solveCombatantOrder(combatants: List<Combatant>): List<Combatant>
    {
        val newCombatants = mutableListOf<Combatant>()

        for (combatant in combatants)
        {
            val roll = Random.nextInt(1, 21) + combatant.modifier
            newCombatants.add(Combatant(roll, combatant.user, combatant.modifier))
        }

        val groupedCombatants = newCombatants.groupBy { it.trackedInitiative }.toList().sortedByDescending { it.first }
        val finalCombatants = mutableListOf<Combatant>()

        for (group in groupedCombatants)
        {
            if (group.second.size == 1) finalCombatants.add(group.second.first())
            else finalCombatants.addAll(solveCombatantOrder(group.second))
        }

        return finalCombatants
    }
}

data class Combatant(val trackedInitiative: Int, val user: User, val modifier: Int)