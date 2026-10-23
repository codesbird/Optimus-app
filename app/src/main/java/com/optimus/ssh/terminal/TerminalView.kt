package com.optimus.ssh.terminal

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

private const val SENTINEL = " "

@Composable
fun TerminalView(
    emulator: TerminalEmulator,
    onSendInput: (String) -> Unit,
    onResizePty: (cols: Int, rows: Int) -> Unit,
    modifier: Modifier = Modifier,
    fontSizeSp: Float = 12f
) {
    val density = LocalDensity.current
    val focusRequester = remember { FocusRequester() }

    // Use a persistent non-empty sentinel so Android IME Backspace key is reliably captured
    var textInput by remember { mutableStateOf(TextFieldValue(SENTINEL, TextRange(SENTINEL.length))) }
    var scrollOffsetY by remember { mutableStateOf(0) }
    var scrollOffsetX by remember { mutableStateOf(0f) }

    // Collect buffer updates to redraw canvas
    val updateTick by emulator.updateTick.collectAsState()

    // Cursor blink animation
    val infiniteTransition = rememberInfiniteTransition(label = "cursorBlink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    // Text Paint with monospace typeface
    val fontSizePx = with(density) { fontSizeSp.sp.toPx() }
    val paint = remember(fontSizePx) {
        Paint().apply {
            typeface = Typeface.MONOSPACE
            textSize = fontSizePx
            isAntiAlias = true
        }
    }

    val charWidth = remember(paint) { paint.measureText("M") }
    val fontMetrics = remember(paint) { paint.fontMetrics }
    val charHeight = remember(fontMetrics) { fontMetrics.descent - fontMetrics.ascent }
    val baselineOffset = remember(fontMetrics) { -fontMetrics.ascent }

    // Request keyboard focus automatically
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AnsiColor.DEFAULT_BG)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        focusRequester.requestFocus()
                    },
                    onDoubleTap = {
                        // Double tap resets horizontal and vertical scroll
                        scrollOffsetX = 0f
                        scrollOffsetY = 0
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    if (abs(dragAmount.x) > abs(dragAmount.y) * 1.2f) {
                        // Horizontal swipe: scroll horizontally
                        scrollOffsetX = (scrollOffsetX - dragAmount.x).coerceAtLeast(0f)
                    } else {
                        // Vertical swipe: scroll through terminal history
                        if (dragAmount.y > 0) {
                            scrollOffsetY = (scrollOffsetY - 1).coerceAtLeast(0)
                        } else if (dragAmount.y < 0) {
                            val maxScroll = emulator.buffer.scrollbackSize
                            scrollOffsetY = (scrollOffsetY + 1).coerceAtMost(maxScroll)
                        }
                    }
                }
            }
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.Enter -> {
                            onSendInput(TerminalEmulator.KEY_ENTER)
                            true
                        }
                        Key.Backspace -> {
                            onSendInput(TerminalEmulator.KEY_BACKSPACE)
                            true
                        }
                        Key.Tab -> {
                            onSendInput(TerminalEmulator.KEY_TAB)
                            true
                        }
                        Key.DirectionUp -> {
                            onSendInput(TerminalEmulator.KEY_ARROW_UP)
                            true
                        }
                        Key.DirectionDown -> {
                            onSendInput(TerminalEmulator.KEY_ARROW_DOWN)
                            true
                        }
                        Key.DirectionLeft -> {
                            onSendInput(TerminalEmulator.KEY_ARROW_LEFT)
                            true
                        }
                        Key.DirectionRight -> {
                            onSendInput(TerminalEmulator.KEY_ARROW_RIGHT)
                            true
                        }
                        Key.Escape -> {
                            onSendInput(TerminalEmulator.KEY_ESC)
                            true
                        }
                        else -> {
                            val unicode = keyEvent.utf16CodePoint
                            if (unicode > 0) {
                                onSendInput(unicode.toChar().toString())
                                true
                            } else false
                        }
                    }
                } else false
            }
            .focusRequester(focusRequester)
            .focusable()
    ) {
        // Invisible input field catching virtual keyboard input
        BasicTextField(
            value = textInput,
            onValueChange = { newValue ->
                if (textInput.text.isNotEmpty() && newValue.text.isEmpty()) {
                    // Backspace detected from software keyboard!
                    onSendInput(TerminalEmulator.KEY_BACKSPACE)
                    textInput = TextFieldValue(SENTINEL, TextRange(1))
                } else if (newValue.text.length > 1) {
                    val typed = if (newValue.text.startsWith(SENTINEL)) {
                        newValue.text.substring(1)
                    } else {
                        newValue.text
                    }
                    if (typed.isNotEmpty()) {
                        onSendInput(typed)
                    }
                    textInput = TextFieldValue(SENTINEL, TextRange(1))
                } else if (newValue.text != SENTINEL) {
                    textInput = TextFieldValue(SENTINEL, TextRange(1))
                }
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.None
            ),
            keyboardActions = KeyboardActions(
                onAny = {
                    onSendInput(TerminalEmulator.KEY_ENTER)
                }
            ),
            modifier = Modifier
                .size(1.dp)
                .alpha(0.01f)
                .focusRequester(focusRequester)
        )

        // Terminal Canvas Rendering
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Read updateTick to trigger canvas redraw when new terminal bytes arrive
            @Suppress("UNUSED_VARIABLE")
            val tick = updateTick

            val canvasWidth = size.width
            val canvasHeight = size.height

            if (charWidth > 0 && charHeight > 0) {
                val measuredCols = (canvasWidth / charWidth).toInt().coerceIn(30, 200)
                val measuredRows = (canvasHeight / charHeight).toInt().coerceIn(5, 100)

                if (measuredCols != emulator.buffer.cols || measuredRows != emulator.buffer.rows) {
                    emulator.buffer.resize(measuredCols, measuredRows)
                    onResizePty(measuredCols, measuredRows)
                }
            }

            // Clamp horizontal scroll so it doesn't scroll excessively past the line width
            val contentWidthPx = emulator.buffer.cols * charWidth
            val maxScrollX = (contentWidthPx - canvasWidth).coerceAtLeast(0f)
            val currentScrollX = scrollOffsetX.coerceIn(0f, maxScrollX)

            val lines = emulator.buffer.getLines(scrollOffsetY)
            val nativeCanvas = drawContext.canvas.nativeCanvas

            // Draw line by line
            for (rowIdx in lines.indices) {
                val rowY = rowIdx * charHeight
                if (rowY > canvasHeight) break

                val line = lines[rowIdx]
                var colIdx = 0
                while (colIdx < line.size) {
                    val cell = line[colIdx]
                    val cellX = (colIdx * charWidth) - currentScrollX

                    // Draw custom background if not default
                    if (cell.bgColor != AnsiColor.DEFAULT_BG) {
                        drawRect(
                            color = cell.bgColor,
                            topLeft = Offset(cellX, rowY),
                            size = Size(charWidth, charHeight)
                        )
                    }

                    // Group adjacent characters with identical styling
                    val startCol = colIdx
                    val textBuilder = StringBuilder()
                    while (colIdx < line.size &&
                        line[colIdx].fgColor == cell.fgColor &&
                        line[colIdx].isBold == cell.isBold &&
                        line[colIdx].isUnderline == cell.isUnderline
                    ) {
                        textBuilder.append(line[colIdx].char)
                        colIdx++
                    }

                    paint.color = cell.fgColor.toArgb()
                    paint.isFakeBoldText = cell.isBold
                    paint.isUnderlineText = cell.isUnderline

                    val textX = (startCol * charWidth) - currentScrollX
                    nativeCanvas.drawText(
                        textBuilder.toString(),
                        textX,
                        rowY + baselineOffset,
                        paint
                    )
                }
            }

            // Draw Cursor (only if at bottom of scrollback)
            if (scrollOffsetY == 0 && emulator.buffer.cursorVisible && cursorAlpha > 0.3f) {
                val cursorX = (emulator.buffer.cursorX * charWidth) - currentScrollX
                val cursorY = emulator.buffer.cursorY * charHeight
                drawRect(
                    color = AnsiColor.CURSOR_COLOR.copy(alpha = cursorAlpha),
                    topLeft = Offset(cursorX, cursorY),
                    size = Size(charWidth, charHeight)
                )
            }
        }
    }
}
