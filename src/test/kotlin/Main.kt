import com.pigeonyuze.isDebugging0
import com.pigeonyuze.util.decode.CommandConfigDecoder
import com.sksamuel.hoplite.ConfigLoaderBuilder
import java.io.File

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        isDebugging0 = true
        val codes = File(Main::class.java.getResource("commands")?.file!!)
        val configLoader = ConfigLoaderBuilder.default().build()
        codes.listFiles()?.forEach {
            CommandConfigDecoder.handle(configLoader.loadNodeOrThrow(it.path))
        }
    }
}