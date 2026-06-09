package de.nicerdicer.util

import kotlin.random.Random

enum class DiceType
{
    COIN
    {
        override fun roll(): Int = Random.nextInt(1, 3)
        override fun maxRoll(): Int = 2
    },
    D4
    {
        override fun roll(): Int = Random.nextInt(1, 5)
        override fun maxRoll(): Int = 4
    },
    D6
    {
        override fun roll(): Int = Random.nextInt(1, 7)
        override fun maxRoll(): Int = 6
    },
    D8
    {
        override fun roll(): Int = Random.nextInt(1, 9)
        override fun maxRoll(): Int = 8
    },
    D10
    {
        override fun roll(): Int = Random.nextInt(1, 11)
        override fun maxRoll(): Int = 10
    },
    D12
    {
        override fun roll(): Int = Random.nextInt(1, 13)
        override fun maxRoll(): Int = 12
    },
    D20
    {
        override fun roll(): Int = Random.nextInt(1, 21)
        override fun maxRoll(): Int = 20
    },
    D100
    {
        override fun roll(): Int = Random.nextInt(1, 101)
        override fun maxRoll(): Int = 100
    };

    abstract fun roll(): Int

    abstract fun maxRoll(): Int

    companion object {
        public fun asDiceType(rollString: String): DiceType
        {
            return when(rollString.uppercase()) {
                "COIN" -> COIN
                "D2" -> COIN
                "D4" -> D4
                "D6" -> D6
                "D8" -> D8
                "D10" -> D10
                "D12" -> D12
                "D20" -> D20
                "D100" -> D100
                else -> throw IllegalArgumentException("Invalid roll string: $rollString")
            }
        }
    }
}