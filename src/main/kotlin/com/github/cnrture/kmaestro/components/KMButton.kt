package com.github.cnrture.kmaestro.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.cnrture.kmaestro.common.NoRippleInteractionSource
import com.github.cnrture.kmaestro.theme.KMTheme

@Composable
fun KMButton(
    modifier: Modifier = Modifier,
    text: String,
    backgroundColor: Color,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = KMTheme.colors.white,
        ),
        interactionSource = NoRippleInteractionSource(),
        onClick = onClick,
        content = {
            KMText(
                text = text,
                color = KMTheme.colors.white,
            )
        },
    )
}

@Composable
fun KMOutlinedButton(
    modifier: Modifier = Modifier,
    text: String,
    backgroundColor: Color,
    onClick: () -> Unit,
) {
    OutlinedButton(
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = backgroundColor.copy(alpha = 0.1f),
            contentColor = KMTheme.colors.white,
        ),
        border = BorderStroke(
            width = 2.dp,
            color = backgroundColor,
        ),
        onClick = onClick,
        content = {
            KMText(
                text = text,
                color = KMTheme.colors.white,
            )
        },
    )
}