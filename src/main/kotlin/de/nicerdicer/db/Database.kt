@file:Suppress("SameParameterValue")

package de.nicerdicer.db

import java.sql.Connection
import java.sql.DriverManager

object Database {
    private const val DB_PATH = "db/nicerdicer.db"
    @Volatile
    private var initialized = false

    private fun connect(): Connection
    {
        try {
            Class.forName("org.sqlite.JDBC")
        } catch (e: ClassNotFoundException) {
            println("Database: SQLite JDBC driver not found. Add dependency org.xerial:sqlite-jdbc. Error: ${e.message}")
            throw e
        }
        return DriverManager.getConnection("jdbc:sqlite:$DB_PATH")
    }

    fun init() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            println("Database: Initializing SQLite DB at $DB_PATH ...")
            try {
                connect().use { conn ->
                    conn.createStatement().use { stmt ->
                        stmt.execute(
                            """
                            CREATE TABLE IF NOT EXISTS augments (
                                card TEXT PRIMARY KEY,
                                general TEXT,
                                blaster TEXT, breaker TEXT, brute TEXT, changer TEXT,
                                master TEXT, mover TEXT, shaker TEXT, stranger TEXT,
                                striker TEXT, tinker TEXT, thinker TEXT, trump TEXT
                            );
                            """.trimIndent()
                        )
                        stmt.execute("CREATE TABLE IF NOT EXISTS power_perks (card TEXT, name TEXT, text TEXT, meaning TEXT);")
                        stmt.execute("CREATE TABLE IF NOT EXISTS life_perks (card TEXT, name TEXT, text TEXT, meaning TEXT);")
                        stmt.execute("CREATE TABLE IF NOT EXISTS power_flaws (card TEXT, name TEXT, text TEXT, meaning TEXT);")
                        stmt.execute("CREATE TABLE IF NOT EXISTS life_flaws (card TEXT, name TEXT, text TEXT, meaning TEXT);")
                        // normalized wounds table (store full structure: type, severity, location, name, description)
                        stmt.execute("CREATE TABLE IF NOT EXISTS wounds (wound_type TEXT, wound_severity TEXT, wound_location TEXT, wound_name TEXT, wound_description TEXT);")
                    }

                    fillTableIfEmpty(conn, "augments", "/Perklist - Augments.csv") { _, row ->
                        conn.prepareStatement(
                            "INSERT OR IGNORE INTO augments(card,general,blaster,breaker,brute,changer,master,mover,shaker,stranger,striker,tinker,thinker,trump) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
                        ).use { ps ->
                            ps.setString(1, row["Card"] ?: "")
                            ps.setString(2, row["General"] ?: "")
                            ps.setString(3, row["Blaster"] ?: "")
                            ps.setString(4, row["Breaker"] ?: "")
                            ps.setString(5, row["Brute"] ?: "")
                            ps.setString(6, row["Changer"] ?: "")
                            ps.setString(7, row["Master"] ?: "")
                            ps.setString(8, row["Mover"] ?: "")
                            ps.setString(9, row["Shaker"] ?: "")
                            ps.setString(10, row["Stranger"] ?: "")
                            ps.setString(11, row["Striker"] ?: "")
                            ps.setString(12, row["Tinker"] ?: "")
                            ps.setString(13, row["Thinker"] ?: "")
                            ps.setString(14, row["Trump"] ?: "")
                            ps.executeUpdate()
                        }
                    }

                    fillSimpleFourColumnCsv(conn, "power_perks", "/Perklist - Power Perks.csv")
                    fillSimpleFourColumnCsv(conn, "life_perks", "/Perklist - Life Perks.csv")
                    fillSimpleFourColumnCsv(conn, "power_flaws", "/Perklist - Power Flaws.csv")
                    fillSimpleFourColumnCsv(conn, "life_flaws", "/Perklist - Life Flaws.csv")

                    // populate normalized wounds table if empty
                    val woundsRows = conn.createStatement().use { st ->
                        st.executeQuery("SELECT count(*) as c FROM wounds").use { rs -> if (rs.next()) rs.getInt("c") else 0 }
                    }
                    if (woundsRows == 0) {
                        try {
                            val parsed = parseWoundsFromJsonResource("/wounds.json")
                            if (parsed.isNotEmpty()) {
                                conn.autoCommit = false
                                try {
                                    conn.prepareStatement("INSERT INTO wounds(wound_type,wound_severity,wound_location,wound_name,wound_description) VALUES(?,?,?,?,?)").use { ps ->
                                        for (w in parsed) {
                                            try {
                                                ps.setString(1, w.woundType)
                                                ps.setString(2, w.woundSeverity)
                                                ps.setString(3, w.woundLocation)
                                                ps.setString(4, w.woundName)
                                                ps.setString(5, w.woundDescription)
                                                ps.addBatch()
                                            } catch (ie: Exception) {
                                                println("Database: failed preparing insert for wound row: ${ie.message}")
                                                ie.printStackTrace()
                                            }
                                        }
                                        ps.executeBatch()
                                    }
                                    conn.commit()
                                    println("Database: inserted ${parsed.size} wounds into wounds table.")
                                } catch (e: Exception) {
                                    conn.rollback()
                                    println("Database: rollback while inserting wounds due to: ${e.message}")
                                    e.printStackTrace()
                                } finally {
                                    conn.autoCommit = true
                                }
                            } else {
                                println("Database: no wounds parsed from JSON; wounds table left empty.")
                            }
                        } catch (e: Exception) {
                            println("Database: failed to parse/insert wounds: ${e.message}")
                            e.printStackTrace()
                        }
                    } else {
                        println("Database: wounds table already populated ($woundsRows rows).")
                    }
                }
                initialized = true
                println("Database: initialization finished.")
            } catch (e: Exception) {
                println("Database: initialization failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun fillSimpleFourColumnCsv(conn: Connection, tableName: String, resourcePath: String) {
        fillTableIfEmpty(conn, tableName, resourcePath) { headers, row ->
            val nameKey = headers.find { it.equals("Perk Name", true) } ?: headers.find { it.equals("Flaw Name", true) } ?: "Perk Name"
            val textKey = headers.find { it.endsWith("Text", true) } ?: headers.find { it.contains("Text", true) } ?: "Perk Text"
            val meaningKey = headers.find { it.equals("CARD MEANING", true) } ?: "CARD MEANING"
            conn.prepareStatement("INSERT INTO $tableName(card,name,text,meaning) VALUES(?,?,?,?)").use { ps ->
                ps.setString(1, row["Card"] ?: "")
                ps.setString(2, row[nameKey] ?: "")
                ps.setString(3, row[textKey] ?: "")
                ps.setString(4, row[meaningKey] ?: "")
                ps.executeUpdate()
            }
        }
    }

    private fun fillTableIfEmpty(conn: Connection, tableName: String, resourcePath: String, inserter: (headers: List<String>, row: Map<String, String>) -> Unit) {
        try {
            val count = conn.createStatement().use { st ->
                st.executeQuery("SELECT count(*) as c FROM $tableName").use { rs -> if (rs.next()) rs.getInt("c") else 0 }
            }
            if (count > 0) {
                println("Database: table $tableName already populated ($count rows).")
                return
            }
            println("Database: populating $tableName from resource $resourcePath ...")
            val (headers, rows) = parseCsvFromResource(resourcePath)
            conn.autoCommit = false
            try {
                for (row in rows) {
                    try {
                        inserter(headers, row)
                    } catch (ie: Exception) {
                        println("Database: failed to insert row into $tableName: ${ie.message}")
                        ie.printStackTrace()
                    }
                }
                conn.commit()
                println("Database: populated $tableName with ${rows.size} rows.")
            } catch (e: Exception) {
                conn.rollback()
                println("Database: rollback populating $tableName due to ${e.message}")
                e.printStackTrace()
            } finally {
                conn.autoCommit = true
            }
        } catch (e: Exception) {
            println("Database: error checking/populating $tableName: ${e.message}")
            e.printStackTrace()
        }
    }

    // Multiline-aware CSV parser that respects quoted fields and escaped quotes ("")
    private fun parseCsvFromResource(resourcePath: String): Pair<List<String>, List<Map<String, String>>> {
        val stream = javaClass.getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("Resource $resourcePath not found.")
        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val records = mutableListOf<List<String>>()
        val curField = StringBuilder()
        val curRecord = mutableListOf<String>()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            when (val c = text[i]) {
                '"' -> {
                    // handle escaped double quote
                    if (inQuotes && i + 1 < text.length && text[i + 1] == '"') {
                        curField.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ',' -> {
                    if (inQuotes) curField.append(c) else {
                        curRecord.add(curField.toString())
                        curField.setLength(0)
                    }
                }
                '\r' -> {
                    if (inQuotes) {
                        curField.append(c)
                    } else {
                        curRecord.add(curField.toString())
                        curField.setLength(0)
                        records.add(ArrayList(curRecord))
                        curRecord.clear()
                        if (i + 1 < text.length && text[i + 1] == '\n') i++
                    }
                }
                '\n' -> {
                    if (inQuotes) curField.append(c) else {
                        curRecord.add(curField.toString())
                        curField.setLength(0)
                        records.add(ArrayList(curRecord))
                        curRecord.clear()
                    }
                }
                else -> curField.append(c)
            }
            i++
        }
        if (inQuotes) {
            println("Database.parseCsvFromResource: warning - file ended while still inside quoted field for $resourcePath")
        }
        if (curField.isNotEmpty() || curRecord.isNotEmpty()) {
            curRecord.add(curField.toString())
            records.add(ArrayList(curRecord))
        }
        if (records.isEmpty()) return Pair(emptyList(), emptyList())
        val headers = records.first().map { it.trim().removeSurrounding("\"").trim() }
        val rows = mutableListOf<Map<String, String>>()
        for (rIdx in 1 until records.size) {
            val row = records[rIdx]
            val map = headers.mapIndexed { idx, h -> h to (if (idx < row.size) row[idx].trim().removeSurrounding("\"") else "") }.toMap()
            rows.add(map)
        }
        return Pair(headers, rows)
    }

    private fun readResourceAsText(resourcePath: String): String {
        val stream = javaClass.getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("Resource $resourcePath not found.")
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    // parse wounds.json into normalized WoundEntry rows (no external JSON dependency)
    private fun parseWoundsFromJsonResource(resourcePath: String): List<WoundEntry> {
        val text = try {
            readResourceAsText(resourcePath)
        } catch (e: Exception) {
            println("Database.parseWoundsFromJsonResource: cannot read resource $resourcePath: ${e.message}")
            return emptyList()
        }
        val results = mutableListOf<WoundEntry>()

        // find top-level keys (wound types) and extract their object blocks
        val topKeyPattern = Regex("\"([^\"]+)\"\\s*:\\s*\\{")
        val matcher = topKeyPattern.findAll(text)
        for (m in matcher) {
            val typeKey = m.groupValues[1]
            val braceStart = text.indexOf('{', m.range.last)
            if (braceStart < 0) continue
            val (typeObj, _) = extractObjectBlock(text, braceStart) ?: continue

            // find severity blocks inside typeObj
            val sevMatches = topKeyPattern.findAll(typeObj)
            for (sm in sevMatches) {
                val severityKey = sm.groupValues[1]
                val sevBraceStart = typeObj.indexOf('{', sm.range.last)
                if (sevBraceStart < 0) continue
                val (sevObj, _) = extractObjectBlock(typeObj, sevBraceStart) ?: continue

                // find location blocks inside sevObj
                val locMatches = topKeyPattern.findAll(sevObj)
                for (lm in locMatches) {
                    val locKey = lm.groupValues[1]
                    val locBraceStart = sevObj.indexOf('{', lm.range.last)
                    if (locBraceStart < 0) continue
                    val (locObj, _) = extractObjectBlock(sevObj, locBraceStart) ?: continue

                    // within locObj, wound entries are name: "description"
                    val entryPattern = Regex("(?s)\"([^\"]+)\"\\s*:\\s*\"(.*?)\"")
                    for (em in entryPattern.findAll(locObj)) {
                        val woundName = em.groupValues[1]
                        val woundDesc = em.groupValues[2].replace("\"\"", "\"").trim()
                        results.add(WoundEntry(typeKey, severityKey, locKey, woundName, woundDesc))
                    }
                }
            }
        }
        println("Database.parseWoundsFromJsonResource: parsed ${results.size} wound entries from $resourcePath")
        return results
    }

    // returns Pair(substringOfObject, endIndex) or null; startIndex must point at '{'
    private fun extractObjectBlock(text: String, startIndex: Int): Pair<String, Int>? {
        var i = startIndex
        var depth = 0
        var inQuotes = false
        while (i < text.length) {
            val c = text[i]
            if (c == '"') {
                // handle escaped quotes
                if (inQuotes && i + 1 < text.length && text[i + 1] == '"') {
                    i += 2
                    continue
                }
                inQuotes = !inQuotes
                i++
                continue
            }
            if (!inQuotes) {
                if (c == '{') depth++
                else if (c == '}') {
                    depth--
                    if (depth == 0) {
                        val obj = text.substring(startIndex, i + 1)
                        return Pair(obj, i)
                    }
                }
            }
            i++
        }
        return null
    }

    // getters to fetch lists into memory for use by functions
    fun getAugments(): List<AugmentEntry> {
        init()
        val list = mutableListOf<AugmentEntry>()
        try {
            connect().use { conn ->
                conn.prepareStatement("SELECT * FROM augments").use { ps ->
                    ps.executeQuery().use { rs ->
                        while (rs.next()) {
                            val a = AugmentEntry(
                                card = rs.getString("card") ?: "",
                                general = rs.getString("general") ?: "",
                                blaster = rs.getString("blaster") ?: "",
                                breaker = rs.getString("breaker") ?: "",
                                brute = rs.getString("brute") ?: "",
                                changer = rs.getString("changer") ?: "",
                                master = rs.getString("master") ?: "",
                                mover = rs.getString("mover") ?: "",
                                shaker = rs.getString("shaker") ?: "",
                                stranger = rs.getString("stranger") ?: "",
                                striker = rs.getString("striker") ?: "",
                                tinker = rs.getString("tinker") ?: "",
                                thinker = rs.getString("thinker") ?: "",
                                trump = rs.getString("trump") ?: ""
                            )
                            list.add(a)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Database.getAugments failed: ${e.message}")
            e.printStackTrace()
        }
        return list
    }

    fun getPerks(tableName: String): List<PerkEntry> {
        init()
        val list = mutableListOf<PerkEntry>()
        try {
            connect().use { conn ->
                conn.prepareStatement("SELECT card, name, text, meaning FROM $tableName").use { ps ->
                    ps.executeQuery().use { rs ->
                        while (rs.next()) {
                            val p = PerkEntry(
                                card = rs.getString("card") ?: "",
                                name = rs.getString("name") ?: "",
                                text = rs.getString("text") ?: "",
                                meaning = rs.getString("meaning") ?: ""
                            )
                            list.add(p)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Database.getPerks($tableName) failed: ${e.message}")
            e.printStackTrace()
        }
        return list
    }

    fun getWounds(): List<WoundEntry> {
        init()
        val list = mutableListOf<WoundEntry>()
        try {
            connect().use { conn ->
                conn.prepareStatement("SELECT wound_type, wound_severity, wound_location, wound_name, wound_description FROM wounds").use { ps ->
                    ps.executeQuery().use { rs ->
                        while (rs.next()) {
                            list.add(
                                WoundEntry(
                                    woundType = rs.getString("wound_type") ?: "",
                                    woundSeverity = rs.getString("wound_severity") ?: "",
                                    woundLocation = rs.getString("wound_location") ?: "",
                                    woundName = rs.getString("wound_name") ?: "",
                                    woundDescription = rs.getString("wound_description") ?: ""
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Database.getWounds failed: ${e.message}")
            e.printStackTrace()
        }
        return list
    }
}