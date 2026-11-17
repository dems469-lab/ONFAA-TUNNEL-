package com.onfaa.tunnel.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Palette identifiers. They are persisted by key, so keys must never be
 * renamed without a migration.
 *
 * The reference mock-up shows seven illustrated palettes plus a
 * "Système / Clair / Sombre" mode selector. The eighth entry of the brief
 * ("Dark / Light") is implemented as [ThemeMode], which is orthogonal to the
 * palette: 7 palettes x 3 modes.
 */
enum class ThemePaletteId(val key: String, val label: String, val tagline: String) {
    ONFAA("onfaa", "ONFAA", "Le style signature, moderne et élégant"),
    AURORA("aurora", "Aurora", "Aurores froides, teintes cyan et violette"),
    EMBER("ember", "Ember", "Braises chaudes, accents orangés"),
    GRAPHITE("graphite", "Graphite", "Neutres sombres, contraste maîtrisé"),
    ORCHID("orchid", "Orchid", "Violet profond, accents magenta"),
    PAPER("paper", "Paper", "Clair et sobre, inspiration papier"),
    MATCHA("matcha", "Matcha", "Verts naturels, ambiance apaisante");

    companion object {
        fun fromKey(key: String?): ThemePaletteId =
            entries.firstOrNull { it.key == key } ?: ONFAA
    }
}

enum class ThemeMode(val key: String, val label: String) {
    SYSTEM("system", "Système"),
    LIGHT("light", "Clair"),
    DARK("dark", "Sombre");

    companion object {
        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

@Immutable
data class ThemeSettings(
    val paletteId: ThemePaletteId = ThemePaletteId.ONFAA,
    val mode: ThemeMode = ThemeMode.SYSTEM
)

@Immutable
data class OnfaaPalette(
    val id: ThemePaletteId,
    val label: String,
    val tagline: String,
    val isDark: Boolean,
    // Screen background
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val glowPrimary: Color,
    val glowSecondary: Color,
    // Surfaces
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceMuted: Color,
    val border: Color,
    // Brand & semantic accents
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val tertiary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    // Navigation
    val navBackground: Color,
    val navSelected: Color,
    // Theme preview card gradient (decorative, not semantic)
    val previewColors: List<Color>
)

val LocalOnfaaPalette = staticCompositionLocalOf<OnfaaPalette> {
    error("OnfaaPalette is not provided; wrap the UI in OnfaaTunnelTheme")
}

fun paletteFor(id: ThemePaletteId, dark: Boolean): OnfaaPalette = when (id) {
    ThemePaletteId.ONFAA -> if (dark) OnfaaDark else OnfaaLight
    ThemePaletteId.AURORA -> if (dark) AuroraDark else AuroraLight
    ThemePaletteId.EMBER -> if (dark) EmberDark else EmberLight
    ThemePaletteId.GRAPHITE -> if (dark) GraphiteDark else GraphiteLight
    ThemePaletteId.ORCHID -> if (dark) OrchidDark else OrchidLight
    ThemePaletteId.PAPER -> if (dark) PaperDark else PaperLight
    ThemePaletteId.MATCHA -> if (dark) MatchaDark else MatchaLight
}

// ---------------------------------------------------------------------------
// ONFAA — sampled from the reference screenshots: deep navy (#021429,
// #01142B), cards (#092849, #0E244E), cyan (#08EBEF), blue (#0775F0).
// ---------------------------------------------------------------------------

private val OnfaaDark = OnfaaPalette(
    id = ThemePaletteId.ONFAA,
    label = "ONFAA",
    tagline = "Le style signature, moderne et élégant",
    isDark = true,
    backgroundTop = Color(0xFF04152B),
    backgroundBottom = Color(0xFF010812),
    glowPrimary = Color(0xFF0A6CFF),
    glowSecondary = Color(0xFF00E5FF),
    surface = Color(0xF20A1E38),
    surfaceElevated = Color(0xFF0D2949),
    surfaceMuted = Color(0xFF07182C),
    border = Color(0xFF1B3A5E),
    primary = Color(0xFF19D8F5),
    onPrimary = Color(0xFF001018),
    secondary = Color(0xFF2F80FF),
    tertiary = Color(0xFF8B5CF6),
    textPrimary = Color(0xFFEAF4FF),
    textSecondary = Color(0xFF9CB2C8),
    textMuted = Color(0xFF6E849C),
    success = Color(0xFF2FD46B),
    warning = Color(0xFFF5A524),
    danger = Color(0xFFFF5C6C),
    navBackground = Color(0xFF04101F),
    navSelected = Color(0xFF0C3157),
    previewColors = listOf(Color(0xFF04213F), Color(0xFF00E5FF), Color(0xFF2F80FF))
)

private val OnfaaLight = OnfaaPalette(
    id = ThemePaletteId.ONFAA,
    label = "ONFAA",
    tagline = "Le style signature en version claire",
    isDark = false,
    backgroundTop = Color(0xFFF3F9FF),
    backgroundBottom = Color(0xFFE1EEFF),
    glowPrimary = Color(0xFF7DD3FC),
    glowSecondary = Color(0xFF93C5FD),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFEDF6FF),
    surfaceMuted = Color(0xFFF5FAFF),
    border = Color(0xFFC4DBF1),
    primary = Color(0xFF0E7490),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF1D4ED8),
    tertiary = Color(0xFF7C3AED),
    textPrimary = Color(0xFF0B1B2B),
    textSecondary = Color(0xFF45637D),
    textMuted = Color(0xFF6B8299),
    success = Color(0xFF15803D),
    warning = Color(0xFFB45309),
    danger = Color(0xFFDC2626),
    navBackground = Color(0xFFFDFEFF),
    navSelected = Color(0xFFD7ECFF),
    previewColors = listOf(Color(0xFFDBEEFF), Color(0xFF7DD3FC), Color(0xFF2F80FF))
)

