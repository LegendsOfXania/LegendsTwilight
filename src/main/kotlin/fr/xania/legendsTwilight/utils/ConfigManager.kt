package fr.xania.legendsTwilight.utils

import com.google.gson.Gson
import org.bukkit.Bukkit
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

data class Config(val enabled_worlds: List<String>,
                  val propagation_interval: Int,
                  val corruption_growth_factor: Double)

object ConfigManager {

    private val gson = Gson()

    private fun getConfigFile(): File {
        return File(Bukkit.getPluginManager().getPlugin("LegendsTwilight")!!.dataFolder, "config.json")
    }

    fun loadConfig(): Config {
        val file = getConfigFile()

        if (!file.exists()) {
            val defaultConfig = Config(
                enabled_worlds = listOf("world", "world_the_end"),
                propagation_interval = 60,
                corruption_growth_factor = 1.5)
            saveConfig(defaultConfig)
            return defaultConfig
        }

        val json = String(Files.readAllBytes(Paths.get(file.toURI())))
        return gson.fromJson(json, Config::class.java)
    }

    fun saveConfig(config: Config) {
        val json = gson.toJson(config)
        Files.write(getConfigFile().toPath(), json.toByteArray())
    }
}