package de.nicerdicer.util

class RollResult(val diceType: Int, val amount: Int, val modifier: Int)
{
    private val diceRolls: MutableList<Int> = mutableListOf()
    private var result: Int = 0
    private var isCrit: Boolean = false

    /**
     * Rolls the dice new and sets the result and isCrit. Returns true, if successful or false otherwise.
     */
    fun roll(): Boolean
    {
        if (diceType <= 0 || amount <= 0) return false

        diceRolls.clear()
        repeat(amount)
        {
            val roll = NicerRandom.nextRoll(diceType)
            diceRolls.add(roll)
            if (roll == diceType) isCrit = true
        }
        result = diceRolls.max() + modifier

        return true
    }

    fun getDiceRolls(): List<Int> = diceRolls.toList()

    fun getResult(): Int = result

    fun isCrit() = isCrit

    fun getRollString(): String = "${getInputString()} => ${toString()}"

    fun getInputString(): String = "$amount D$diceType ${if (modifier >= 0) "+" else "-"} $modifier"

    override fun toString(): String
    {
        val sb = StringBuilder()

        sb.append(StringFormatter.formatRolls(diceRolls))
        sb.append(" (+ $modifier) = ")
        sb.append(StringFormatter.formatResult(result, isCrit))

        return sb.toString()
    }
}