// ---------------------------------------------------------------------------
// AURORA
// ---------------------------------------------------------------------------

private val AuroraDark = OnfaaPalette(
    id = ThemePaletteId.AURORA,
    label = "Aurora",
    tagline = "Aurores froides, teintes cyan et violette",
    isDark = true,
    backgroundTop = Color(0xFF080D22),
    backgroundBottom = Color(0xFF020412),
    glowPrimary = Color(0xFF22D3EE),
    glowSecondary = Color(0xFF8B5CF6),
    surface = Color(0xF20B1230),
    surfaceElevated = Color(0xFF121A40),
    surfaceMuted = Color(0xFF080D22),
    border = Color(0xFF26305E),
    primary = Color(0xFF5EEAD4),
    onPrimary = Color(0xFF00211D),
    secondary = Color(0xFF818CF8),
    tertiary = Color(0xFFC084FC),
    textPrimary = Color(0xFFEDF7FF),
    textSecondary = Color(0xFFA5B4D4),
    textMuted = Color(0xFF6F7DA3),
    success = Color(0xFF34D399),
    warning = Color(0xFFFBBF24),
    danger = Color(0xFFFB7185),
    navBackground = Color(0xFF05081A),
    navSelected = Color(0xFF1B2750),
    previewColors = listOf(Color(0xFF0B1035), Color(0xFF22D3EE), Color(0xFF8B5CF6))
)

private val AuroraLight = OnfaaPalette(
    id = ThemePaletteId.AURORA,
    label = "Aurora",
    tagline = "Aurores lumineuses en mode clair",
    isDark = false,
    backgroundTop = Color(0xFFF4F7FF),
    backgroundBottom = Color(0xFFE8EEFF),
    glowPrimary = Color(0xFF99F6E4),
    glowSecondary = Color(0xFFC4B5FD),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFEEF2FF),
    surfaceMuted = Color(0xFFF7F8FF),
    border = Color(0xFFC7D2FE),
    primary = Color(0xFF0F766E),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF4338CA),
    tertiary = Color(0xFF9333EA),
    textPrimary = Color(0xFF10162E),
    textSecondary = Color(0xFF44507A),
    textMuted = Color(0xFF6B7799),
    success = Color(0xFF047857),
    warning = Color(0xFFB45309),
    danger = Color(0xFFBE123C),
    navBackground = Color(0xFFFDFDFF),
    navSelected = Color(0xFFE0E7FF),
    previewColors = listOf(Color(0xFFDCE6FF), Color(0xFF5EEAD4), Color(0xFF818CF8))
)

