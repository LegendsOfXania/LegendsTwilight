package fr.xania.legendsTwilight

import org.bukkit.plugin.java.JavaPlugin

class LegendsTwilight : JavaPlugin() {

    override fun onEnable() {

        enableLogger()

    }

    override fun onDisable() {
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
