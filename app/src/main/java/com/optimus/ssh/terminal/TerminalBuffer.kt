package com.optimus.ssh.terminal

import androidx.compose.ui.graphics.Color

data class TerminalStyle(
    var fgColor: Color = AnsiColor.DEFAULT_FG,
    var bgColor: Color = AnsiColor.DEFAULT_BG,
    var isBold: Boolean = false,
    var isUnderline: Boolean = false,
    var isInverse: Boolean = false
) {
    fun reset() {
        fgColor = AnsiColor.DEFAULT_FG
        bgColor = AnsiColor.DEFAULT_BG
        isBold = false
        isUnderline = false
        isInverse = false
    }

    fun toCell(char: Char): TerminalCell {
        val effectiveFg = if (isInverse) bgColor else fgColor
        val effectiveBg = if (isInverse) fgColor else bgColor
        return TerminalCell(
            char = char,
            fgColor = effectiveFg,
            bgColor = effectiveBg,
            isBold = isBold,
            isUnderline = isUnderline,
            isInverse = isInverse
        )
    }
}

class TerminalBuffer(
    var cols: Int = 80,
    var rows: Int = 24,
    private val maxScrollback: Int = 2000
) {
    // Primary screen buffer
    private var primaryScreen: Array<Array<TerminalCell>> = createEmptyScreen(rows, cols)
    private val scrollback: ArrayDeque<Array<TerminalCell>> = ArrayDeque()

    // Alternate screen buffer (for vim, htop, etc.)
    private var altScreen: Array<Array<TerminalCell>> = createEmptyScreen(rows, cols)
    var isAltScreen: Boolean = false
        private set

    // Cursor position (0-based)
    var cursorX: Int = 0
        private set
    var cursorY: Int = 0
        private set
    var cursorVisible: Boolean = true

    // Saved cursor position
    private var savedCursorX: Int = 0
    private var savedCursorY: Int = 0

    // Margins
    var scrollTop: Int = 0
        private set
    var scrollBottom: Int = rows - 1
        private set

    val currentStyle = TerminalStyle()

    private val activeScreen: Array<Array<TerminalCell>>
        get() = if (isAltScreen) altScreen else primaryScreen

    private fun createEmptyScreen(r: Int, c: Int): Array<Array<TerminalCell>> {
        return Array(r) { Array(c) { TerminalCell.EMPTY } }
    }

    private fun createEmptyLine(c: Int): Array<TerminalCell> {
        return Array(c) { TerminalCell.EMPTY }
    }

    @Synchronized
    fun writeChar(char: Char) {
        if (cursorX >= cols) {
            cursorX = 0
            lineFeed()
        }
        val line = activeScreen[cursorY]
        line[cursorX] = currentStyle.toCell(char)
        cursorX++
    }

    @Synchronized
    fun carriageReturn() {
        cursorX = 0
    }

    @Synchronized
    fun lineFeed() {
        if (cursorY == scrollBottom) {
            scrollUpInRegion()
        } else if (cursorY < rows - 1) {
            cursorY++
        }
    }

    @Synchronized
    fun backspace() {
        if (cursorX > 0) {
            cursorX--
        }
    }

    @Synchronized
    fun tab() {
        val nextTab = ((cursorX / 8) + 1) * 8
        cursorX = minOf(nextTab, cols - 1)
    }

    @Synchronized
    fun setCursor(x: Int, y: Int) {
        cursorX = x.coerceIn(0, cols - 1)
        cursorY = y.coerceIn(0, rows - 1)
    }

    @Synchronized
    fun moveCursor(dx: Int, dy: Int) {
        setCursor(cursorX + dx, cursorY + dy)
    }

    @Synchronized
    fun saveCursor() {
        savedCursorX = cursorX
        savedCursorY = cursorY
    }

    @Synchronized
    fun restoreCursor() {
        cursorX = savedCursorX.coerceIn(0, cols - 1)
        cursorY = savedCursorY.coerceIn(0, rows - 1)
    }

    @Synchronized
    fun setScrollRegion(top: Int, bottom: Int) {
        scrollTop = top.coerceIn(0, rows - 1)
        scrollBottom = bottom.coerceIn(scrollTop, rows - 1)
        cursorX = 0
        cursorY = 0
    }

    @Synchronized
    fun resetScrollRegion() {
        scrollTop = 0
        scrollBottom = rows - 1
    }

    private fun scrollUpInRegion() {
        if (!isAltScreen && scrollTop == 0) {
            // Push top line to scrollback
            if (scrollback.size >= maxScrollback) {
                scrollback.removeFirst()
            }
            scrollback.addLast(primaryScreen[0].copyOf())
        }

        for (y in scrollTop until scrollBottom) {
            activeScreen[y] = activeScreen[y + 1]
        }
        activeScreen[scrollBottom] = createEmptyLine(cols)
    }

    @Synchronized
    fun eraseInDisplay(mode: Int) {
        when (mode) {
            0 -> {
                // Clear from cursor to end of screen
                eraseInLine(0)
                for (y in (cursorY + 1) until rows) {
                    activeScreen[y] = createEmptyLine(cols)
                }
            }
            1 -> {
                // Clear from beginning of screen to cursor
                for (y in 0 until cursorY) {
                    activeScreen[y] = createEmptyLine(cols)
                }
                eraseInLine(1)
            }
            2, 3 -> {
                // Clear entire screen (3 also clears scrollback)
                for (y in 0 until rows) {
                    activeScreen[y] = createEmptyLine(cols)
                }
                if (mode == 3 && !isAltScreen) {
                    scrollback.clear()
                }
            }
        }
    }

    @Synchronized
    fun eraseInLine(mode: Int) {
        val line = activeScreen[cursorY]
        when (mode) {
            0 -> {
                // Clear from cursor to end of line
                for (x in cursorX until cols) {
                    line[x] = TerminalCell.EMPTY
                }
            }
            1 -> {
                // Clear from start to cursor
                for (x in 0..cursorX.coerceAtMost(cols - 1)) {
                    line[x] = TerminalCell.EMPTY
                }
            }
            2 -> {
                // Clear entire line
                activeScreen[cursorY] = createEmptyLine(cols)
            }
        }
    }

    @Synchronized
    fun enterAltScreen() {
        if (!isAltScreen) {
            isAltScreen = true
            altScreen = createEmptyScreen(rows, cols)
            cursorX = 0
            cursorY = 0
        }
    }

    @Synchronized
    fun exitAltScreen() {
        if (isAltScreen) {
            isAltScreen = false
            cursorX = 0
            cursorY = 0
        }
    }

    @Synchronized
    fun resize(newCols: Int, newRows: Int) {
        if (newCols <= 0 || newRows <= 0) return
        val newPrimary = createEmptyScreen(newRows, newCols)
        val newAlt = createEmptyScreen(newRows, newCols)

        val copyRows = minOf(rows, newRows)
        val copyCols = minOf(cols, newCols)

        for (r in 0 until copyRows) {
            for (c in 0 until copyCols) {
                newPrimary[r][c] = primaryScreen[r][c]
                newAlt[r][c] = altScreen[r][c]
            }
        }

        primaryScreen = newPrimary
        altScreen = newAlt
        cols = newCols
        rows = newRows
        scrollTop = 0
        scrollBottom = rows - 1
        cursorX = cursorX.coerceIn(0, cols - 1)
        cursorY = cursorY.coerceIn(0, rows - 1)
    }

    val scrollbackSize: Int
        @Synchronized get() = scrollback.size

    /**
     * Returns the lines to display. If scrollOffset > 0, reads from scrollback history.
     */
    @Synchronized
    fun getLines(scrollOffset: Int = 0): List<Array<TerminalCell>> {
        if (isAltScreen || scrollOffset == 0 || scrollback.isEmpty()) {
            return activeScreen.toList()
        }

        val clampedOffset = scrollOffset.coerceIn(0, scrollback.size)
        val result = mutableListOf<Array<TerminalCell>>()

        val scrollbackStart = scrollback.size - clampedOffset
        val fromScrollback = minOf(rows, clampedOffset)
        val fromScreen = rows - fromScrollback

        for (i in 0 until fromScrollback) {
            val idx = scrollbackStart + i
            if (idx in 0 until scrollback.size) {
                result.add(scrollback[idx])
            }
        }

        for (i in 0 until fromScreen) {
            if (i in 0 until rows) {
                result.add(activeScreen[i])
            }
        }

        return result
    }
}
