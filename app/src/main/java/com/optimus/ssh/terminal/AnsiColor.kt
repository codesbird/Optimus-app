package com.optimus.ssh.terminal

import androidx.compose.ui.graphics.Color

object AnsiColor {
    // 16 Standard Terminal Colors (Tokyo Night / Dracula Inspired)
    val BLACK = Color(0xFF1F2335)
    val RED = Color(0xFFF7768E)
    val GREEN = Color(0xFF9ECE6A)
    val YELLOW = Color(0xFFE0AF68)
    val BLUE = Color(0xFF7AA2F7)
    val MAGENTA = Color(0xFFBB9AF7)
    val CYAN = Color(0xFF7DCFFF)
    val WHITE = Color(0xFFA9B1D6)

    // Bright Colors
    val BRIGHT_BLACK = Color(0xFF414868)
    val BRIGHT_RED = Color(0xFFFF9E3B)
    val BRIGHT_GREEN = Color(0xFFB9F27C)
    val BRIGHT_YELLOW = Color(0xFF0DB9D7)
    val BRIGHT_BLUE = Color(0xFF82AAFE)
    val BRIGHT_MAGENTA = Color(0xFFC099FF)
    val BRIGHT_CYAN = Color(0xFFB4F9F8)
    val BRIGHT_WHITE = Color(0xFFC0CAF5)

    val DEFAULT_FG = Color(0xFFC0CAF5)
    val DEFAULT_BG = Color(0xFF12141C)
    val CURSOR_COLOR = Color(0xFF7AA2F7)

    private val standardColors = arrayOf(
        BLACK, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE,
        BRIGHT_BLACK, BRIGHT_RED, BRIGHT_GREEN, BRIGHT_YELLOW,
        BRIGHT_BLUE, BRIGHT_MAGENTA, BRIGHT_CYAN, BRIGHT_WHITE
    )

    /**
     * Resolves an xterm 256-color index (0-255) to a Compose Color.
     */
    fun from256(index: Int): Color {
        if (index in 0..15) {
            return standardColors[index]
        }
        if (index in 16..231) {
            // 6x6x6 color cube
            val c = index - 16
            val r = (c / 36) * 51
            val g = ((c % 36) / 6) * 51
            val b = (c % 6) * 51
            return Color(r, g, b)
        }
        if (index in 232..255) {
            // Grayscale ramp from 8 to 238 in steps of 10
            val gray = 8 + (index - 232) * 10
            return Color(gray, gray, gray)
        }
        return DEFAULT_FG
    }

    fun standard(index: Int): Color {
        return standardColors.getOrElse(index % 16) { DEFAULT_FG }
    }
}
