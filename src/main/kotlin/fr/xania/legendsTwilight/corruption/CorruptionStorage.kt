import com.google.gson.Gson
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import org.bukkit.Bukkit

data class ChunkCoord(val x: Int, val z: Int)
data class CorruptionData(val version: Int = 1, val corruptedChunks: List<ChunkCoord>)

object CorruptionStorage {

    private val gson = Gson()

    private fun getFile(world: String): File {
        val worldFolder = File(Bukkit.getWorldContainer(), world)
        return File(worldFolder, "legends_twilight.json")
    }

    fun loadCorruptedChunks(world: String): List<ChunkCoord> {
        val file = getFile(world)

        if (!file.exists()) {
            saveCorruptedChunks(world, emptyList())
            return emptyList()
        }

        val json = String(Files.readAllBytes(Paths.get(file.toURI())))
        val corruptionData: CorruptionData = gson.fromJson(json, CorruptionData::class.java)
        return corruptionData.corruptedChunks
    }

    private fun saveCorruptedChunks(world: String, corruptedChunks: List<ChunkCoord>) {
        val file = getFile(world)
        val corruptionData = CorruptionData(corruptedChunks = corruptedChunks)
        val json = gson.toJson(corruptionData)
        Files.write(file.toPath(), json.toByteArray())
    }

    fun addCorruptedChunk(world: String, chunkCoord: ChunkCoord) {
        val corruptedChunks = loadCorruptedChunks(world).toMutableList()
        if (!corruptedChunks.contains(chunkCoord)) {
            corruptedChunks.add(chunkCoord)
            saveCorruptedChunks(world, corruptedChunks)
        }
    }

    fun removeCorruptedChunk(world: String, chunkCoord: ChunkCoord) {
        val corruptedChunks = loadCorruptedChunks(world).toMutableList()
        corruptedChunks.remove(chunkCoord)
        saveCorruptedChunks(world, corruptedChunks)
    }
}