// ---------------------------------------------------------------------------
// EMBER
// ---------------------------------------------------------------------------

private val EmberDark = OnfaaPalette(
    id = ThemePaletteId.EMBER,
    label = "Ember",
    tagline = "Braises chaudes, accents orangés",
    isDark = true,
    backgroundTop = Color(0xFF1A0B07),
    backgroundBottom = Color(0xFF090302),
    glowPrimary = Color(0xFFFB923C),
    glowSecondary = Color(0xFFF43F5E),
    surface = Color(0xF222100C),
    surfaceElevated = Color(0xFF2E1712),
    surfaceMuted = Color(0xFF190A07),
    border = Color(0xFF4A2A22),
    primary = Color(0xFFFF9A4D),
    onPrimary = Color(0xFF200A00),
    secondary = Color(0xFFF43F5E),
    tertiary = Color(0xFFFBBF24),
    textPrimary = Color(0xFFFFF3EA),
    textSecondary = Color(0xFFD3A99A),
    textMuted = Color(0xFF9A7568),
    success = Color(0xFF4ADE80),
    warning = Color(0xFFFBBF24),
    danger = Color(0xFFFB7185),
    navBackground = Color(0xFF120705),
    navSelected = Color(0xFF3A1A12),
    previewColors = listOf(Color(0xFF2A0B08), Color(0xFFFB923C), Color(0xFFF43F5E))
)

private val EmberLight = OnfaaPalette(
    id = ThemePaletteId.EMBER,
    label = "Ember",
    tagline = "Braises douces en mode clair",
    isDark = false,
    backgroundTop = Color(0xFFFFF8F1),
    backgroundBottom = Color(0xFFFFE9DD),
    glowPrimary = Color(0xFFFDBA74),
    glowSecondary = Color(0xFFFDA4AF),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFFFF1E8),
    surfaceMuted = Color(0xFFFFFAF6),
    border = Color(0xFFF5C9B5),
    primary = Color(0xFFC2410C),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFBE123C),
    tertiary = Color(0xFFA16207),
    textPrimary = Color(0xFF2A120A),
    textSecondary = Color(0xFF7C4A37),
    textMuted = Color(0xFF9A6B58),
    success = Color(0xFF15803D),
    warning = Color(0xFFB45309),
    danger = Color(0xFFB91C1C),
    navBackground = Color(0xFFFFFBF7),
    navSelected = Color(0xFFFFDCC8),
    previewColors = listOf(Color(0xFFFFE3CF), Color(0xFFFDBA74), Color(0xFFF43F5E))
)

// ---------------------------------------------------------------------------
// GRAPHITE
// ---------------------------------------------------------------------------

private val GraphiteDark = OnfaaPalette(
    id = ThemePaletteId.GRAPHITE,
    label = "Graphite",
    tagline = "Neutres sombres, contraste maîtrisé",
    isDark = true,
    backgroundTop = Color(0xFF0D1117),
    backgroundBottom = Color(0xFF05070A),
    glowPrimary = Color(0xFF94A3B8),
    glowSecondary = Color(0xFF38BDF8),
    surface = Color(0xF2141A22),
    surfaceElevated = Color(0xFF1C242F),
    surfaceMuted = Color(0xFF0B0F14),
    border = Color(0xFF2E3A49),
    primary = Color(0xFFCBD5E1),
    onPrimary = Color(0xFF0B1015),
    secondary = Color(0xFF7DD3FC),
    tertiary = Color(0xFFA78BFA),
    textPrimary = Color(0xFFF1F5F9),
    textSecondary = Color(0xFFA8B5C4),
    textMuted = Color(0xFF73808F),
    success = Color(0xFF4ADE80),
    warning = Color(0xFFFACC15),
    danger = Color(0xFFFB7185),
    navBackground = Color(0xFF080B0F),
    navSelected = Color(0xFF26313F),
    previewColors = listOf(Color(0xFF15191E), Color(0xFF94A3B8), Color(0xFF475569))
)

