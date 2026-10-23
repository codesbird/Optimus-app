package com.optimus.ssh.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.optimus.ssh.ui.theme.*

@Composable
fun TerminalKeyboardBar(
    onSendKey: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isCtrlActive by remember { mutableStateOf(false) }
    var isAltActive by remember { mutableStateOf(false) }

    fun sendWithModifiers(key: String) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        var toSend = key
        if (isCtrlActive && key.isNotEmpty() && key[0].isLetter()) {
            toSend = TerminalEmulator.ctrl(key[0])
            isCtrlActive = false
        }
        if (isAltActive) {
            toSend = "\u001b$toSend"
            isAltActive = false
        }
        onSendKey(toSend)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ESC
        KeyButton(
            text = "ESC",
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSendKey(TerminalEmulator.KEY_ESC)
            }
        )

        // TAB
        KeyButton(
            text = "TAB",
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSendKey(TerminalEmulator.KEY_TAB)
            }
        )

        // BACKSPACE (1-tap quick delete)
        KeyButton(
            text = "⌫",
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSendKey(TerminalEmulator.KEY_BACKSPACE)
            }
        )

        // DEL
        KeyButton(
            text = "DEL",
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSendKey(TerminalEmulator.KEY_DELETE)
            }
        )

        // CTRL Modifier (Sticky Toggle)
        ModifierKeyButton(
            text = "CTRL",
            isActive = isCtrlActive,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                isCtrlActive = !isCtrlActive
            }
        )

        // ALT Modifier (Sticky Toggle)
        ModifierKeyButton(
            text = "ALT",
            isActive = isAltActive,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                isAltActive = !isAltActive
            }
        )

        // Arrow Keys
        IconKeyButton(
            icon = Icons.Default.KeyboardArrowUp,
            contentDescription = "Up",
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSendKey(TerminalEmulator.KEY_ARROW_UP)
            }
        )
        IconKeyButton(
            icon = Icons.Default.KeyboardArrowDown,
            contentDescription = "Down",
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSendKey(TerminalEmulator.KEY_ARROW_DOWN)
            }
        )
        IconKeyButton(
            icon = Icons.Default.KeyboardArrowLeft,
            contentDescription = "Left",
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSendKey(TerminalEmulator.KEY_ARROW_LEFT)
            }
        )
        IconKeyButton(
            icon = Icons.Default.KeyboardArrowRight,
            contentDescription = "Right",
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSendKey(TerminalEmulator.KEY_ARROW_RIGHT)
            }
        )

        // Developer Symbols
        listOf("|", "/", "-", "_", "~", ":", "$", "\\", "@", "&").forEach { symbol ->
            KeyButton(
                text = symbol,
                onClick = { sendWithModifiers(symbol) }
            )
        }
    }
}

@Composable
private fun KeyButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(38.dp)
            .widthIn(min = 38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceVariantDark)
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            ),
            color = TextPrimary
        )
    }
}

@Composable
private fun ModifierKeyButton(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isActive) PrimaryAccent else SurfaceVariantDark
    val fg = if (isActive) BackgroundDark else TextPrimary

    Box(
        modifier = Modifier
            .height(38.dp)
            .widthIn(min = 42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            ),
            color = fg
        )
    }
}

@Composable
private fun IconKeyButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceVariantDark)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = TextPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}
