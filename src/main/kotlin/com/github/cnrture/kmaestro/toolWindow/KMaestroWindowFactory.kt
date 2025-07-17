package com.github.cnrture.kmaestro.toolWindow

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.cnrture.kmaestro.services.MaestroFile
import com.github.cnrture.kmaestro.services.MaestroService
import com.github.cnrture.kmaestro.theme.KMTheme
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
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
        var logOutput by remember { mutableStateOf("Welcome to KMaestro! \nSelect a directory to scan for Maestro files.") }
        var statusMessage by remember { mutableStateOf("Ready") }

        LaunchedEffect(Unit) {
            scanFiles(maestroService, directoryPath) { files, message ->
                maestroFiles = files
                statusMessage = message
                if (files.isNotEmpty()) {
                    logOutput = " Found ${files.size} Maestro file(s) in directory"
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = KMTheme.colors.black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                HeaderSection()

                // Directory Section
                DirectorySection(
                    directoryPath = directoryPath,
                    onDirectoryChange = { directoryPath = it },
                    onBrowse = {
                        browseDirectory(project) { path ->
                            directoryPath = path
                            scope.launch {
                                isScanning = true
                                scanFiles(maestroService, path) { files, message ->
                                    maestroFiles = files
                                    statusMessage = message
                                    isScanning = false
                                    logOutput = if (files.isNotEmpty()) {
                                        " Found ${files.size} Maestro file(s) in directory"
                                    } else {
                                        " No Maestro YAML files found in the specified directory"
                                    }
                                }
                            }
                        }
                    },
                    onRefresh = {
                        scope.launch {
                            isScanning = true
                            scanFiles(maestroService, directoryPath) { files, message ->
                                maestroFiles = files
                                statusMessage = message
                                isScanning = false
                                logOutput = if (files.isNotEmpty()) {
                                    " Refreshed - Found ${files.size} Maestro file(s)"
                                } else {
                                    " No Maestro YAML files found in the specified directory"
                                }
                            }
                        }
                    },
                    isScanning = isScanning
                )

                // Files List Section
                FilesSection(
                    files = maestroFiles,
                    selectedFile = selectedFile,
                    onFileSelect = { selectedFile = it },
                    onFileDoubleClick = { file ->
                        ApplicationManager.getApplication().invokeLater {
                            FileEditorManager.getInstance(project).openFile(file.virtualFile, true)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                // Run Button
                RunButton(
                    enabled = selectedFile != null && !isRunning,
                    isRunning = isRunning,
                    onClick = {
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

                // Status Bar
                StatusBar(
                    status = statusMessage,
                    filesCount = maestroFiles.size
                )

                // Log Output Section
                LogSection(
                    logOutput = logOutput,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    @Composable
    private fun HeaderSection() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = KMTheme.colors.gray.copy(alpha = 0.1f),
            elevation = 8.dp,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = KMTheme.colors.purple,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "KMaestro",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = KMTheme.colors.black
                    )
                    Text(
                        "Maestro UI Testing Made Easy",
                        fontSize = 14.sp,
                        color = KMTheme.colors.hintGray
                    )
                }
            }
        }
    }

    @Composable
    private fun DirectorySection(
        directoryPath: String,
        onDirectoryChange: (String) -> Unit,
        onBrowse: () -> Unit,
        onRefresh: () -> Unit,
        isScanning: Boolean,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = KMTheme.colors.lightGray.copy(alpha = 0.1f),
            elevation = 4.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = KMTheme.colors.purple
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Maestro Files Directory",
                        fontWeight = FontWeight.Medium,
                        color = KMTheme.colors.black
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = directoryPath,
                        onValueChange = onDirectoryChange,
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                "Enter directory path...",
                                color = KMTheme.colors.hintGray
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = KMTheme.colors.purple
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = KMTheme.colors.black,
                            backgroundColor = KMTheme.colors.white,
                            focusedBorderColor = KMTheme.colors.purple,
                            unfocusedBorderColor = KMTheme.colors.lightGray
                        )
                    )

                    Button(
                        onClick = onBrowse,
                        enabled = !isScanning,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = KMTheme.colors.green,
                            contentColor = KMTheme.colors.white
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Browse")
                    }

                    Button(
                        onClick = onRefresh,
                        enabled = !isScanning,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = KMTheme.colors.green,
                            contentColor = KMTheme.colors.white
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = KMTheme.colors.white
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Refresh")
                    }
                }
            }
        }
    }

    @Composable
    private fun FilesSection(
        files: List<MaestroFile>,
        selectedFile: MaestroFile?,
        onFileSelect: (MaestroFile) -> Unit,
        onFileDoubleClick: (MaestroFile) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            backgroundColor = KMTheme.colors.lightGray.copy(alpha = 0.1f),
            elevation = 4.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = null,
                        tint = KMTheme.colors.purple
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Test Files (${files.size})",
                        fontWeight = FontWeight.Medium,
                        color = KMTheme.colors.black
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (files.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = KMTheme.colors.hintGray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No Maestro files found",
                                color = KMTheme.colors.hintGray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(files) { file ->
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
        }
    }

    @Composable
    private fun FileItem(
        file: MaestroFile,
        isSelected: Boolean,
        onClick: () -> Unit,
        onDoubleClick: () -> Unit,
    ) {
        val backgroundColor by animateColorAsState(
            targetValue = if (isSelected)
                KMTheme.colors.purple.copy(alpha = 0.2f)
            else
                Color.Transparent,
            animationSpec = tween(200)
        )

        var clickCount by remember { mutableStateOf(0) }

        LaunchedEffect(clickCount) {
            if (clickCount == 2) {
                onDoubleClick()
                clickCount = 0
            } else if (clickCount == 1) {
                kotlinx.coroutines.delay(300)
                if (clickCount == 1) {
                    clickCount = 0
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    onClick()
                    clickCount++
                },
            color = backgroundColor,
            elevation = if (isSelected) 4.dp else 0.dp
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = if (isSelected)
                        KMTheme.colors.purple
                    else
                        KMTheme.colors.hintGray
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    file.name,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected)
                        KMTheme.colors.black
                    else
                        KMTheme.colors.hintGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    @Composable
    private fun RunButton(
        enabled: Boolean,
        isRunning: Boolean,
        onClick: () -> Unit,
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = KMTheme.colors.purple,
                contentColor = KMTheme.colors.white,
                disabledBackgroundColor = KMTheme.colors.lightGray
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 8.dp
            )
        ) {
            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = KMTheme.colors.white
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Running Test...", fontWeight = FontWeight.Medium, fontSize = 16.sp)
            } else {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Run Selected Test", fontWeight = FontWeight.Medium, fontSize = 16.sp)
            }
        }
    }

    @Composable
    private fun StatusBar(
        status: String,
        filesCount: Int,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = KMTheme.colors.green.copy(alpha = 0.1f),
            elevation = 2.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    status,
                    fontSize = 14.sp,
                    color = KMTheme.colors.black,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "$filesCount files",
                    fontSize = 14.sp,
                    color = KMTheme.colors.hintGray
                )
            }
        }
    }

    @Composable
    private fun LogSection(
        logOutput: String,
        modifier: Modifier = Modifier,
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            backgroundColor = KMTheme.colors.gray.copy(alpha = 0.05f),
            elevation = 4.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Terminal,
                        contentDescription = null,
                        tint = KMTheme.colors.purple
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Output",
                        fontWeight = FontWeight.Medium,
                        color = KMTheme.colors.black
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            KMTheme.colors.lightGray.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        logOutput,
                        fontSize = 13.sp,
                        color = KMTheme.colors.black,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        lineHeight = 18.sp
                    )
                }
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

    private fun browseDirectory(project: Project, onDirectorySelected: (String) -> Unit) {
        val descriptor = FileChooserDescriptor(false, true, false, false, false, false)
        descriptor.title = "Select Maestro Files Directory"
        descriptor.description = "Choose a directory containing Maestro YAML files"

        val dialog = FileChooserFactory.getInstance().createFileChooser(descriptor, project, null)
        val selectedFiles = dialog.choose(project)

        if (selectedFiles.isNotEmpty()) {
            onDirectorySelected(selectedFiles[0].path)
        }
    }
}