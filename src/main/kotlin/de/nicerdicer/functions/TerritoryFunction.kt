package de.nicerdicer.functions

import de.nicerdicer.db.Database
import de.nicerdicer.util.KordUtil.getMemberName
import de.nicerdicer.util.bold
import de.nicerdicer.util.box
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.subCommand
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.addFile
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import java.util.ArrayDeque
import java.awt.Color
import java.nio.file.Files
import java.nio.file.Path

object TerritoryFunction : FunctionBase("territory", "Everything concerning territories.")
{
    var kord: Kord? = null

    // Map of territory id -> starting pixel coordinate (x,y) inside the region.
    // Replace / extend these placeholder coordinates with your real coordinates.
    private val TERRITORY_COORDS: Map<Int, Pair<Int, Int>> = mapOf(
        1 to (200 to 150),
        2 to (300 to 300),
        3 to (450 to 450),
        4 to (400 to 600),
        5 to (250 to 900),
        6 to (500 to 900),
        7 to (700 to 1000),
        8 to (900 to 950),
        9 to (950 to 500),
        10 to (1000 to 300),
        11 to (1000 to 100),
        12 to (800 to 100),
        13 to (600 to 200),
        14 to (800 to 300),
        15 to (700 to 400),
        16 to (850 to 500),
        17 to (600 to 600),
        18 to (600 to 750),
        19 to (750 to 850),
        20 to (1000 to 750)
    )

