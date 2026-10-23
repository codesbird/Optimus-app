package com.optimus.ssh.terminal

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TerminalEmulator(
    val buffer: TerminalBuffer = TerminalBuffer()
) {
    private enum class ParserState {
        GROUND,
        ESCAPE,
        CSI,
        OSC,
        CHARSET
    }

    private var state = ParserState.GROUND
    private val csiParams = StringBuilder()
    private val oscBuffer = StringBuilder()

    private val _windowTitle = MutableStateFlow("Terminal")
    val windowTitle: StateFlow<String> = _windowTitle.asStateFlow()

    private val _bellTrigger = MutableStateFlow(0L)
    val bellTrigger: StateFlow<Long> = _bellTrigger.asStateFlow()

    private val _updateTick = MutableStateFlow(0L)
    val updateTick: StateFlow<Long> = _updateTick.asStateFlow()

    /**
     * Feeds raw incoming bytes or text from the SSH shell into the terminal buffer.
     */
    @Synchronized
    fun feed(data: String) {
        val len = data.length
        var i = 0
        while (i < len) {
            val ch = data[i]
            when (state) {
                ParserState.GROUND -> {
                    when (ch) {
                        '\u001b' -> state = ParserState.ESCAPE
                        '\r' -> buffer.carriageReturn()
                        '\n' -> buffer.lineFeed()
                        '\b' -> buffer.backspace()
                        '\t' -> buffer.tab()
                        '\u0007' -> _bellTrigger.value = System.currentTimeMillis()
                        '\u0000', '\u000e', '\u000f' -> { /* Ignore NUL and shift */ }
                        else -> {
                            if (ch >= ' ') {
                                buffer.writeChar(ch)
                            }
                        }
                    }
                }
                ParserState.ESCAPE -> {
                    when (ch) {
                        '[' -> {
                            csiParams.clear()
                            state = ParserState.CSI
                        }
                        ']' -> {
                            oscBuffer.clear()
                            state = ParserState.OSC
                        }
                        '(', ')' -> state = ParserState.CHARSET
                        '7' -> {
                            buffer.saveCursor()
                            state = ParserState.GROUND
                        }
                        '8' -> {
                            buffer.restoreCursor()
                            state = ParserState.GROUND
                        }
                        'M' -> {
                            // Reverse Index: move cursor up or scroll down
                            if (buffer.cursorY == buffer.scrollTop) {
                                // Scroll down
                            } else {
                                buffer.moveCursor(0, -1)
                            }
                            state = ParserState.GROUND
                        }
                        'c' -> {
                            // Reset
                            buffer.eraseInDisplay(2)
                            buffer.setCursor(0, 0)
                            buffer.currentStyle.reset()
                            state = ParserState.GROUND
                        }
                        else -> state = ParserState.GROUND
                    }
                }
                ParserState.CHARSET -> {
                    // Ignore G0/G1 charset specifier (e.g. B, 0)
                    state = ParserState.GROUND
                }
                ParserState.CSI -> {
                    if (ch in '0'..'9' || ch == ';' || ch == '?' || ch == '>' || ch == ' ') {
                        csiParams.append(ch)
                    } else {
                        // Final command character
                        handleCsi(ch, csiParams.toString())
                        state = ParserState.GROUND
                    }
                }
                ParserState.OSC -> {
                    if (ch == '\u0007' || (ch == '\\' && oscBuffer.endsWith("\u001b"))) {
                        handleOsc(oscBuffer.toString().removeSuffix("\u001b"))
                        state = ParserState.GROUND
                    } else {
                        oscBuffer.append(ch)
                    }
                }
            }
            i++
        }
        _updateTick.value = System.currentTimeMillis()
    }

    private fun handleCsi(command: Char, params: String) {
        val isPrivate = params.startsWith("?")
        val cleanParams = if (isPrivate) params.substring(1) else params
        val args = if (cleanParams.isEmpty()) emptyList() else cleanParams.split(";").mapNotNull { it.toIntOrNull() }

        when (command) {
            'H', 'f' -> {
                // Cursor position: \e[<row>;<col>H (1-based)
                val row = (args.getOrNull(0) ?: 1) - 1
                val col = (args.getOrNull(1) ?: 1) - 1
                buffer.setCursor(col, row)
            }
            'A' -> {
                // Cursor Up
                val n = args.getOrNull(0) ?: 1
                buffer.moveCursor(0, -n)
            }
            'B' -> {
                // Cursor Down
                val n = args.getOrNull(0) ?: 1
                buffer.moveCursor(0, n)
            }
            'C' -> {
                // Cursor Forward
                val n = args.getOrNull(0) ?: 1
                buffer.moveCursor(n, 0)
            }
            'D' -> {
                // Cursor Backward
                val n = args.getOrNull(0) ?: 1
                buffer.moveCursor(-n, 0)
            }
            'G' -> {
                // Cursor Horizontal Absolute
                val col = (args.getOrNull(0) ?: 1) - 1
                buffer.setCursor(col, buffer.cursorY)
            }
            'd' -> {
                // Cursor Vertical Absolute
                val row = (args.getOrNull(0) ?: 1) - 1
                buffer.setCursor(buffer.cursorX, row)
            }
            'J' -> {
                // Erase in Display
                val mode = args.getOrNull(0) ?: 0
                buffer.eraseInDisplay(mode)
            }
            'K' -> {
                // Erase in Line
                val mode = args.getOrNull(0) ?: 0
                buffer.eraseInLine(mode)
            }
            'm' -> {
                // SGR: Select Graphic Rendition
                handleSgr(args)
            }
            'r' -> {
                // Set Top and Bottom Margins
                if (args.isEmpty()) {
                    buffer.resetScrollRegion()
                } else {
                    val top = (args.getOrNull(0) ?: 1) - 1
                    val bottom = (args.getOrNull(1) ?: buffer.rows) - 1
                    buffer.setScrollRegion(top, bottom)
                }
            }
            'h' -> {
                // Set Mode
                if (isPrivate) {
                    when (args.getOrNull(0)) {
                        25 -> buffer.cursorVisible = true
                        1049, 47 -> buffer.enterAltScreen()
                    }
                }
            }
            'l' -> {
                // Reset Mode
                if (isPrivate) {
                    when (args.getOrNull(0)) {
                        25 -> buffer.cursorVisible = false
                        1049, 47 -> buffer.exitAltScreen()
                    }
                }
            }
            's' -> buffer.saveCursor()
            'u' -> buffer.restoreCursor()
        }
    }

    private fun handleSgr(args: List<Int>) {
        if (args.isEmpty()) {
            buffer.currentStyle.reset()
            return
        }

        var idx = 0
        while (idx < args.size) {
            when (val code = args[idx]) {
                0 -> buffer.currentStyle.reset()
                1 -> buffer.currentStyle.isBold = true
                4 -> buffer.currentStyle.isUnderline = true
                7 -> buffer.currentStyle.isInverse = true
                22 -> buffer.currentStyle.isBold = false
                24 -> buffer.currentStyle.isUnderline = false
                27 -> buffer.currentStyle.isInverse = false
                in 30..37 -> buffer.currentStyle.fgColor = AnsiColor.standard(code - 30)
                39 -> buffer.currentStyle.fgColor = AnsiColor.DEFAULT_FG
                in 40..47 -> buffer.currentStyle.bgColor = AnsiColor.standard(code - 40)
                49 -> buffer.currentStyle.bgColor = AnsiColor.DEFAULT_BG
                in 90..97 -> buffer.currentStyle.fgColor = AnsiColor.standard(code - 90 + 8)
                in 100..107 -> buffer.currentStyle.bgColor = AnsiColor.standard(code - 100 + 8)
                38 -> {
                    // Extended FG: 38;5;<n> or 38;2;<r>;<g>;<b>
                    if (idx + 2 < args.size && args[idx + 1] == 5) {
                        buffer.currentStyle.fgColor = AnsiColor.from256(args[idx + 2])
                        idx += 2
                    } else if (idx + 4 < args.size && args[idx + 1] == 2) {
                        buffer.currentStyle.fgColor = Color(args[idx + 2], args[idx + 3], args[idx + 4])
                        idx += 4
                    }
                }
                48 -> {
                    // Extended BG: 48;5;<n> or 48;2;<r>;<g>;<b>
                    if (idx + 2 < args.size && args[idx + 1] == 5) {
                        buffer.currentStyle.bgColor = AnsiColor.from256(args[idx + 2])
                        idx += 2
                    } else if (idx + 4 < args.size && args[idx + 1] == 2) {
                        buffer.currentStyle.bgColor = Color(args[idx + 2], args[idx + 3], args[idx + 4])
                        idx += 4
                    }
                }
            }
            idx++
        }
    }

    private fun handleOsc(content: String) {
        val parts = content.split(";", limit = 2)
        if (parts.size == 2 && (parts[0] == "0" || parts[0] == "2")) {
            _windowTitle.value = parts[1]
        }
    }

    companion object {
        const val KEY_ESC = "\u001b"
        const val KEY_ENTER = "\r"
        const val KEY_BACKSPACE = "\u007f"
        const val KEY_TAB = "\t"
        const val KEY_ARROW_UP = "\u001b[A"
        const val KEY_ARROW_DOWN = "\u001b[B"
        const val KEY_ARROW_RIGHT = "\u001b[C"
        const val KEY_ARROW_LEFT = "\u001b[D"
        const val KEY_HOME = "\u001b[H"
        const val KEY_END = "\u001b[F"
        const val KEY_PAGE_UP = "\u001b[5~"
        const val KEY_PAGE_DOWN = "\u001b[6~"
        const val KEY_DELETE = "\u001b[3~"

        fun ctrl(char: Char): String {
            val code = char.uppercaseChar().code - 64
            return if (code in 1..26) code.toChar().toString() else char.toString()
        }
    }
}
