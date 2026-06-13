package commandScheduler.plugincommandrunner

import com.google.common.io.ByteArrayDataInput
import commandScheduler.plugincommandrunner.configs.ConfigManager.loadConfig
import net.fabricmc.api.ModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.google.common.io.ByteStreams
import com.pokeskies.fabricpluginmessaging.PluginMessageEvent
import commandScheduler.plugincommandrunner.configs.ConfigManager
import kotlinx.datetime.Clock
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.MinecraftServer
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs
import java.time.Clock as jtClock

class Plugincommandrunner : ModInitializer {

    companion object {
        const val MOD_ID: String = "PluginCommandRunner"
        val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)
    }
    val MAIN_MESSAGING_CHANNEL = "PluginCommandRunner"
    lateinit var SECONDARY_MESSAGING_CHANNEL: String
    var previous_id: Int = 0

    override fun onInitialize() {
        try {
            loadConfig()
            SECONDARY_MESSAGING_CHANNEL = ConfigManager.config.server

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
        val currentServer = inputStream.readUTF()
        val command = inputStream.readUTF()
        val id = inputStream.readInt()
        val timestamp = inputStream.readLong()
        val signature = inputStream.readUTF()

        if (channel != MAIN_MESSAGING_CHANNEL) {
            LOGGER.warn("Different Messaging Channel")
            return;
        }
        if (currentServer != SECONDARY_MESSAGING_CHANNEL) {
            LOGGER.warn("Different Server")
            return;
        }

        val now = jtClock.systemUTC().instant().epochSecond
        if (abs(now - timestamp) > 30) {
            LOGGER.warn("Possibly fake plugin message detected.")
            return
        }

        val mac = createMac()
        val data = "$channel:$currentServer:$id:$command"
        val raw = mac.doFinal(data.toByteArray(Charsets.UTF_8))

        val encoded: String = Base64.getEncoder().encodeToString(raw)
        if (encoded != signature){
            LOGGER.warn("Invalid signature detected.")
            return
        }

        if(id <= previous_id) return // Out of order ids, possibly duplicated message
        previous_id = id





        executeCommand(context.server(),command);
    }
    fun createMac(): Mac {
        val secretKey = SecretKeySpec(
            ConfigManager.config.secret.toByteArray(Charsets.UTF_8),
            "HmacSHA256"
        )

        return Mac.getInstance("HmacSHA256").apply {
            init(secretKey)
        }
    }


    fun executeCommand(server: MinecraftServer,command: String){
        server.commandManager.executeWithPrefix(server.commandSource, command)
    }

}
