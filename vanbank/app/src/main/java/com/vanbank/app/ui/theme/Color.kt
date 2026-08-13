package com.vanbank.app.ui.theme

import androidx.compose.ui.graphics.Color

// Base surfaces -- black as the primary accent, near-black panels on top of it.
val VbBackground = Color(0xFF0A0A0A)
val VbPanel = Color(0xFF141C2B)
val VbPanelElevated = Color(0xFF1B2536)
val VbPanelBorder = Color(0xFF262F42)

// Text
val VbTextPrimary = Color(0xFFF5F6FA)
val VbTextSecondary = Color(0xFF9AA3B8)
val VbTextMuted = Color(0xFF6B7488)

// DIR brand accent (from the DIR/VANBank wordmark) -- used for primary actions & the logo.
val VbAccent = Color(0xFF3D5AFE)
val VbAccentDim = Color(0xFF2A3AF5)
val VbOnAccent = Color(0xFFF5F6FA)

// Semantic
val VbPositive = Color(0xFF34D399) // incoming money, paid-off, success
val VbNegative = Color(0xFFF87171) // outgoing money, declined, errors
val VbWarning = Color(0xFFFBBF24) // pending, frozen

// Debit card: dark navy-to-charcoal gradient
val VbDebitGradientStart = Color(0xFF16202F)
val VbDebitGradientEnd = Color(0xFF2B3244)

// Credit card: warm orange/rust gradient
val VbCreditGradientStart = Color(0xFFDB8A45)
val VbCreditGradientEnd = Color(0xFF7A2E12)

// Frozen card overlay
val VbFrostOverlay = Color(0x991B2536)
