package de.nicerdicer.functions

import de.nicerdicer.util.RollResult
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.behavior.interaction.response.createPublicFollowup
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.Channel
import dev.kord.core.entity.effectiveName
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.integer
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.subCommand
import dev.kord.rest.builder.interaction.user

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
                integer("amount", "Amount of dice to roll, important for SW etc.") {
                    required = false
                }
                integer("modifier", "Modifier to add to your initiative roll.") {
                    required = false
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
            subCommand("remind", "Reminds you in the given amount of turns of something!") {
                integer("turns", "In how many turns to remind you.") {
                    required = true
                }
                string("note", "What to remind you of.") {
                    required = true
                }
            }
        }
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
    {
        val channel = event.interaction.getChannel()
        val user = event.interaction.user

        val combat = trackedCombats.getOrPut(channel) { Combat() }
        val subCommand = event.interaction.command.data.options.value?.map { it.name } ?: emptyList()

        when (subCommand.firstOrNull())
        {
            "start" ->
            {
                val response = event.interaction.deferPublicResponse()

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
                val response = event.interaction.deferPublicResponse()

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
                val response = event.interaction.deferPublicResponse()

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
                val response = event.interaction.deferPublicResponse()

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

                val amount = event.interaction.command.integers["amount"]?.toInt() ?: 1
                val modifier = event.interaction.command.integers["modifier"]?.toInt() ?: 4

                val rolls = RollResult(20, amount, modifier)

                combat.rollInitiative(user, rolls)

                val sb = StringBuilder()
                sb.append("${user.mention} rolled a ")
                sb.append(rolls.getRollString())
                sb.append(" for initiative!")

                response.respond {
                    content = sb.toString()
                }
            }

            "end" ->
            {
                val response = event.interaction.deferPublicResponse()

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

                val responseSb = StringBuilder()

                if (combat.endTurn()) responseSb.append("Round ${combat.roundTracker}! ")
                responseSb.append("${combat.combatantToGo!!.user.mention}'s turn!")

                val followup = response.respond {
                    content = responseSb.toString()
                }

                combat.checkForReminders(combat.combatantToGo!!.user)?.let {
                    followup.createPublicFollowup {
                        content = "Reminders:\n$it"
                    }
                }
            }

            "down" ->
            {
                val response = event.interaction.deferPublicResponse()

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
                val response = event.interaction.deferPublicResponse()

                var list = combat.initiativeOrder.map { getMemberName(event.interaction.data.guildId.value, it.user) }
                if (combat.isRunning) list = combat.combatOrder.map { it.user.effectiveName }

                response.respond {
                    content = "Current participants:\n${list.joinToString("\n")}"
                }
            }

            "delay" ->
            {
                val response = event.interaction.deferPublicResponse()

                val targetUser = event.interaction.command.users["user"]!!

                if (!combat.delayAfter(targetUser))
                {
                    response.respond {
                        content = "Delay unsuccessful! To delay, combat needs to be started and you have to delay after someone lower in initiative than you!"
                    }
                    return
                }

                response.respond {
                    content = "${user.mention} now takes their turn after ${targetUser.mention}!\nIt is ${combat.combatantToGo!!.user.mention}'s turn!"
                }
            }

            "remind" ->
            {
                val response = event.interaction.deferEphemeralResponse()

                if (!combat.isRunning)
                {
                    response.respond {
                        content = "Combat hasn't started yet!"
                    }
                    return
                }

                val turnAmount = event.interaction.command.integers["turns"]!!.toInt()
                val note = event.interaction.command.strings["note"]!!

                combat.addReminder(turnAmount, user, note)

                response.respond {
                    content = "Reminding you of '$note' in round ${turnAmount + combat.roundTracker}!"
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

class Combat(val combatOrder: MutableList<Combatant> = mutableListOf(), val trackedReminders: MutableMap<Int, MutableList<Pair<User, String>>> = mutableMapOf())
{
    var isRunning = false
    var turnTracker = 0
    var roundTracker = 1
    var combatantToGo: Combatant? = null
    var initiativeOrder: MutableList<Combatant> = mutableListOf()

    /**
     * Adds a reminder to this combat for the specified user.
     */
    fun addReminder(turnDelay: Int, user: User, note: String)
    {
        trackedReminders.getOrPut(turnDelay + roundTracker) { mutableListOf() }.add(Pair(user, note))
    }

    /**
     * Returns all notes of this user for this round at once.
     */
    fun checkForReminders(user: User): String?
    {
        val roundNotes = trackedReminders.getOrElse(roundTracker) { return null }

        val userNotes = roundNotes.filter { it.first == user }.ifEmpty { return null }.joinToString("\n") { it.second }

        return userNotes
    }

    fun resetReminders() = trackedReminders.clear()

    fun rollInitiative(user: User, rollResult: RollResult)
    {
        if (!rollResult.roll()) throw IllegalStateException("User ${user.effectiveName} does not have dice to roll!")

        initiativeOrder.add(Combatant(user, rollResult))
    }

    fun prepareList()
    {
        val groupedOrder = initiativeOrder.groupBy { it.rollResult.getResult() }.toList().sortedByDescending { it.first }.toMutableList()
        while (groupedOrder.isNotEmpty())
        {
            val (_, currentCombatants) = groupedOrder.removeFirst()
            if (currentCombatants.size == 1) combatOrder.add(currentCombatants.first())
            else combatOrder.addAll(solveCombatantOrder(currentCombatants))
        }
    }

    fun startCombat(): Combatant?
    {
        resetReminders()
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
        roundTracker = 1
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
            if (!combatant.rollResult.roll()) throw IllegalStateException("Combatant ${combatant.user.effectiveName} does not have dice to roll!")

            newCombatants.add(combatant)
        }

        val groupedCombatants = newCombatants.groupBy { it.rollResult.getResult() }.toList().sortedByDescending { it.first }
        val finalCombatants = mutableListOf<Combatant>()

        for (group in groupedCombatants)
        {
            if (group.second.size == 1) finalCombatants.add(group.second.first())
            else finalCombatants.addAll(solveCombatantOrder(group.second))
        }

        return finalCombatants
    }
}

data class Combatant(val user: User, val rollResult: RollResult)