package commandScheduler.plugincommandrunner

import com.google.common.io.ByteArrayDataInput
import commandScheduler.plugincommandrunner.configs.ConfigManager.loadConfig
import net.fabricmc.api.ModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.google.common.io.ByteStreams
import com.pokeskies.fabricpluginmessaging.PluginMessageEvent
import commandScheduler.plugincommandrunner.configs.ConfigManager
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.MinecraftServer

class Plugincommandrunner : ModInitializer {

    companion object {
        const val MOD_ID: String = "PluginCommandRunner"
        val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)
    }
    val MAIN_MESSAGING_CHANNEL = "PluginCommandRunner"
    val SECONDARY_MESSAGING_CHANNEL: String = ConfigManager.config.server



    override fun onInitialize() {

        try {
            loadConfig()
            registerCommandRunner()

            LOGGER.info("PluginCommandRunner loaded correctly.")
            LOGGER.info("Listening for plugin messages and commands to run on: ${ConfigManager.config.server}")
        } catch (e: Exception) {
            LOGGER.warn(e.toString())
        }
    }

    fun registerCommandRunner(){


        PluginMessageEvent.EVENT.register { payload, context ->
            LOGGER.info("Received something.");
            if (!payload.data.isEmpty()){
                val inputStream = ByteStreams.newDataInput(payload.data)

                checkChannelMetaData(inputStream,context)
            }

        }
    }

    fun checkChannelMetaData(inputStream: ByteArrayDataInput, context: ServerPlayNetworking.Context){

        val channel = inputStream.readUTF()
        LOGGER.info("current channel: $channel")
        if (channel == MAIN_MESSAGING_CHANNEL) {
            val currentServer = inputStream.readUTF()
            LOGGER.info("current server: $currentServer")
            if (currentServer == SECONDARY_MESSAGING_CHANNEL){
                val server = context.server()
                if (server != null){

                    val command = inputStream.readUTF()
                    LOGGER.info("command to run: $command")
                    executeCommand(server,command)

                }
                else{
                    LOGGER.warn("Had problems getting the server instance.")
                }



            }
        }
    }


    fun executeCommand(server: MinecraftServer,command: String){
        server.commandManager.executeWithPrefix(server.commandSource, command)
    }

}
