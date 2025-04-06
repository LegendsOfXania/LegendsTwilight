package fr.xania.legendsTwilight

import CorruptionManager
import CorruptionStorage
import fr.xania.legendsTwilight.utils.ConfigManager
import fr.xania.legendsTwilight.utils.Config
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.event.Listener
import org.bukkit.event.EventHandler
import org.bukkit.event.server.ServerLoadEvent

class LegendsTwilight : JavaPlugin(), Listener {

    private lateinit var config: Config

    override fun onEnable() {
        config = ConfigManager.loadConfig(this)

        enableLogger()

        CorruptionManager.initialize(this)

        server.pluginManager.registerEvents(this, this)

        CorruptionManager.startCorruptionPropagation()

        logger.info("LegendsTwilight activé avec succès !")
    }

    @EventHandler
    fun onServerLoad(event: ServerLoadEvent) {
        if (event.type == ServerLoadEvent.LoadType.STARTUP) {
            logger.info("Serveur complètement chargé, vérification de la corruption...")

            var worldsWithCorruption = 0
            var totalCorruptedChunks = 0

            config.enabled_worlds.forEach { worldName ->
                val corruptedChunks = CorruptionStorage.loadCorruptedChunks(worldName)

                if (corruptedChunks.isNotEmpty()) {
                    corruptedChunks.forEach { chunk ->
                        CorruptionManager.getCorruptedChunks(worldName)
                        val world = server.getWorld(worldName)
                        if (world != null) {
                            CorruptionManager.addCorruptedChunk(world, chunk)
                        }
                    }

                    worldsWithCorruption++
                    totalCorruptedChunks += corruptedChunks.size
                    logger.info("${corruptedChunks.size} chunks corrompus trouvés et chargés pour $worldName")
                }
            }

            if (worldsWithCorruption == 0) {
                logger.info("Aucun chunk corrompu trouvé, initialisation de la corruption...")
                CorruptionManager.initializeCorruptionInConfiguredWorlds()
            } else {
                logger.info("Corruption chargée: $totalCorruptedChunks chunks dans $worldsWithCorruption monde(s)")
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
        logger.info("LegendsTwilight désactivé avec succès.")
    }

    private fun enableLogger() {
        val pluginlogger = ("""
            |
            |                    _____
            |              |       |      Enabling LegendsTwilight
            |              |___    |      Created by Legends of Xania
            |
        """.trimMargin())
        logger.info(pluginlogger)
    }
}