private val GraphiteLight = OnfaaPalette(
    id = ThemePaletteId.GRAPHITE,
    label = "Graphite",
    tagline = "Neutres clairs et contrastés",
    isDark = false,
    backgroundTop = Color(0xFFF7F9FB),
    backgroundBottom = Color(0xFFE9EEF3),
    glowPrimary = Color(0xFFCBD5E1),
    glowSecondary = Color(0xFFBAE6FD),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF0F4F8),
    surfaceMuted = Color(0xFFF8FAFC),
    border = Color(0xFFCBD5E1),
    primary = Color(0xFF334155),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF0369A1),
    tertiary = Color(0xFF6D28D9),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF64748B),
    success = Color(0xFF15803D),
    warning = Color(0xFFA16207),
    danger = Color(0xFFB91C1C),
    navBackground = Color(0xFFFFFFFF),
    navSelected = Color(0xFFE2E8F0),
    previewColors = listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFF334155))
)

// ---------------------------------------------------------------------------
// ORCHID
// ---------------------------------------------------------------------------

private val OrchidDark = OnfaaPalette(
    id = ThemePaletteId.ORCHID,
    label = "Orchid",
    tagline = "Violet profond, accents magenta",
    isDark = true,
    backgroundTop = Color(0xFF160820),
    backgroundBottom = Color(0xFF08020E),
    glowPrimary = Color(0xFFE879F9),
    glowSecondary = Color(0xFFA855F7),
    surface = Color(0xF21E0B2E),
    surfaceElevated = Color(0xFF291040),
    surfaceMuted = Color(0xFF14061F),
    border = Color(0xFF45215F),
    primary = Color(0xFFF0ABFC),
    onPrimary = Color(0xFF25002E),
    secondary = Color(0xFFC084FC),
    tertiary = Color(0xFFF472B6),
    textPrimary = Color(0xFFFCF0FF),
    textSecondary = Color(0xFFC9A8D8),
    textMuted = Color(0xFF9878A8),
    success = Color(0xFF4ADE80),
    warning = Color(0xFFFBBF24),
    danger = Color(0xFFFB7185),
    navBackground = Color(0xFF100418),
    navSelected = Color(0xFF34144D),
    previewColors = listOf(Color(0xFF240B36), Color(0xFFE879F9), Color(0xFF7C3AED))
)

private val OrchidLight = OnfaaPalette(
    id = ThemePaletteId.ORCHID,
    label = "Orchid",
    tagline = "Violets lumineux en mode clair",
    isDark = false,
    backgroundTop = Color(0xFFFDF6FF),
    backgroundBottom = Color(0xFFF5E6FF),
    glowPrimary = Color(0xFFF5D0FE),
    glowSecondary = Color(0xFFDDD6FE),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFFBF0FF),
    surfaceMuted = Color(0xFFFEF9FF),
    border = Color(0xFFE9C8F7),
    primary = Color(0xFF86198F),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF6D28D9),
    tertiary = Color(0xFFBE185D),
    textPrimary = Color(0xFF2B0A36),
    textSecondary = Color(0xFF6B3B7A),
    textMuted = Color(0xFF8A5D99),
    success = Color(0xFF15803D),
    warning = Color(0xFFA16207),
    danger = Color(0xFFBE123C),
    navBackground = Color(0xFFFFFFFF),
    navSelected = Color(0xFFF3D9FF),
    previewColors = listOf(Color(0xFFF6E4FF), Color(0xFFE879F9), Color(0xFF7C3AED))
)

// ---------------------------------------------------------------------------
// PAPER
// ---------------------------------------------------------------------------

private val PaperDark = OnfaaPalette(
    id = ThemePaletteId.PAPER,
    label = "Paper",
    tagline = "Papier et encre en mode sombre",
    isDark = true,
    backgroundTop = Color(0xFF181410),
    backgroundBottom = Color(0xFF0B0906),
    glowPrimary = Color(0xFFD6B98C),
    glowSecondary = Color(0xFF93C5FD),
    surface = Color(0xF2221D17),
    surfaceElevated = Color(0xFF2C261E),
    surfaceMuted = Color(0xFF171310),
    border = Color(0xFF453B2D),
    primary = Color(0xFFE7C79A),
    onPrimary = Color(0xFF241A0C),
    secondary = Color(0xFF93C5FD),
    tertiary = Color(0xFFF0ABFC),
    textPrimary = Color(0xFFF7F1E7),
    textSecondary = Color(0xFFC4B7A3),
    textMuted = Color(0xFF958875),
    success = Color(0xFF86EFAC),
    warning = Color(0xFFFCD34D),
    danger = Color(0xFFFCA5A5),
    navBackground = Color(0xFF100D09),
    navSelected = Color(0xFF332A1E),
    previewColors = listOf(Color(0xFFE8DCC5), Color(0xFF7DB7E8), Color(0xFF2B6CB0))
)

