package commandScheduler.plugincommandrunner.configs

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import commandScheduler.plugincommandrunner.Plugincommandrunner
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException


object ConfigManager {
    private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
    private val CONFIG_FILE: File = File("config/PluginCommandRunner/config.json")

    var config: ChannelConfig = ChannelConfig()

    fun loadConfig() {
        if (CONFIG_FILE.exists()) {
            try {
                FileReader(CONFIG_FILE).use { reader ->
                    config = GSON.fromJson(reader, ChannelConfig::class.java)
                }
            } catch (e: IOException) {
                throw IOException("Failed to load config file: ${e.message}", e)
            }
        } else {
            saveConfig()
        }
    }

    fun saveConfig() {
        try {
            CONFIG_FILE.getParentFile().mkdirs()
            FileWriter(CONFIG_FILE).use { writer ->
                GSON.toJson(config, writer)
            }
            Plugincommandrunner.LOGGER.info("Configs created for PluginCommandRunner.")
        } catch (e: IOException) {
            Plugincommandrunner.LOGGER.info("Could not create configs for pluginCommandRunner.")
            Plugincommandrunner.LOGGER.info(e.toString())
        }
    }


}