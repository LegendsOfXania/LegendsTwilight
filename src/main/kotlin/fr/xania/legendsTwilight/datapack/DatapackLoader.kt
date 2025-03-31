package fr.xania.legendsTwilight.datapack

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class DatapackLoader : JavaPlugin() {

    fun datapackLoader() {

        val worldsConfig = config.getStringList("datapack.worlds")

        if (worldsConfig.isEmpty()) {
            logger.warning("No world found in the config file.")
            return
        }

        val datapackName = config.getString("datapack.name")
        val datapackFolder = File(dataFolder, config.getString("datapack.path" + ".zip") ?: "pack/legends_twilight.zip")

        if (!datapackFolder.exists()) {

            DatapackCreate().datapackCreate()
            logger.warning("Datapack file doesn't exist in ${datapackFolder.absolutePath}. Contact the developer.")
        }

        Bukkit.getWorlds().forEach { world ->

            if (worldsConfig.contains(world.name)) {
                val worldDatapackFolder = File(world.worldFolder, "datapacks")

                val datapackExists = worldDatapackFolder.listFiles()?.any { it.name.equals(datapackName, ignoreCase = true) } == true

                if (!datapackExists) {
                    val target = File(worldDatapackFolder, datapackName ?: "legends_twilight")
                    if (!target.exists()) {
                        datapackFolder.inputStream().use { inputStream ->
                            val zip = java.util.zip.ZipInputStream(inputStream)
                            var entry = zip.nextEntry
                            while (entry != null) {
                                val file = File(target, entry.name)
                                if (entry.isDirectory) {
                                    file.mkdirs()
                                } else {
                                    file.outputStream().use { out ->
                                        zip.copyTo(out)
                                    }
                                }
                                zip.closeEntry()
                                entry = zip.nextEntry
                            }
                        }
                    }
                    logger.info("Datapack was added in world : ${world.name}")
                }
            }
        }
    }
}
