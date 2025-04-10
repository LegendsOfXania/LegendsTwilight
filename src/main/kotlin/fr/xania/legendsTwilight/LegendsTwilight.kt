package fr.xania.legendsTwilight

import CorruptionManager
import CorruptionStorage
import com.github.retrooper.packetevents.PacketEvents
import fr.xania.legendsTwilight.utils.Config
import fr.xania.legendsTwilight.utils.ConfigManager
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.ServerLoadEvent
import org.bukkit.plugin.java.JavaPlugin

class LegendsTwilight : JavaPlugin(), Listener {

    private lateinit var config: Config

    override fun onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
        PacketEvents.getAPI().load()
    }


    override fun onEnable() {
        config = ConfigManager.loadConfig(this)


        CorruptionManager.initialize(this)
        server.pluginManager.registerEvents(this, this)

        CorruptionManager.startCorruptionPropagation()

        logger.info("LegendsTwilight enabled successfully.")
    }

    @EventHandler
    fun onServerLoad(event: ServerLoadEvent) {
        if (event.type == ServerLoadEvent.LoadType.STARTUP) {
            logger.info("Server is loaded, checking for corrupted chunks...")

            var worldsWithCorruption = 0
            var totalCorruptedChunks = 0

            config.enabled_worlds.forEach { worldName ->
                val corruptedChunks = CorruptionStorage.loadCorruptedChunks(worldName)

                if (corruptedChunks.isNotEmpty()) {
                    corruptedChunks.forEach { chunk ->
                        val world = server.getWorld(worldName)
                        if (world != null) {
                            CorruptionManager.addCorruptedChunk(world, chunk)
                        }
                    }

                    worldsWithCorruption++
                    totalCorruptedChunks += corruptedChunks.size
                    logger.info("${corruptedChunks.size} corrupted chunks found and loaded in : $worldName")
                }
            }

            if (worldsWithCorruption == 0) {
                logger.info("No corrupted chunks found in any world. Initializing corruption in configured worlds...")
                CorruptionManager.initializeCorruptionInConfiguredWorlds()
            } else {
                logger.info("$totalCorruptedChunks chunks are loaded in : $worldsWithCorruption.")
            }
        }
    }

    override fun onDisable() {
        logger.info("Sauvegarde de l'état de la corruption...")
        try {
            CorruptionManager.saveAllCorruptedChunksSync()
            logger.info("État de la corruption sauvegardé avec succès.")
        } catch (e: Exception) {
            logger.severe("Erreur lors de la sauvegarde de l'état de corruption: ${e.message}")
        }

        PacketEvents.getAPI().terminate()

        logger.info("LegendsTwilight désactivé avec succès.")
    }
}