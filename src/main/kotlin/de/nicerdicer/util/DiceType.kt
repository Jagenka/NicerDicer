package de.nicerdicer.util

import kotlin.random.Random

enum class DiceType
{
    COIN
    {
        override fun roll(): Int = Random.nextInt(1, 3)
    },
    D4
    {
        override fun roll(): Int = Random.nextInt(1, 5)
    },
    D6
    {
        override fun roll(): Int = Random.nextInt(1, 7)
    },
    D8
    {
        override fun roll(): Int = Random.nextInt(1, 9)
    },
    D10
    {
        override fun roll(): Int = Random.nextInt(1, 11)
    },
    D12
    {
        override fun roll(): Int = Random.nextInt(1, 13)
    },
    D20
    {
        override fun roll(): Int = Random.nextInt(1, 21)
    },
    D100
    {
        override fun roll(): Int = Random.nextInt(1, 101)
    };

    abstract fun roll(): Int

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