private val PaperLight = OnfaaPalette(
    id = ThemePaletteId.PAPER,
    label = "Paper",
    tagline = "Clair et sobre, inspiration papier",
    isDark = false,
    backgroundTop = Color(0xFFFBF7EF),
    backgroundBottom = Color(0xFFF2EADC),
    glowPrimary = Color(0xFFEAD9B8),
    glowSecondary = Color(0xFFBFDBFE),
    surface = Color(0xFFFFFDF8),
    surfaceElevated = Color(0xFFF7F0E3),
    surfaceMuted = Color(0xFFFCF8F0),
    border = Color(0xFFDCCDB4),
    primary = Color(0xFF1D4ED8),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF0F766E),
    tertiary = Color(0xFF9A3412),
    textPrimary = Color(0xFF2B2620),
    textSecondary = Color(0xFF5E564B),
    textMuted = Color(0xFF7A7063),
    success = Color(0xFF15803D),
    warning = Color(0xFFA16207),
    danger = Color(0xFFB91C1C),
    navBackground = Color(0xFFFFFDF8),
    navSelected = Color(0xFFE3EDFF),
    previewColors = listOf(Color(0xFFF1E7D2), Color(0xFF93C5FD), Color(0xFF2563EB))
)

// ---------------------------------------------------------------------------
// MATCHA
// ---------------------------------------------------------------------------

private val MatchaDark = OnfaaPalette(
    id = ThemePaletteId.MATCHA,
    label = "Matcha",
    tagline = "Verts naturels, ambiance apaisante",
    isDark = true,
    backgroundTop = Color(0xFF08150D),
    backgroundBottom = Color(0xFF020A05),
    glowPrimary = Color(0xFF4ADE80),
    glowSecondary = Color(0xFFA3E635),
    surface = Color(0xF20E2116),
    surfaceElevated = Color(0xFF153021),
    surfaceMuted = Color(0xFF07130C),
    border = Color(0xFF24452F),
    primary = Color(0xFF86EFAC),
    onPrimary = Color(0xFF04210F),
    secondary = Color(0xFFBEF264),
    tertiary = Color(0xFF5EEAD4),
    textPrimary = Color(0xFFEFFBF2),
    textSecondary = Color(0xFFA7C9B1),
    textMuted = Color(0xFF749A80),
    success = Color(0xFF4ADE80),
    warning = Color(0xFFFDE047),
    danger = Color(0xFFFB7185),
    navBackground = Color(0xFF051009),
    navSelected = Color(0xFF173A25),
    previewColors = listOf(Color(0xFF0D2C18), Color(0xFF4ADE80), Color(0xFFA3E635))
)

private val MatchaLight = OnfaaPalette(
    id = ThemePaletteId.MATCHA,
    label = "Matcha",
    tagline = "Verts naturels en mode clair",
    isDark = false,
    backgroundTop = Color(0xFFF5FAEF),
    backgroundBottom = Color(0xFFE8F3DD),
    glowPrimary = Color(0xFFBBF7D0),
    glowSecondary = Color(0xFFD9F99D),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF1F8E9),
    surfaceMuted = Color(0xFFF8FCF4),
    border = Color(0xFFCBE0B5),
    primary = Color(0xFF15803D),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF4D7C0F),
    tertiary = Color(0xFF0F766E),
    textPrimary = Color(0xFF14230F),
    textSecondary = Color(0xFF4B6240),
    textMuted = Color(0xFF6B7F5D),
    success = Color(0xFF15803D),
    warning = Color(0xFFA16207),
    danger = Color(0xFFB91C1C),
    navBackground = Color(0xFFFFFFFF),
    navSelected = Color(0xFFDDEFC8),
    previewColors = listOf(Color(0xFFE8F6DA), Color(0xFF4ADE80), Color(0xFF65A30D))
)
