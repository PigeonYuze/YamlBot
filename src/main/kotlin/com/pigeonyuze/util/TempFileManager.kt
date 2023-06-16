package com.pigeonyuze.util

import com.pigeonyuze.YamlBot
import kotlinx.coroutines.*
import java.io.File
import java.util.*
import kotlin.coroutines.CoroutineContext
import com.pigeonyuze.YamlBot.dataFolderPath as tempFilePath

object TempFileManager : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = CoroutineScope(YamlBot.coroutineContext).coroutineContext

    private val clearThreadFileSet = Collections.synchronizedSet(mutableSetOf<File>())

    private suspend fun getTempFile(tempFile: TempFileDir, tempName: String = ""): File = withContext(Dispatchers.IO) {
        val dir = File(tempFilePath.toFile(), tempFile.name).apply { mkdirs() }
        val file = File(dir, tempName)
        check(file.canWrite() && file.canRead()) { "Cannot write/read file: ${tempFile.name}" }
        return@withContext file
    }

    private fun newThreadToClearTempDir(file: File) {
        val dir = file.parentFile
        if (clearThreadFileSet.contains(file)) {
            return
        }
        launch(Dispatchers.IO + coroutineContext) {
            while (true) {
                delay(60 * 60 * 1000L)
                if (dir.listFiles()?.isEmpty() == true) {
                    clearThreadFileSet.remove(dir)
                    this.cancel()
                }
                dir.deleteRecursively()
            }
        }
    }

    data class TempFileDir(val name: String) {
        private var fileDeferred: Deferred<File>? = null

        suspend fun getFile(tempName: String = System.currentTimeMillis().toString()): File {
            if (fileDeferred?.isCompleted == true && !fileDeferred!!.isCancelled) {
                val file = fileDeferred!!.await()
                clearThreadFileSet.add(file)
                return file
            }
            return getNewDeferredFile(tempName)
        }

        private suspend fun getNewDeferredFile(tempName: String): File {
            val oldDeferred = fileDeferred
            val newDeferred = async(Dispatchers.IO) {
                getTempFile(this@TempFileDir, tempName).apply { newThreadToClearTempDir(this) }
            }
            fileDeferred = newDeferred
            oldDeferred?.cancel()
            val file = newDeferred.await()
            clearThreadFileSet.add(file)
            return file
        }
    }

    //////////////////////
    ///////  Impl  ///////
    //////////////////////
    val silkTempDir = TempFileDir("temp/silks")
    val downlandTemplateDir = TempFileDir("template/downland")
}
