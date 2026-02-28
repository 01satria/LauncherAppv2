package id.satria.launcher.data

import kotlinx.serialization.Serializable

// ─────────────────────────────────────────────────────────────────────────────
// App Categories
// ─────────────────────────────────────────────────────────────────────────────
enum class AppCategory(val displayName: String, val emoji: String) {
    SOCIAL       ("Social",        "💬"),
    GAMES        ("Games",         "🎮"),
    PRODUCTIVITY ("Productivity",  "💼"),
    ENTERTAINMENT("Entertainment", "🎬"),
    FINANCE      ("Finance",       "💳"),
    HEALTH       ("Health",        "❤️"),
    SHOPPING     ("Shopping",      "🛍️"),
    EDUCATION    ("Education",     "📚"),
    TOOLS        ("Tools",         "🔧"),
    MUSIC        ("Music",         "🎵"),
    PHOTO        ("Photo",         "📷"),
    MAPS         ("Maps",          "🗺️"),
    NEWS         ("News",          "📰"),
    FOOD         ("Food",          "🍔"),
    TRAVEL       ("Travel",        "✈️"),
    SYSTEM       ("System",        "⚙️"),
    OTHER        ("Other",         "📱"),
}

// ─────────────────────────────────────────────────────────────────────────────
// Icon Shape
// ─────────────────────────────────────────────────────────────────────────────
enum class IconShape(val label: String) {
    CIRCLE         ("Circle"),
    ROUNDED_SQUARE ("Rounded"),
    SQUIRCLE       ("Squircle"),
    TEARDROP       ("Teardrop"),
    NONE           ("Original"),
}

// ─────────────────────────────────────────────────────────────────────────────
// Icon Effect
// ─────────────────────────────────────────────────────────────────────────────
enum class IconEffect(val label: String) {
    NONE       ("None"),
    GRADIENT   ("Gradient"),
    WATERCOLOR ("Watercolor"),
    GLASS      ("Glass"),
}

// ─────────────────────────────────────────────────────────────────────────────
// CategoryStyle — visual style untuk satu kategori
// Warna disimpan sebagai Long (ARGB) agar serializable tanpa overhead
// ─────────────────────────────────────────────────────────────────────────────
@Serializable
data class CategoryStyle(
    val shape          : String = IconShape.SQUIRCLE.name,
    val effect         : String = IconEffect.NONE.name,
    val primaryColor   : Long   = 0xFF27AE60,     // default: green accent
    val secondaryColor : Long   = 0xFF1A5C38,
    val borderThickness: Float  = 0f,
    val borderColor    : Long   = 0xFFFFFFFF,
    val opacity        : Float  = 1f,
) {
    fun shapeEnum()  : IconShape  = runCatching { IconShape.valueOf(shape) }.getOrDefault(IconShape.SQUIRCLE)
    fun effectEnum() : IconEffect = runCatching { IconEffect.valueOf(effect) }.getOrDefault(IconEffect.NONE)
}

// ─────────────────────────────────────────────────────────────────────────────
// Default styles per category — digunakan sebagai starting point
// ─────────────────────────────────────────────────────────────────────────────
val DEFAULT_CATEGORY_STYLES: Map<AppCategory, CategoryStyle> = mapOf(
    AppCategory.SOCIAL        to CategoryStyle(shape = "CIRCLE",          primaryColor = 0xFF1877F2, secondaryColor = 0xFF0A5DC9),
    AppCategory.GAMES         to CategoryStyle(shape = "SQUIRCLE",        primaryColor = 0xFFE91E63, secondaryColor = 0xFF880E4F, effect = "GRADIENT"),
    AppCategory.PRODUCTIVITY  to CategoryStyle(shape = "ROUNDED_SQUARE",  primaryColor = 0xFF1565C0, secondaryColor = 0xFF0D47A1),
    AppCategory.ENTERTAINMENT to CategoryStyle(shape = "SQUIRCLE",        primaryColor = 0xFFE53935, secondaryColor = 0xFFB71C1C, effect = "GRADIENT"),
    AppCategory.FINANCE       to CategoryStyle(shape = "ROUNDED_SQUARE",  primaryColor = 0xFF2E7D32, secondaryColor = 0xFF1B5E20),
    AppCategory.HEALTH        to CategoryStyle(shape = "CIRCLE",          primaryColor = 0xFFFF5252, secondaryColor = 0xFFD50000, effect = "WATERCOLOR"),
    AppCategory.SHOPPING      to CategoryStyle(shape = "SQUIRCLE",        primaryColor = 0xFFFF6F00, secondaryColor = 0xFFE65100),
    AppCategory.EDUCATION     to CategoryStyle(shape = "ROUNDED_SQUARE",  primaryColor = 0xFF6A1B9A, secondaryColor = 0xFF4A148C),
    AppCategory.TOOLS         to CategoryStyle(shape = "ROUNDED_SQUARE",  primaryColor = 0xFF546E7A, secondaryColor = 0xFF37474F),
    AppCategory.MUSIC         to CategoryStyle(shape = "CIRCLE",          primaryColor = 0xFFAD1457, secondaryColor = 0xFF880E4F, effect = "GRADIENT"),
    AppCategory.PHOTO         to CategoryStyle(shape = "SQUIRCLE",        primaryColor = 0xFFBF360C, secondaryColor = 0xFF870000),
    AppCategory.MAPS          to CategoryStyle(shape = "TEARDROP",        primaryColor = 0xFF00897B, secondaryColor = 0xFF004D40),
    AppCategory.NEWS          to CategoryStyle(shape = "ROUNDED_SQUARE",  primaryColor = 0xFF424242, secondaryColor = 0xFF212121),
    AppCategory.FOOD          to CategoryStyle(shape = "CIRCLE",          primaryColor = 0xFFF57C00, secondaryColor = 0xFFE65100, effect = "WATERCOLOR"),
    AppCategory.TRAVEL        to CategoryStyle(shape = "TEARDROP",        primaryColor = 0xFF0277BD, secondaryColor = 0xFF01579B),
    AppCategory.SYSTEM        to CategoryStyle(shape = "ROUNDED_SQUARE",  primaryColor = 0xFF455A64, secondaryColor = 0xFF263238),
    AppCategory.OTHER         to CategoryStyle(shape = "SQUIRCLE",        primaryColor = 0xFF616161, secondaryColor = 0xFF424242),
)
