package fr.xania.legendsTwilight.utils

import com.google.gson.GsonBuilder
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

data class Config(
    val enabled_worlds: List<String>,
    val propagation_interval: Int,
    val corruption_growth_factor: Double
)

object ConfigManager {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    private fun getConfigFile(plugin: JavaPlugin): File {
        val dataFolder = plugin.dataFolder
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        return File(dataFolder, "config.json")
    }

    fun loadConfig(plugin: JavaPlugin): Config {
        val file = getConfigFile(plugin)

        if (!file.exists()) {
            val defaultConfig = Config(
                enabled_worlds = listOf("world", "world_nether", "world_the_end"),
                propagation_interval = 60,
                corruption_growth_factor = 1.5
            )
            saveConfig(plugin, defaultConfig)
            return defaultConfig
        }

        val json = String(Files.readAllBytes(Paths.get(file.toURI())))
        return gson.fromJson(json, Config::class.java)
    }

    private fun saveConfig(plugin: JavaPlugin, config: Config) {
        val json = gson.toJson(config)
        val file = getConfigFile(plugin)
        Files.write(file.toPath(), json.toByteArray())
    }
}