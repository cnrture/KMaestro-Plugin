package com.github.cnrture.kmaestro.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import java.io.File
import java.io.IOException
import java.util.concurrent.CompletableFuture

@Service(Service.Level.PROJECT)
class MaestroService(private val project: Project) {

    private val logger = thisLogger()

    /**
     * Belirtilen dizinde Maestro YAML dosyalarını tarar
     * @param directoryPath Taranacak dizin yolu
     * @return Bulunan Maestro dosyalarının listesi
     */
    fun scanMaestroFiles(directoryPath: String): List<MaestroFile> {
        val files = mutableListOf<MaestroFile>()

        try {
            val directory = File(directoryPath)
            if (!directory.exists() || !directory.isDirectory) {
                logger.warn("Directory does not exist or is not a directory: $directoryPath")
                return files
            }

            // Dizini recursive olarak tarar ve YAML dosyalarını bulur
            directory.walkTopDown()
                .filter { it.isFile && (it.extension == "yaml" || it.extension == "yml") }
                .forEach { file ->
                    val virtualFile = VirtualFileManager.getInstance().findFileByUrl("file://${file.absolutePath}")
                    if (virtualFile != null) {
                        files.add(MaestroFile(file.name, file.absolutePath, virtualFile))
                    }
                }
        } catch (e: Exception) {
            logger.error("Error scanning Maestro files", e)
        }

        return files
    }

    /**
     * Belirtilen Maestro test dosyasını çalıştırır
     * @param filePath Çalıştırılacak dosya yolu
     * @return Test sonucu
     */
    fun runMaestroTest(filePath: String): CompletableFuture<MaestroTestResult> {
        return CompletableFuture.supplyAsync {
            try {
                val process = ProcessBuilder("maestro", "test", filePath)
                    .directory(File(project.basePath ?: "."))
                    .redirectErrorStream(true)
                    .start()

                val output = process.inputStream.bufferedReader().readText()
                val exitCode = process.waitFor()

                MaestroTestResult(
                    filePath = filePath,
                    exitCode = exitCode,
                    output = output,
                    success = exitCode == 0
                )
            } catch (e: IOException) {
                logger.error("Error running Maestro test", e)
                MaestroTestResult(
                    filePath = filePath,
                    exitCode = -1,
                    output = "Error: ${e.message}\nMake sure Maestro is installed and available in PATH",
                    success = false
                )
            } catch (e: InterruptedException) {
                logger.error("Maestro test interrupted", e)
                MaestroTestResult(
                    filePath = filePath,
                    exitCode = -1,
                    output = "Test interrupted: ${e.message}",
                    success = false
                )
            }
        }
    }
}

/**
 * Maestro dosyasını temsil eden data class
 */
data class MaestroFile(
    val name: String,
    val path: String,
    val virtualFile: VirtualFile,
)

/**
 * Maestro test sonucunu temsil eden data class
 */
data class MaestroTestResult(
    val filePath: String,
    val exitCode: Int,
    val output: String,
    val success: Boolean,
)