    override suspend fun prepare(kord: Kord)
    {
        this.kord = kord
        // register command with subcommands: claim, release, list, map, color, rename
        kord.createGlobalChatInputCommand(name, description) {
            subCommand("claim", "Claim a territory by its numeric id") {
                string("id", "Territory id (number)") { required = true }
                string("name", "Optional: give this territory a custom name") { required = false }
            }
            subCommand("release", "Release a territory you own") {
                string("id", "Territory id (number)") { required = true }
            }
            subCommand("rename", "Rename a territory you own") {
                string("id", "Territory id (number)") { required = true }
                string("name", "New territory name") { required = true }
            }
            subCommand("list", "List all territories and owners") { }
            subCommand("map", "Show current map image with colored territories") { }
            subCommand("color", "Set your personal territory color (hex #RRGGBB)") {
                string("hex", "Color in #RRGGBB format") { required = true }
            }
        }

        // ensure DB ready and ensure all configured territories exist with defaults
        Database.init()
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
    {
        // ensure the command runs in a guild
        val guildIdVal = event.interaction.data.guildId.value?.toString()
        if (guildIdVal == null)
        {
            event.interaction.respondPublic { content = "This command can only be used in a guild." }
            return
        }

        // ensure rows exist for this guild
        try
        {
            Database.initializeTerritories(TERRITORY_COORDS.keys, guildIdVal)
        } catch (e: Exception)
        {
            println("TerritoryFunction.execute: initializeTerritories for guild $guildIdVal failed: ${e.message}")
            e.printStackTrace()
        }

        val response = event.interaction.deferPublicResponse()
        try
        {
            val subCommand = event.interaction.command.data.options.value?.map { it.name }?.first() ?: "list"

            when (subCommand)
            {
                "claim" ->
                {
                    val idText = event.interaction.command.strings["id"]?.trim().orEmpty()
                    val nameOpt = event.interaction.command.strings["name"]?.trim()
                    val id = idText.toIntOrNull()
                    if (id == null)
                    {
                        response.respond { content = "Usage: /territory claim id:<number> [name:optional]" }
                        return
                    }

                    // only allow claiming territories that are present in the coordinate map
                    if (!TERRITORY_COORDS.containsKey(id))
                    {
                        response.respond { content = "Territory $id is not claimable. Only territories listed in the map coordinates may be claimed." }
                        return
                    }

                    val ownerId = event.interaction.user.id.toString()
                    val ownerColor = Database.getOwnerColor(ownerId, guildIdVal)
                    if (ownerColor == null)
                    {
                        response.respond { content = "You have not set a color yet. Use /territory color hex:#RRGGBB to set your color first." }
                        return
                    }
                    // Check ownership constraints in DB and claim
                    val ok = Database.claimTerritory(id, ownerId, guildIdVal, nameOpt)
                    if (!ok)
                    {
                        response.respond { content = "Failed to claim territory $id. It may already be owned by someone else." }
                        return
                    }

                    // after Database.claimTerritory succeeded, always render full map with all claims
                    try
                    {
                        // render full map with all claims for this guild and attach
                        val outPath = renderFullMapWithClaims(guildIdVal)
                        val f = File(outPath)
                        response.respond {
                            content = "Territory $id claimed and map updated."
                            addFile(f.toPath())
                        }
                    } catch (e: Exception)
                    {
                        println("TerritoryFunction.execute: no base map found or failed generating map after claim for id $id: ${e.message}")
                        e.printStackTrace()
                        response.respond { content = "Territory claimed, but something went wrong with map generation." }
                    }
                }

                "release" ->
                {
                    val idText = event.interaction.command.strings["id"]?.trim().orEmpty()
                    val id = idText.toIntOrNull()
                    if (id == null)
                    {
                        response.respond { content = "Usage: /territory release id:<number>" }
                        return
                    }

                    // only allow releasing territories that are present in the coordinate map
                    if (!TERRITORY_COORDS.containsKey(id))
                    {
                        response.respond { content = "Territory $id is not managed by the map and cannot be released via this command." }
                        return
                    }

                    val ownerId = event.interaction.user.id.toString()
                    val ok = Database.releaseTerritory(id, ownerId, guildIdVal)
                    if (!ok)
                    {
                        response.respond { content = "Failed to release territory $id. It may not be owned by you or may not exist." }
                        return
                    }

                    // after successful release, render full map with all claims
                    try
                    {
                        val outPath = renderFullMapWithClaims(guildIdVal)
                        val f = File(outPath)
                        response.respond {
                            content = "Territory $id released and map updated."
                            addFile(f.toPath())
                        }
                    } catch (e: Exception)
                    {
                        println("TerritoryFunction.execute: no base map found or failed generating map after release for id $id: ${e.message}")
                        e.printStackTrace()
                        response.respond { content = "Territory released, but something went wrong with map generation." }
                    }
                }

                "rename" ->
                {
                    val idText = event.interaction.command.strings["id"]?.trim().orEmpty()
                    val newName = event.interaction.command.strings["name"]?.trim().orEmpty()
                    val id = idText.toIntOrNull()
                    if (id == null || newName.isBlank())
                    {
                        response.respond { content = "Usage: /territory rename id:<number> name:<new name>" }
                        return
                    }
                    if (!TERRITORY_COORDS.containsKey(id))
                    {
                        response.respond { content = "Territory $id is not managed by the map and cannot be renamed via this command." }
                        return
                    }
                    val ownerId = event.interaction.user.id.toString()
                    val ok = Database.renameTerritory(id, ownerId, guildIdVal, newName)
                    if (!ok)
                    {
                        response.respond { content = "Failed to rename territory $id. Ensure you own it and the new name is unique (case-insensitive)." }
                        return
                    }
                    response.respond {
                        content = "Territory $id renamed to '$newName'."
                    }
                }

                "list" ->
                {
                    val list = Database.listTerritories(guildIdVal)
                    if (list.isEmpty())
                    {
                        response.respond { content = "No territories registered yet." }
                        return
                    }
                    val sb = StringBuilder()
                    for (t in list)
                    {
                        val ownerDesc = if (t.owner == null) "unowned" else try
                        {
                            "${"Owner:".bold()} ${
                                getMemberName(
                                    kord,
                                    event.interaction.data.guildId.value,
                                    Snowflake(t.owner)
                                ).box()
                            } ${"Color:".bold()} ${t.color?.box() ?: "none".box()}"
                        } catch (e: Exception)
                        {
                            println("TerritoryFunction.list: getMemberName failed: ${e.message}")
                            e.printStackTrace()
                            "${"Owner:".bold()} ${t.owner.box()} ${"Color:".bold()} ${t.color?.box() ?: "none".box()}"
                        }
                        sb.append("ID ${t.id}: ${t.name} - $ownerDesc\n")
                    }
                    response.respond { content = sb.toString() }
                }

                "map" ->
                {
                    // produce current map image (base image with all owned territories recolored for this guild)
                    try
                    {
                        val outPath = renderFullMapWithClaims(guildIdVal)
                        val f = File(outPath)
                        response.respond {
                            content = "Map generated."
                            addFile(f.toPath())
                        }
                    } catch (e: Exception)
                    {
                        println("TerritoryFunction.execute.map failed: ${e.message}")
                        e.printStackTrace()
                        response.respond { content = "Failed to generate map: ${e.message}" }
                    }
                }

                "color" ->
                {
                    val hex = event.interaction.command.strings["hex"]?.trim().orEmpty()
                    if (hex.isBlank())
                    {
                        response.respond { content = "Usage: /territory color hex:#RRGGBB" }
                        return
                    }
                    val ownerId = event.interaction.user.id.toString()
                    val ok = Database.setOwnerColor(ownerId, guildIdVal, hex)
                    if (ok) response.respond { content = "Your color was set to ${hex.trim()}." }
                    else response.respond { content = "Failed to set color. Ensure format is #RRGGBB and color is not black (#000000)." }
                }

                else ->
                {
                    response.respond { content = "Unknown subcommand. Use claim/release/rename/list/map/color." }
                }
            }
        } catch (e: Exception)
        {
            println("TerritoryFunction.execute: unexpected error: ${e.message}")
            e.printStackTrace()
            response.respond { content = "An internal error occurred while handling the territory command." }
        }
    }

