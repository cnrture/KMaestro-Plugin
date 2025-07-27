package com.github.cnrture.kmaestro.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

object KMTheme {
    val colors: KMColor
        @Composable
        @ReadOnlyComposable
        get() = LocalColors.current
}

@Composable
fun KMTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalColors provides lightColors(),
    ) {
        content()
    }
}