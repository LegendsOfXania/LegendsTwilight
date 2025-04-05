package fr.xania.legendsTwilight.corruption

import ChunkCoord
import CorruptionStorage
import fr.xania.legendsTwilight.utils.ConfigManager
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.scheduler.BukkitRunnable

object CorruptionManager {
    private val corruptedChunks = mutableMapOf<String, MutableSet<ChunkCoord>>()

    fun startCorruptionPropagation() {
        val config = ConfigManager.loadConfig()
        val worlds = config.enabled_worlds
        val interval = config.propagation_interval * 60L

        object : BukkitRunnable() {
            override fun run() {
                worlds.forEach { worldName ->
                    val world = Bukkit.getWorld(worldName) ?: return@forEach
                    propagateCorruption(world)
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LegendsTwilight")!!, 0L, interval * 20L)
    }

    private fun propagateCorruption(world: World) {
        val corrupedChunksInWorld = corruptedChunks[world.name] ?: return

        corrupedChunksInWorld.forEach { chunkCoord ->
            val chunk = world.getChunkAt(chunkCoord.x, chunkCoord.z)
            propagateToNeightbors(world, chunk, corrupedChunksInWorld)
        }
    }

    private fun propagateToNeightbors(world: World, chunk: org.bukkit.Chunk, corruptedChunksInWorld: MutableSet<ChunkCoord>) {
        val neighbors = listOf(
            ChunkCoord(chunk.x - 1, chunk.z - 1), ChunkCoord(chunk.x, chunk.z - 1), ChunkCoord(chunk.x + 1, chunk.z - 1),
            ChunkCoord(chunk.x - 1, chunk.z), ChunkCoord(chunk.x + 1, chunk.z),
            ChunkCoord(chunk.x - 1, chunk.z + 1), ChunkCoord(chunk.x, chunk.z + 1), ChunkCoord(chunk.x + 1, chunk.z + 1)
        )

        neighbors.forEach { neighbor ->
            if (!corruptedChunksInWorld.contains(neighbor)) {
                corruptedChunksInWorld.add(neighbor)
                CorruptionStorage.addCorruptedChunk(world.name, neighbor)
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

    fun removeCorruptedChunk(world: World, chunkCoord: ChunkCoord) {
        val worldName = world.name
        val corruptedChunksInWorld = corruptedChunks[worldName] ?: return
        if (corruptedChunksInWorld.remove(chunkCoord)) {
            CorruptionStorage.removeCorruptedChunk(worldName, chunkCoord)
        }
    }

}