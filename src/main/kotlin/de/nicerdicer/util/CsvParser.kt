package de.nicerdicer.util

import com.opencsv.CSVReader

abstract class CsvParser<T>
{
    abstract fun parseCsv(path: String): List<T>
}

class CsvParserPerks : CsvParser<Perk>()
{
    override fun parseCsv(path: String): List<Perk>
    {
        val perks = mutableListOf<Perk>()
        val bufferedReader = this::class.java.getResourceAsStream(path)?.bufferedReader()

        bufferedReader?.let {
            val csvReader = CSVReader(it)
            val header = csvReader.readNext()

            var line = csvReader.readNext()
            while (line != null)
            {
                perks.add(Perk(line[0], line[1], line[2], line[3]))

                line = csvReader.readNext()
            }
        }

        return perks
    }
}

class CsvParserAugments : CsvParser<Augment>()
{
    override fun parseCsv(path: String): List<Augment>
    {
        val augments = mutableListOf<Augment>()
        val bufferedReader = this::class.java.getResourceAsStream(path)?.bufferedReader()

        bufferedReader?.let {
            val csvReader = CSVReader(it)
            val header = csvReader.readNext()

            var line = csvReader.readNext()
            while (line != null)
            {
                augments.add(
                    Augment(
                        line[0],
                        line[1],
                        line[2],
                        line[3],
                        line[4],
                        line[5],
                        line[6],
                        line[7],
                        line[8],
                        line[9],
                        line[10],
                        line[11],
                        line[12],
                        line[13]
                    )
                )

                line = csvReader.readNext()
            }
        }

        return augments
    }
}

data class Perk(val card: String, val name: String, val text: String, val meaning: String)

data class Augment(
    val card: String,
    val general: String,
    val blaster: String,
    val breaker: String,
    val brute: String,
    val changer: String,
    val master: String,
    val mover: String,
    val shaker: String,
    val stranger: String,
    val striker: String,
    val tinker: String,
    val thinker: String,
    val trump: String
)