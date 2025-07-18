package com.github.cnrture.kmaestro.toolWindow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.cnrture.kmaestro.components.KMActionCard
import com.github.cnrture.kmaestro.components.KMActionCardType
import com.github.cnrture.kmaestro.components.KMCheckbox
import com.github.cnrture.kmaestro.components.KMText
import com.github.cnrture.kmaestro.services.MaestroFile
import com.github.cnrture.kmaestro.services.MaestroService
import com.github.cnrture.kmaestro.theme.KMTheme
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.swing.SwingUtilities

@Composable
fun RunnerTabContent(project: Project) {
    val maestroService = project.service<MaestroService>()
    val scope = rememberCoroutineScope()
    var directoryPath by remember { mutableStateOf(project.basePath ?: "") }
    var maestroFiles by remember { mutableStateOf<List<MaestroFile>>(emptyList()) }
    var selectedFiles by remember { mutableStateOf<Set<MaestroFile>>(emptySet()) }
    var isScanning by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    var logOutput by remember { mutableStateOf("Welcome to KMaestro!\nSelect files to run Maestro tests sequentially.") }
    var currentTestProgress by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        scanFiles(maestroService, directoryPath) { files, message ->
            maestroFiles = files
            if (files.isNotEmpty()) {
                logOutput = "✓ Found ${files.size} Maestro file(s) in directory"
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with file count and controls
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KMText(
                text = "Test Files",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KMTheme.colors.yellow,
                )
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Select All button
                KMActionCard(
                    title = "Select All",
                    actionColor = KMTheme.colors.green,
                    icon = Icons.Default.SelectAll,
                    type = KMActionCardType.SMALL,
                    onClick = {
                        selectedFiles = maestroFiles.toSet()
                    },
                )

                // Deselect All button
                KMActionCard(
                    title = "Clear",
                    actionColor = KMTheme.colors.red,
                    icon = Icons.Default.Clear,
                    type = KMActionCardType.SMALL,
                    onClick = {
                        selectedFiles = emptySet()
                    },
                )

                // Refresh button
                KMActionCard(
                    title = if (isScanning) "..." else "Refresh",
                    actionColor = KMTheme.colors.yellow,
                    icon = Icons.Default.Refresh,
                    type = KMActionCardType.SMALL,
                    onClick = {
                        scope.launch {
                            isScanning = true
                            scanFiles(maestroService, directoryPath) { files, message ->
                                maestroFiles = files
                                selectedFiles = emptySet()
                                isScanning = false
                                logOutput = if (files.isNotEmpty()) {
                                    "✓ Refreshed - Found ${files.size} Maestro file(s)"
                                } else {
                                    "⚠ No Maestro YAML files found in the specified directory"
                                }
                            }
                        }
                    },
                )

                // Run button
                KMActionCard(
                    title = if (isRunning) "Running ${selectedFiles.size} tests..." else "Run Selected Tests (${selectedFiles.size})",
                    actionColor = if (selectedFiles.isNotEmpty()) KMTheme.colors.yellow else KMTheme.colors.lightGray,
                    type = KMActionCardType.SMALL,
                    icon = Icons.Rounded.PlayArrow,
                    onClick = {
                        if (selectedFiles.isNotEmpty() && !isRunning) {
                            scope.launch {
                                isRunning = true
                                val filePaths = selectedFiles.map { it.path }

                                logOutput = "🚀 Starting ${selectedFiles.size} test(s) sequentially...\n\n"
                                currentTestProgress = "Preparing tests..."

                                ApplicationManager.getApplication().executeOnPooledThread {
                                    maestroService.runMultipleMaestroTests(
                                        filePaths = filePaths,
                                        onProgress = { currentIndex, total, currentFile, result ->
                                            SwingUtilities.invokeLater {
                                                val statusIcon = if (result.success) "✅" else "❌"
                                                val progressText = "Test $currentIndex/$total: $currentFile $statusIcon"
                                                currentTestProgress = progressText

                                                logOutput += "\n${"=".repeat(60)}\n"
                                                logOutput += "📋 $progressText\n"
                                                logOutput += "${"=".repeat(60)}\n"
                                                logOutput += result.output
                                                logOutput += "\n"
                                            }
                                        }
                                    ).thenAccept { multiResult ->
                                        SwingUtilities.invokeLater {
                                            isRunning = false
                                            currentTestProgress = ""

                                            val summary = """
                                        
                                        ${"=".repeat(60)}
                                        📊 FINAL TEST SUMMARY 📊
                                        ${"=".repeat(60)}
                                        📁 Total Tests: ${multiResult.totalTests}
                                        ✅ Successful: ${multiResult.successfulTests}
                                        ❌ Failed: ${multiResult.failedTests}
                                        ⏱️ Total Duration: ${multiResult.totalDurationMs / 1000.0}s
                                        📈 Success Rate: ${(multiResult.successfulTests.toFloat() / multiResult.totalTests * 100).toInt()}%
                                        ${"=".repeat(60)}
                                    """.trimIndent()

                                            logOutput += summary
                                        }
                                    }
                                }
                            }
                        }
                    },
                )
            }

            // Files List
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        KMTheme.colors.gray,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(16.dp)
            ) {
                if (maestroFiles.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        KMText(
                            text = "No Maestro files found",
                            color = KMTheme.colors.hintGray,
                            style = TextStyle(fontSize = 16.sp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        KMText(
                            text = "Make sure you have Maestro YAML files with 'appId:' in your project",
                            color = KMTheme.colors.hintGray,
                            style = TextStyle(fontSize = 12.sp)
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(maestroFiles) { file ->
                            FileItem(
                                file = file,
                                isSelected = selectedFiles.contains(file),
                                onClick = {
                                    selectedFiles = if (selectedFiles.contains(file)) {
                                        selectedFiles - file
                                    } else {
                                        selectedFiles + file
                                    }
                                },
                                onDoubleClick = {
                                    ApplicationManager.getApplication().invokeLater {
                                        FileEditorManager.getInstance(project).openFile(file.virtualFile, true)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Current test progress
            if (currentTestProgress.isNotEmpty()) {
                KMText(
                    text = "🔄 $currentTestProgress",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = KMTheme.colors.yellow
                    )
                )
            }
            // Output section
            KMText(
                text = "Output",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KMTheme.colors.yellow,
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        KMTheme.colors.gray,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(16.dp)
            ) {
                KMText(
                    text = logOutput,
                    color = KMTheme.colors.white,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun FileItem(
    file: MaestroFile,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
) {
    var clickCount by remember { mutableStateOf(0) }

    LaunchedEffect(clickCount) {
        if (clickCount == 2) {
            onDoubleClick()
            clickCount = 0
        } else if (clickCount == 1) {
            delay(300)
            if (clickCount == 1) {
                clickCount = 0
            }
        }
    }

    KMCheckbox(
        label = file.name,
        color = if (isSelected) KMTheme.colors.yellow else KMTheme.colors.lightGray,
        checked = isSelected,
        isBackgroundEnable = true,
        onCheckedChange = {
            onClick()
            clickCount++
        },
    )
}

private fun scanFiles(
    maestroService: MaestroService,
    path: String,
    onResult: (List<MaestroFile>, String) -> Unit,
) {
    ApplicationManager.getApplication().executeOnPooledThread {
        val files = maestroService.scanMaestroFiles(path)
        val message = if (files.isEmpty()) "No files found" else "Found ${files.size} files"

        SwingUtilities.invokeLater {
            onResult(files, message)
        }
    }
}