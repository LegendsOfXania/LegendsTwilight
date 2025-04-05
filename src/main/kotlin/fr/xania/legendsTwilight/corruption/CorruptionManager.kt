import fr.xania.legendsTwilight.utils.ConfigManager
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

object CorruptionManager {

    private val corruptedChunks = mutableMapOf<String, MutableSet<ChunkCoord>>()
    private lateinit var plugin: JavaPlugin

    fun initialize(plugin: JavaPlugin) {
        this.plugin = plugin
    }

    fun startCorruptionPropagation() {
        val config = ConfigManager.loadConfig(plugin)
        val worlds = config.enabled_worlds
        val interval = config.propagation_interval * 60L
        val growthFactor = config.corruption_growth_factor

        // Planifie la propagation tous les X ticks
        object : BukkitRunnable() {
            override fun run() {
                worlds.forEach { worldName ->
                    val world = Bukkit.getWorld(worldName) ?: return@forEach
                    propagateCorruption(world, growthFactor)
                }
            }
        }.runTaskTimer(plugin, 0L, interval * 20L)  // 1 tick = 1/20 secondes
    }

    private fun propagateCorruption(world: World, growthFactor: Double) {
        val corruptedChunksInWorld = corruptedChunks[world.name] ?: return

        plugin.logger.info("Début de la propagation de la corruption dans le monde ${world.name}.")

        corruptedChunksInWorld.forEach { chunkCoord ->
            val chunk = world.getChunkAt(chunkCoord.x, chunkCoord.z)
            propagateToNeighbors(world, chunk, corruptedChunksInWorld, growthFactor)
        }
    }

    private fun propagateToNeighbors(world: World, chunk: Chunk, corruptedChunksInWorld: MutableSet<ChunkCoord>, growthFactor: Double) {
        val neighbors = listOf(
            ChunkCoord(chunk.x - 1, chunk.z - 1), ChunkCoord(chunk.x, chunk.z - 1), ChunkCoord(chunk.x + 1, chunk.z - 1),
            ChunkCoord(chunk.x - 1, chunk.z), ChunkCoord(chunk.x + 1, chunk.z),
            ChunkCoord(chunk.x - 1, chunk.z + 1), ChunkCoord(chunk.x, chunk.z + 1), ChunkCoord(chunk.x + 1, chunk.z + 1)
        )

        val adjustedNeighbors = neighbors.take((neighbors.size * growthFactor).toInt())

        plugin.logger.info("Propagation dans le chunk (${chunk.x}, ${chunk.z}) du monde ${world}.")

        adjustedNeighbors.forEach { neighbor ->
            if (!corruptedChunksInWorld.contains(neighbor)) {
                corruptedChunksInWorld.add(neighbor)
                CorruptionStorage.addCorruptedChunk(world.name, neighbor)
                plugin.logger.info("Nouveau chunk corrompu ajouté: ${neighbor.x}, ${neighbor.z} dans ${world.name}")
            }
        }
    }

    fun addCorruptedChunk(world: World, chunkCoord: ChunkCoord) {
        val worldName = world.name
        val corruptedChunksInWorld = corruptedChunks.computeIfAbsent(worldName) { mutableSetOf() }
        if (corruptedChunksInWorld.add(chunkCoord)) {
            CorruptionStorage.addCorruptedChunk(worldName, chunkCoord)
        }
    }

    fun getCorruptedChunks(worldName: String): Set<ChunkCoord> {
        return corruptedChunks[worldName] ?: emptySet()
    }
}