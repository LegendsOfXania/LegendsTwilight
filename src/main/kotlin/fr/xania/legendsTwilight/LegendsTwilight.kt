package fr.xania.legendsTwilight

import CorruptionManager
import CorruptionStorage
import fr.xania.legendsTwilight.utils.ConfigManager
import org.bukkit.plugin.java.JavaPlugin

class LegendsTwilight : JavaPlugin() {

    private val config = ConfigManager.loadConfig()

    override fun onEnable() {
        enableLogger()

        config.enabled_worlds.forEach { worldName ->
            CorruptionStorage.loadCorruptedChunks(worldName)
        }

        // Démarrer la propagation de la corruption
        CorruptionManager.startCorruptionPropagation()
    }

    override fun onDisable() {
        config.enabled_worlds.forEach { worldName ->
            val corruptedChunks = CorruptionStorage.loadCorruptedChunks(worldName)
            CorruptionStorage.saveCorruptedChunks(worldName, corruptedChunks)
        }
    }

    private fun enableLogger() {
        val pluginlogger = ("""
            |
            |                    _____
            |              |       |      Enabling LegendsTwilight
            |              |___    |      Created by Legends of Xania
            |
        """.trimMargin())
        logger.info { pluginlogger }
    }
}