    /**
     * Produces a full map with all claimed territories colored for the given guild
     * @throws IllegalArgumentException - If the base map resource is not found or cannot be read.
     */
    private fun renderFullMapWithClaims(guildId: String): String
    {
        val stream = javaClass.getResourceAsStream("/SmallHavenMap.png")
            ?: throw IllegalArgumentException("Resource /SmallHavenMap.png not found.")
        val img = ImageIO.read(stream) ?: throw IllegalArgumentException("Could not read image resource.")

        val territories = Database.listTerritories(guildId)
        for (t in territories)
        {
            val coord = TERRITORY_COORDS[t.id]
            val color = t.color
            if (coord != null && color != null && t.owner != null)
            {
                floodFillBounded(img, coord.first, coord.second, parseHexColor(color))
            }
        }

        val outDir = File("db/maps")
        if (!outDir.exists()) outDir.mkdirs()
        val out = File(outDir, "SmallHavenMap_all_claims_${guildId}.png")
        ImageIO.write(img, "PNG", out)
        return out.absolutePath
    }

    // parse #RRGGBB -> ARGB int
    private fun parseHexColor(hex: String): Int
    {
        val h = hex.trim().let { if (it.startsWith("#")) it.substring(1) else it }
        val rgb = Integer.parseInt(h, 16) and 0xFFFFFF
        return 0xFF000000.toInt() or rgb
    }

    // flood fill bounded by black border pixels (treated as barrier). Replaces pixels that match the starting pixel's color only.
    private fun floodFillBounded(img: BufferedImage, sx: Int, sy: Int, replacementArgb: Int)
    {
        val w = img.width
        val h = img.height
        if (sx !in 0 until w || sy !in 0 until h) throw IllegalArgumentException("Start coordinate out of bounds: $sx,$sy for image $w x $h")

        val borderArgb = Color.BLACK.rgb // treat exact black pixels as barrier
        val targetColor = img.getRGB(sx, sy) // only replace pixels equal to this color
        if (targetColor == replacementArgb) return

        val visited = Array(w) { BooleanArray(h) }
        val dq = ArrayDeque<Pair<Int, Int>>()
        dq.addLast(sx to sy)

        while (dq.isNotEmpty())
        {
            val (x, y) = dq.removeFirst()
            if (x < 0 || x >= w || y < 0 || y >= h) continue
            if (visited[x][y]) continue
            visited[x][y] = true
            val col = img.getRGB(x, y)
            if (col == borderArgb) continue // never paint border
            // Only paint if the pixel exactly matches the starting pixel's color
            if (col != targetColor) continue
            // paint pixel
            img.setRGB(x, y, replacementArgb)
            // push neighbors
            dq.addLast(x + 1 to y)
            dq.addLast(x - 1 to y)
            dq.addLast(x to y + 1)
            dq.addLast(x to y - 1)
        }
    }
}