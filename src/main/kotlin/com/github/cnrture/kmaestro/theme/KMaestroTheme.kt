package com.github.cnrture.kmaestro.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

object KMaestroTheme {
    val colors: KMaestroColor
        @Composable
        @ReadOnlyComposable
        get() = LocalColors.current
}

@Composable
fun KMaestroTheme(content: @Composable () -> Unit) {
    KMaestroTheme(
        colors = lightColors(),
        content = content,
    )
}

@Composable
private fun KMaestroTheme(
    colors: KMaestroColor = KMaestroTheme.colors,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalColors provides colors,
    ) {
        content()
    }
}