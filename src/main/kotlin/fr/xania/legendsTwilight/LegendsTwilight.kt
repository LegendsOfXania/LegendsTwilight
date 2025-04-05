package fr.xania.legendsTwilight

import CorruptionManager
import CorruptionStorage
import fr.xania.legendsTwilight.utils.ConfigManager
import fr.xania.legendsTwilight.utils.Config
import org.bukkit.plugin.java.JavaPlugin

class LegendsTwilight : JavaPlugin() {

    private lateinit var config: Config

    override fun onEnable() {
        enableLogger()

        config = ConfigManager.loadConfig(this)

        CorruptionManager.initialize(this)

        config.enabled_worlds.forEach { worldName ->
            CorruptionStorage.loadCorruptedChunks(worldName)
        }
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
            |
        """.trimMargin())
        logger.info { pluginlogger }
    }
}