package com.optimus.ssh.terminal

import androidx.compose.ui.graphics.Color

data class TerminalCell(
    val char: Char = ' ',
    val fgColor: Color = AnsiColor.DEFAULT_FG,
    val bgColor: Color = AnsiColor.DEFAULT_BG,
    val isBold: Boolean = false,
    val isUnderline: Boolean = false,
    val isInverse: Boolean = false
) {
    companion object {
        val EMPTY = TerminalCell()
    }
}
