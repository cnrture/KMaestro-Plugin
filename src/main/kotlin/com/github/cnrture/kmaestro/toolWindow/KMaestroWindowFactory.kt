package com.github.cnrture.kmaestro.toolWindow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.github.cnrture.kmaestro.components.KMActionCard
import com.github.cnrture.kmaestro.components.KMActionCardType
import com.github.cnrture.kmaestro.theme.KMTheme
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class KMaestroWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // Force the tool window to anchor at bottom
        toolWindow.setAnchor(com.intellij.openapi.wm.ToolWindowAnchor.BOTTOM, null)

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
        var selectedTab by remember { mutableStateOf("Create") }

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
                when (selectedTab) {
                    "Create" -> CreateTestContent()
                    "Run" -> RunnerTabContent(project)
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
                .background(KMTheme.colors.black)
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
            actionColor = if (isSelected) KMTheme.colors.yellow else KMTheme.colors.lightGray,
            type = KMActionCardType.MEDIUM,
            onClick = onClick
        )
    }
}