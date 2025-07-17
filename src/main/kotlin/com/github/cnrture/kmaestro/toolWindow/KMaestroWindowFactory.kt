package com.github.cnrture.kmaestro.toolWindow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.cnrture.kmaestro.components.*
import com.github.cnrture.kmaestro.services.MaestroFile
import com.github.cnrture.kmaestro.services.MaestroService
import com.github.cnrture.kmaestro.theme.KMTheme
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

class KMaestroWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.contentManager.addContent(
            ContentFactory.getInstance().createContent(
                createToolWindowComponent(project),
                "",
                false,
            )
        )
    }

    private fun createToolWindowComponent(project: Project): JComponent {
        val panel = JPanel(BorderLayout())
        ComposePanel().apply {
            setContent {
                KMTheme {
                    MainContent(project)
                }
            }
            panel.add(this)
        }
        return panel
    }

    @Composable
    private fun MainContent(project: Project) {
        val maestroService = project.service<MaestroService>()
        val scope = rememberCoroutineScope()

        var directoryPath by remember { mutableStateOf(project.basePath ?: "") }
        var maestroFiles by remember { mutableStateOf<List<MaestroFile>>(emptyList()) }
        var selectedFile by remember { mutableStateOf<MaestroFile?>(null) }
        var isScanning by remember { mutableStateOf(false) }
        var isRunning by remember { mutableStateOf(false) }
        var logOutput by remember { mutableStateOf("Welcome to KMaestro!\nSelect a directory to scan for Maestro files.") }
        var selectedTab by remember { mutableStateOf("Create") }

        LaunchedEffect(Unit) {
            scanFiles(maestroService, directoryPath) { files, message ->
                maestroFiles = files
                if (files.isNotEmpty()) {
                    logOutput = " Found ${files.size} Maestro file(s) in directory"
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(KMTheme.colors.gray)
        ) {
            SidebarSection(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.width(200.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(KMTheme.colors.black)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HeaderSection()

                when (selectedTab) {
                    "Create" -> {
                        KMText(
                            text = "Create Maestro Test",
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = KMTheme.colors.green
                            ),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    "Run" -> {
                        RunnerTabContent(
                            selectedFile = selectedFile,
                            isRunning = isRunning,
                            logOutput = logOutput,
                            maestroFiles = maestroFiles,
                            onFileSelect = { selectedFile = it },
                            onFileDoubleClick = { file ->
                                ApplicationManager.getApplication().invokeLater {
                                    FileEditorManager.getInstance(project).openFile(file.virtualFile, true)
                                }
                            },
                            isScanning = isScanning,
                            onRefresh = {
                                scope.launch {
                                    isScanning = true
                                    scanFiles(maestroService, directoryPath) { files, message ->
                                        maestroFiles = files
                                        isScanning = false
                                        logOutput = if (files.isNotEmpty()) {
                                            "Refreshed - Found ${files.size} Maestro file(s)"
                                        } else {
                                            "No Maestro YAML files found in the specified directory"
                                        }
                                    }
                                }
                            },
                            onRunTest = {
                                selectedFile?.let { file ->
                                    scope.launch {
                                        isRunning = true
                                        logOutput = " Running test: ${file.name}\n\n"

                                        ApplicationManager.getApplication().executeOnPooledThread {
                                            maestroService.runMaestroTest(file.path).thenAccept { result ->
                                                SwingUtilities.invokeLater {
                                                    isRunning = false
                                                    val statusIcon = if (result.success) " " else " "
                                                    val statusText = if (result.success) "SUCCESS" else "FAILED"
                                                    logOutput =
                                                        "$statusIcon Test completed: $statusText (Exit code: ${result.exitCode})\n\n${result.output}"
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun SidebarSection(
        selectedTab: String,
        onTabSelected: (String) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier
                .fillMaxHeight()
                .background(KMTheme.colors.gray)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SidebarItem(
                icon = Icons.Default.Description,
                text = "Create Test",
                isSelected = selectedTab == "Create",
                onClick = { onTabSelected("Create") }
            )
            SidebarItem(
                icon = Icons.Default.PlayArrow,
                text = "Run Tests",
                isSelected = selectedTab == "Run",
                onClick = { onTabSelected("Run") }
            )
        }
    }

    @Composable
    private fun SidebarItem(
        icon: ImageVector,
        text: String,
        isSelected: Boolean,
        onClick: () -> Unit,
    ) {
        KMActionCard(
            modifier = Modifier.fillMaxWidth(),
            title = text,
            icon = icon,
            actionColor = if (isSelected) KMTheme.colors.purple else KMTheme.colors.lightGray,
            type = KMActionCardType.MEDIUM,
            onClick = onClick
        )
    }

    @Composable
    private fun HeaderSection() {
        KMText(
            text = "Maestro UI Testing",
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = KMTheme.colors.green
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isSelected) KMTheme.colors.purple.copy(alpha = 0.2f) else Color.Transparent,
                    RoundedCornerShape(6.dp)
                )
                .clickable {
                    onClick()
                    clickCount++
                }
                .padding(8.dp)
        ) {
            KMCheckbox(
                label = file.name,
                color = if (isSelected) KMTheme.colors.white else KMTheme.colors.lightGray,
                checked = false,
                isBackgroundEnable = true,
                onCheckedChange = {},
            )
        }
    }

    @Composable
    private fun RunnerTabContent(
        selectedFile: MaestroFile?,
        isRunning: Boolean,
        logOutput: String,
        maestroFiles: List<MaestroFile>,
        onFileSelect: (MaestroFile) -> Unit,
        onFileDoubleClick: (MaestroFile) -> Unit,
        isScanning: Boolean,
        onRefresh: () -> Unit,
        onRunTest: () -> Unit,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            KMActionCard(
                title = if (isScanning) "..." else "Refresh",
                actionColor = KMTheme.colors.green,
                icon = Icons.Default.Refresh,
                type = KMActionCardType.MEDIUM,
                onClick = onRefresh,
            )

            KMText(
                text = "Test Files (${maestroFiles.size})",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = KMTheme.colors.white
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
                if (maestroFiles.isEmpty()) {
                    KMText(
                        text = "No Maestro files found",
                        color = KMTheme.colors.hintGray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(maestroFiles) { file ->
                            FileItem(
                                file = file,
                                isSelected = selectedFile == file,
                                onClick = { onFileSelect(file) },
                                onDoubleClick = { onFileDoubleClick(file) }
                            )
                        }
                    }
                }
            }

            if (selectedFile != null) {
                KMText(
                    text = "Selected: ${selectedFile.name}",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = KMTheme.colors.white
                    )
                )
            }

            KMButton(
                text = if (isRunning) "Running..." else "Run Test",
                backgroundColor = KMTheme.colors.purple,
                onClick = onRunTest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )

            KMText(
                text = "Output",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = KMTheme.colors.white
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
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }
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
}