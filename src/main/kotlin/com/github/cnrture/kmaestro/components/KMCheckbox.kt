package com.github.cnrture.kmaestro.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
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
fun KMCheckbox(
    modifier: Modifier = Modifier,
    checked: Boolean,
    label: String? = null,
    isBackgroundEnable: Boolean = false,
    color: Color = KMTheme.colors.yellow,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    Row(
        modifier = modifier
            .selectable(
                selected = checked,
                role = Role.Checkbox,
                onClick = { onCheckedChange(checked.not()) }
            )
            .then(
                if (isBackgroundEnable && checked) {
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
            Checkbox(
                modifier = Modifier.scale(0.80f),
                checked = checked,
                onCheckedChange = onCheckedChange,
                interactionSource = NoRippleInteractionSource(),
                colors = CheckboxDefaults.colors(
                    checkedColor = if (isBackgroundEnable && checked) {
                        KMTheme.colors.white
                    } else {
                        color
                    },
                    uncheckedColor = KMTheme.colors.white,
                    checkmarkColor = if (isBackgroundEnable && checked) {
                        color
                    } else {
                        KMTheme.colors.white
                    },
                )
            )
        }
        label?.let {
            Spacer(modifier = Modifier.size(6.dp))
            KMText(
                text = label,
                color = KMTheme.colors.white,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}