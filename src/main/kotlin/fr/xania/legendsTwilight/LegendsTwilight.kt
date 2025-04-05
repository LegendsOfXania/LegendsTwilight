package fr.xania.legendsTwilight

import org.bukkit.plugin.java.JavaPlugin

class LegendsTwilight : JavaPlugin() {

    override fun onEnable() {

        enableLogger()

        saveDefaultConfig()
    }

    override fun onDisable() {
    }

    private fun enableLogger() {
        val pluginlogger = ("""
            |    
            |                    _____
            |              |       |      LegendsTwilight v${pluginMeta.version}
            |              |___    |      Running on ${server.version}
            |  
        """.trimMargin())
        logger.info { pluginlogger }
    }


}
