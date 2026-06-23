package de.nicerdicer.util

object StringFormatter
{
    fun formatRolls(rolls: List<Int>): String = rolls.joinToString(" + ") { if (it == rolls.max()) it.toString() else it.toString().stricken() }

    fun formatResult(result: Int, isCrit: Boolean): String
    {
        if (isCrit) return result.toString().bold().underscored()
        return result.toString()
    }
}

fun String.bold(): String = "**$this**"
fun String.italic(): String = "*$this*"
fun String.underscored(): String = "__${this}__"
fun String.stricken(): String = "~~${this}~~"
fun String.box(): String = "```$this```"