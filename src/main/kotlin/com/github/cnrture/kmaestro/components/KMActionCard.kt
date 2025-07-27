package com.github.cnrture.kmaestro.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.cnrture.kmaestro.theme.KMTheme

enum class KMActionCardType { SMALL, MEDIUM, LARGE }

@Composable
fun KMActionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector? = null,
    actionColor: Color,
    isTextVisible: Boolean = true,
    type: KMActionCardType = KMActionCardType.LARGE,
    isEnabled: Boolean = true,
    onClick: () -> Unit,
) {
    val fontSize = when (type) {
        KMActionCardType.SMALL -> 14.sp
        KMActionCardType.MEDIUM -> 16.sp
        KMActionCardType.LARGE -> 20.sp
    }
    val iconBoxSize = when (type) {
        KMActionCardType.SMALL -> 24.dp
        KMActionCardType.MEDIUM -> 28.dp
        KMActionCardType.LARGE -> 32.dp
    }
    val iconSize = when (type) {
        KMActionCardType.SMALL -> 16.dp
        KMActionCardType.MEDIUM -> 20.dp
        KMActionCardType.LARGE -> 24.dp
    }
    val borderSize = when (type) {
        KMActionCardType.SMALL -> 1.dp
        KMActionCardType.MEDIUM -> 2.dp
        KMActionCardType.LARGE -> 3.dp
    }
    val padding = when (type) {
        KMActionCardType.SMALL -> 8.dp
        KMActionCardType.MEDIUM -> 12.dp
        KMActionCardType.LARGE -> 16.dp
    }
    Row(
        modifier = modifier
            .background(
                color = if (isEnabled) KMTheme.colors.gray else KMTheme.colors.lightGray.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = borderSize,
                color = if (isEnabled) actionColor else KMTheme.colors.lightGray.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .then(
                if (isEnabled) Modifier.clickable { onClick() }
                else Modifier
            )
            .padding(padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        icon?.let {
            if (type == KMActionCardType.LARGE) {
                Box(
                    modifier = Modifier
                        .size(iconBoxSize)
                        .clip(RoundedCornerShape(8.dp))
                        .background(actionColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = KMTheme.colors.white,
                        modifier = Modifier.size(iconSize)
                    )
                }
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = actionColor,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        if (icon != null && title != null && isTextVisible) {
            Spacer(modifier = Modifier.width(8.dp))
        }

        if (isTextVisible) {
            title?.let {
                KMText(
                    text = it,
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        fontSize = fontSize,
                        color = KMTheme.colors.white,
                    ),
                )
            }
        }
    }
}