package com.github.cnrture.kmaestro.toolWindow

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.cnrture.kmaestro.components.KMText
import com.github.cnrture.kmaestro.theme.KMTheme

@Composable
fun CreateTestContent() {
    KMText(
        text = "Create Maestro Test",
        style = TextStyle(
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = KMTheme.colors.yellow
        ),
        modifier = Modifier.padding(bottom = 16.dp)
    )
}