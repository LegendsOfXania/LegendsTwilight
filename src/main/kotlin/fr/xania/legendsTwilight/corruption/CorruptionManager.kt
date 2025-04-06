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
                plugin.logger.warning("Monde configuré $worldName introuvable, impossible d'initialiser la corruption")
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
                    // Opération d'écriture en stockage en asynchrone
                    plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
                        CorruptionStorage.addCorruptedChunk(worldName, originChunk)
                        plugin.logger.info("Chunk d'origine (0, 0) ajouté comme corrompu dans le monde $worldName")
                    })
                }
            }
        })
    }

    fun startCorruptionPropagation() {
        val config = ConfigManager.loadConfig(plugin)
        val interval = config.propagation_interval * 60L * 20L // Conversion en ticks
        val growthFactor = config.corruption_growth_factor

        plugin.logger.info("Démarrage de la propagation avec un intervalle de ${config.propagation_interval} minute(s) et un facteur de croissance de $growthFactor")

        object : BukkitRunnable() {
            override fun run() {
                val worlds = config.enabled_worlds.mapNotNull { Bukkit.getWorld(it) }

                plugin.logger.info("Exécution de la propagation de corruption sur ${worlds.size} monde(s)...")

                for (world in worlds) {
                    val chunks = corruptedChunks[world.name]?.toList() ?: emptyList()
                    plugin.logger.info("Monde ${world.name}: ${chunks.size} chunks corrompus avant propagation")

                    // Exécutez la propagation directement sur le thread principal pour simplifier
                    if (chunks.isNotEmpty()) {
                        propagateCorruption(world, chunks, growthFactor)
                    } else {
                        plugin.logger.warning("Aucun chunk corrompu trouvé dans ${world.name}, impossible de propager")
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, interval)
    }

    private fun propagateCorruption(world: World, corruptedChunksList: List<ChunkCoord>, growthFactor: Double) {
        plugin.logger.info("Propagation de la corruption dans ${world.name} avec ${corruptedChunksList.size} chunks corrompus")

        val newCorruptedChunks = mutableSetOf<ChunkCoord>()
        val random = java.util.Random()

        corruptedChunksList.forEach { chunkCoord ->
            val neighbors = getNeighborChunks(chunkCoord)
            plugin.logger.info("Chunk (${chunkCoord.x}, ${chunkCoord.z}) a ${neighbors.size} voisins potentiels")

            neighbors.forEach { neighbor ->
                val shouldPropagate = if (growthFactor >= 1.0) true else random.nextDouble() < growthFactor

                if (shouldPropagate) {
                    val worldChunks = corruptedChunks.computeIfAbsent(world.name) { ConcurrentHashMap.newKeySet() }
                    if (worldChunks.add(neighbor)) {
                        newCorruptedChunks.add(neighbor)
                        plugin.logger.info("Nouveau chunk corrompu: (${neighbor.x}, ${neighbor.z})")
                    }
                }
            }
        }

        if (newCorruptedChunks.isNotEmpty()) {
            plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
                for (chunk in newCorruptedChunks) {
                    CorruptionStorage.addCorruptedChunk(world.name, chunk)
                }
                plugin.logger.info("PROPAGATION TERMINÉE: ${newCorruptedChunks.size} nouveaux chunks corrompus dans ${world.name}")
            })
        } else {
            plugin.logger.warning("Aucun nouveau chunk corrompu n'a été ajouté durant cette propagation")
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

    fun saveAllCorruptedChunksAsync() {
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            saveAllCorruptedChunksSync()
        })
    }

    fun saveAllCorruptedChunksSync() {
        try {
            for ((worldName, chunks) in corruptedChunks) {
                for (chunk in chunks) {
                    CorruptionStorage.addCorruptedChunk(worldName, chunk)
                }
            }
            plugin.logger.info("Sauvegarde de tous les chunks corrompus terminée")
        } catch (e: Exception) {
            plugin.logger.severe("Erreur lors de la sauvegarde des chunks corrompus: ${e.message}")
            e.printStackTrace()
        }
    }
}