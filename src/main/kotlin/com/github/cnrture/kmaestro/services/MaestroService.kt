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

    fun scanMaestroFiles(directoryPath: String): List<MaestroFile> {
        val files = mutableListOf<MaestroFile>()

        try {
            val directory = File(directoryPath)
            if (!directory.exists() || !directory.isDirectory) {
                logger.warn("Directory does not exist or is not a directory: $directoryPath")
                return files
            }

            directory.walkTopDown()
                .filter { it.isFile && (it.extension == "yaml" || it.extension == "yml") }
                .filter { isMaestroFile(it) }
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

    private fun isMaestroFile(file: File): Boolean {
        return try {
            file.readLines().any { line ->
                val trimmedLine = line.trim()
                trimmedLine.contains("appId:")
            }
        } catch (e: Exception) {
            logger.warn("Error reading file for Maestro validation: ${file.absolutePath}", e)
            false
        }
    }

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

    fun runMultipleMaestroTests(
        filePaths: List<String>,
        onProgress: (currentIndex: Int, total: Int, currentFile: String, result: MaestroTestResult) -> Unit,
    ): CompletableFuture<MaestroMultiTestResult> {
        return CompletableFuture.supplyAsync {
            val results = mutableListOf<MaestroTestResult>()
            val startTime = System.currentTimeMillis()

            filePaths.forEachIndexed { index, filePath ->
                logger.info("Running test ${index + 1}/${filePaths.size}: $filePath")

                try {
                    val process = ProcessBuilder("maestro", "test", filePath)
                        .directory(File(project.basePath ?: "."))
                        .redirectErrorStream(true)
                        .start()

                    val output = process.inputStream.bufferedReader().readText()
                    val exitCode = process.waitFor()

                    val result = MaestroTestResult(
                        filePath = filePath,
                        exitCode = exitCode,
                        output = output,
                        success = exitCode == 0
                    )

                    results.add(result)

                    onProgress(index + 1, filePaths.size, File(filePath).name, result)

                } catch (e: IOException) {
                    logger.error("Error running Maestro test for $filePath", e)
                    val result = MaestroTestResult(
                        filePath = filePath,
                        exitCode = -1,
                        output = "Error: ${e.message}\nMake sure Maestro is installed and available in PATH",
                        success = false
                    )
                    results.add(result)
                    onProgress(index + 1, filePaths.size, File(filePath).name, result)
                } catch (e: InterruptedException) {
                    logger.error("Maestro test interrupted for $filePath", e)
                    val result = MaestroTestResult(
                        filePath = filePath,
                        exitCode = -1,
                        output = "Test interrupted: ${e.message}",
                        success = false
                    )
                    results.add(result)
                    onProgress(index + 1, filePaths.size, File(filePath).name, result)
                }
            }

            val endTime = System.currentTimeMillis()
            val totalDuration = endTime - startTime

            MaestroMultiTestResult(
                results = results,
                totalTests = filePaths.size,
                successfulTests = results.count { it.success },
                failedTests = results.count { !it.success },
                totalDurationMs = totalDuration
            )
        }
    }
}

data class MaestroFile(
    val name: String,
    val path: String,
    val virtualFile: VirtualFile,
)

data class MaestroTestResult(
    val filePath: String,
    val exitCode: Int,
    val output: String,
    val success: Boolean,
)

data class MaestroMultiTestResult(
    val results: List<MaestroTestResult>,
    val totalTests: Int,
    val successfulTests: Int,
    val failedTests: Int,
    val totalDurationMs: Long,
)