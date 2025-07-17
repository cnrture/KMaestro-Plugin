package com.github.cnrture.kmaestro.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.cnrture.kmaestro.common.NoRippleInteractionSource
import com.github.cnrture.kmaestro.theme.KMTheme

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun KMRadioButton(
    text: String,
    selected: Boolean,
    color: Color = KMTheme.colors.red,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .then(
                if (selected) {
                    Modifier.background(
                        color = color,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(
            LocalMinimumInteractiveComponentEnforcement provides false,
        ) {
            RadioButton(
                modifier = Modifier.scale(0.80f),
                colors = RadioButtonDefaults.colors(
                    selectedColor = KMTheme.colors.white,
                    unselectedColor = KMTheme.colors.white,
                ),
                interactionSource = NoRippleInteractionSource(),
                selected = selected,
                onClick = onClick,
            )
        }
        Spacer(modifier = Modifier.size(6.dp))
        KMText(
            text = text,
            color = KMTheme.colors.white,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}