package fr.xania.legendsTwilight.datapack

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import org.bukkit.plugin.java.JavaPlugin

class DatapackCreate : JavaPlugin() {

    fun datapackCreate() {

        val datapackName = config.getString("datapack.name")
        val datapackFolder = File(dataFolder, config.getString("datapack.path" + ".zip") ?: "pack/legends_twilight.zip")


        if (!datapackFolder.exists()) {
            logger.info("Le datapack par défaut n'existe pas. Copie en cours...")

            copyResource(datapackName  ?: "legends_twilight", datapackFolder)

            logger.info("Datapack par défaut copié avec succès dans le dossier 'pack'.")
        } else {
            logger.info("Le datapack par défaut existe déjà.")
        }
    }

    private fun copyResource(resourcePath: String, targetFile: File) {

        val resourceStream: InputStream = this.javaClass.classLoader.getResourceAsStream(resourcePath)
            ?: throw IllegalStateException("Impossible de trouver la ressource: $resourcePath")

        targetFile.parentFile.mkdirs()
        val outputStream: OutputStream = targetFile.outputStream()

        resourceStream.copyTo(outputStream)

        resourceStream.close()
        outputStream.close()
    }
}
