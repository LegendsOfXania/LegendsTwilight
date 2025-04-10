import fr.xania.legendsTwilight.utils.ConfigManager
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.util.concurrent.ConcurrentHashMap

object CorruptionManager {

    private val corruptedChunks = ConcurrentHashMap<String, MutableSet<ChunkCoord>>()
    private lateinit var plugin: JavaPlugin

    fun initialize(plugin: JavaPlugin) {
        this.plugin = plugin
    }

    fun initializeCorruptionInConfiguredWorlds() {
        val config = ConfigManager.loadConfig(plugin)

        val validWorlds = config.enabled_worlds.mapNotNull { worldName ->
            val world = Bukkit.getWorld(worldName)
            if (world == null) {
                plugin.logger.warning(" $worldName not found. Skipping...")
                null
            } else {
                worldName to world
            }
        }

        plugin.server.scheduler.runTask(plugin, Runnable {
            for ((worldName, world) in validWorlds) {
                val originChunk = ChunkCoord(0, 0)
                val worldChunks = corruptedChunks.computeIfAbsent(worldName) { ConcurrentHashMap.newKeySet() }

                if (worldChunks.add(originChunk)) {
                    plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
                        CorruptionStorage.addCorruptedChunk(worldName, originChunk)
                        plugin.logger.info("Original chunk (x:0, z:0) is corrupted in $worldName")
                    })
                }
            }
        })
    }

    fun startCorruptionPropagation() {
        val config = ConfigManager.loadConfig(plugin)
        val interval = config.propagation_interval * 60L * 20L // Conversion en ticks
        val growthFactor = config.corruption_growth_factor

        plugin.logger.info("Start propagation with an interval of ${config.propagation_interval} minute(s) and a growth factor of $growthFactor")

        object : BukkitRunnable() {
            override fun run() {
                val worlds = config.enabled_worlds.mapNotNull { Bukkit.getWorld(it) }

                plugin.logger.info("Run corruption propagation on ${worlds.size} world(s)...")

                for (world in worlds) {
                    val chunks = corruptedChunks[world.name]?.toList() ?: emptyList()
                    plugin.logger.info("World ${world.name}: ${chunks.size} chunks corrupted before propagation")

                    if (chunks.isNotEmpty()) {
                        propagateCorruption(world, chunks, growthFactor)
                    } else {
                        plugin.logger.warning("No corrupted chunk found in ${world.name}, impossible to propagate")
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, interval)
    }

    private fun propagateCorruption(world: World, corruptedChunksList: List<ChunkCoord>, growthFactor: Double) {
        plugin.logger.info("Propagation of corruption in ${world.name} with ${corruptedChunksList.size} corrupted chunksNo corrupted chunk found in ${world.name}, impossible to propagate")

        val newCorruptedChunks = mutableSetOf<ChunkCoord>()
        val random = java.util.Random()

        corruptedChunksList.forEach { chunkCoord ->
            val neighbors = getNeighborChunks(chunkCoord)
            plugin.logger.info("Chunk (${chunkCoord.x}, ${chunkCoord.z}) has ${neighbors.size} potential neighbors")

            neighbors.forEach { neighbor ->
                val shouldPropagate = if (growthFactor >= 1.0) true else random.nextDouble() < growthFactor

                if (shouldPropagate) {
                    val worldChunks = corruptedChunks.computeIfAbsent(world.name) { ConcurrentHashMap.newKeySet() }
                    if (worldChunks.add(neighbor)) {
                        newCorruptedChunks.add(neighbor)
                        plugin.logger.info("New corrupted chunk : (${neighbor.x}, ${neighbor.z})")
                    }
                }
            }
        }

        if (newCorruptedChunks.isNotEmpty()) {
            plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
                for (chunk in newCorruptedChunks) {
                    CorruptionStorage.addCorruptedChunk(world.name, chunk)
                }
                plugin.logger.info("PROPAGATION COMPLETE: ${newCorruptedChunks.size} new corrupted chunks in ${world.name}")
            })
        } else {
                plugin.logger.warning("No new corrupted chunks were added during this propagation")
        }
    }

    private val neighborDirections = listOf(
        -1 to -1, 0 to -1, 1 to -1,
        -1 to 0, 1 to 0,
        -1 to 1, 0 to 1, 1 to 1
    )

    private fun getNeighborChunks(chunkCoord: ChunkCoord): List<ChunkCoord> {
        return neighborDirections.map { (dx, dz) ->
            ChunkCoord(chunkCoord.x + dx, chunkCoord.z + dz)
        }
    }

    fun addCorruptedChunk(world: World, chunkCoord: ChunkCoord) {
        val worldName = world.name
        val corruptedChunksInWorld = corruptedChunks.computeIfAbsent(worldName) { ConcurrentHashMap.newKeySet() }
        if (corruptedChunksInWorld.add(chunkCoord)) {
            plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
                CorruptionStorage.addCorruptedChunk(worldName, chunkCoord)
            })
        }
    }

    fun getCorruptedChunks(worldName: String): Set<ChunkCoord> {
        return corruptedChunks[worldName]?.toSet() ?: emptySet()
    }

    fun saveAllCorruptedChunksSync() {
        try {
            for ((worldName, chunks) in corruptedChunks) {
                for (chunk in chunks) {
                    CorruptionStorage.addCorruptedChunk(worldName, chunk)
                }
            }
            plugin.logger.info("Saving all corrupted chunks...")
        } catch (e: Exception) {
            plugin.logger.severe("Error saving corrupted chunks: ${e.message}")
            e.printStackTrace()
        }
    }
}