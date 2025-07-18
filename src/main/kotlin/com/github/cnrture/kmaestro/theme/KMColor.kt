package com.github.cnrture.kmaestro.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

fun lightColors(
    white: Color = Color(0xffecedee),
    black: Color = Color(0xFF18181b),
    gray: Color = Color(0xff232327),
    lightGray: Color = Color(0xffa1a1aa),
    green: Color = Color(0xff339e48),
    red: Color = Color(0xffE44857),
    hintGray: Color = Color(0xFF565656),
    yellow: Color = Color(0xfff5a524),
): KMColor = KMColor(
    white = white,
    black = black,
    gray = gray,
    lightGray = lightGray,
    green = green,
    red = red,
    hintGray = hintGray,
    yellow = yellow,
)

class KMColor(
    white: Color,
    black: Color,
    gray: Color,
    lightGray: Color,
    green: Color,
    red: Color,
    hintGray: Color,
    yellow: Color,
) {
    private var _white: Color by mutableStateOf(white)
    val white: Color = _white

    private var _black: Color by mutableStateOf(black)
    val black: Color = _black

    private var _gray: Color by mutableStateOf(gray)
    val gray: Color = _gray

    private var _lightGray: Color by mutableStateOf(lightGray)
    val lightGray: Color = _lightGray

    private var _red: Color by mutableStateOf(red)
    val red: Color = _red

    private var _green: Color by mutableStateOf(green)
    val green: Color = _green

    private var _hintGray: Color by mutableStateOf(hintGray)
    val hintGray: Color = _hintGray

    private var _yellow: Color by mutableStateOf(yellow)
    val yellow: Color = _yellow
}

internal val LocalColors = staticCompositionLocalOf { lightColors() }