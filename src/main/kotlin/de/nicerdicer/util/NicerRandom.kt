package de.nicerdicer.util

import kotlin.random.Random

object NicerRandom
{
    var random: Random = Random(System.currentTimeMillis())

    fun newRandomSeed()
    {
        random = Random(System.currentTimeMillis())
    }

    fun nextRoll(diceType: Int): Int = random.nextInt(diceType) + 1
}