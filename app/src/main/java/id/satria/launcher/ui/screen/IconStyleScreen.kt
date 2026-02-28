package id.satria.launcher.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import id.satria.launcher.MainViewModel
import id.satria.launcher.data.*
import id.satria.launcher.ui.component.*
import id.satria.launcher.ui.theme.SatriaColors
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// IconStyleScreen — editor kategori + style ikon
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun IconStyleScreen(vm: MainViewModel, onClose: () -> Unit) {
    val scope           = rememberCoroutineScope()
    val allApps         by vm.allApps.collectAsState()
    val appCategories   by vm.appCategories.collectAsState()
    val categoryStyles  by vm.categoryStyles.collectAsState()

    // Kategori yang sedang dipilih untuk diedit
    var selectedCategory by remember { mutableStateOf<AppCategory?>(null) }

    // Tab: "Styles" atau "Apps"
    var tab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize().background(SatriaColors.Surface),
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {
                if (selectedCategory != null) selectedCategory = null else onClose()
            }) {
                Text("←", color = SatriaColors.TextPrimary, fontSize = 20.sp)
            }
            Text(
                text       = if (selectedCategory != null)
                                 "${selectedCategory!!.emoji} ${selectedCategory!!.displayName}"
                             else "Icon Styles ✦",
                color      = SatriaColors.TextPrimary,
                fontSize   = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.weight(1f).padding(start = 4.dp),
            )
            if (selectedCategory == null) {
                TextButton(onClick = {
                    scope.launch { vm.reclassifyAll() }
                }) {
                    Text("Re-scan", color = SatriaColors.Accent, fontSize = 13.sp)
                }
            }
        }
        HorizontalDivider(color = SatriaColors.SurfaceMid, thickness = 1.dp)

        if (selectedCategory == null) {
            // ── Category grid ────────────────────────────────────────────────
            CategoryStyleGrid(
                appCategories  = appCategories,
                categoryStyles = categoryStyles,
                allApps        = allApps,
                onSelectCategory = { selectedCategory = it },
                onResetStyle     = { cat -> scope.launch { vm.resetCategoryStyle(cat) } },
            )
        } else {
            // ── Style editor untuk kategori terpilih ─────────────────────────
            val cat = selectedCategory!!
            val style = categoryStyles[cat] ?: DEFAULT_CATEGORY_STYLES[cat] ?: CategoryStyle()

            // Tab selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SatriaColors.SurfaceMid),
            ) {
                listOf("Style", "Apps").forEachIndexed { i, label ->
                    TextButton(
                        onClick  = { tab = i },
                        modifier = Modifier
                            .weight(1f)
                            .background(if (tab == i) SatriaColors.SurfaceHigh else Color.Transparent),
                    ) {
                        Text(
                            label,
                            color      = if (tab == i) SatriaColors.TextPrimary else SatriaColors.TextSecondary,
                            fontWeight = if (tab == i) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }

            when (tab) {
                0 -> StyleEditor(
                    category = cat,
                    style    = style,
                    onSave   = { newStyle -> scope.launch { vm.saveCategoryStyle(cat, newStyle) } },
                )
                1 -> AppsInCategoryList(
                    category     = cat,
                    appCategories = appCategories,
                    allApps      = allApps,
                    onMoveApp    = { pkg, newCat ->
                        scope.launch { vm.overrideAppCategory(pkg, newCat) }
                    },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CategoryStyleGrid — grid semua kategori dengan preview mini
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CategoryStyleGrid(
    appCategories: Map<String, AppCategory>,
    categoryStyles: Map<AppCategory, CategoryStyle>,
    allApps: List<AppData>,
    onSelectCategory: (AppCategory) -> Unit,
    onResetStyle: (AppCategory) -> Unit,
) {
    val appCountByCategory = remember(appCategories) {
        AppCategory.values().associateWith { cat ->
            appCategories.count { it.value == cat }
        }
    }

    LazyVerticalGrid(
        columns        = GridCells.Fixed(2),
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement   = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(AppCategory.values()) { cat ->
            val style = categoryStyles[cat] ?: DEFAULT_CATEGORY_STYLES[cat] ?: CategoryStyle()
            val count = appCountByCategory[cat] ?: 0

            CategoryCard(
                category = cat,
                style    = style,
                appCount = count,
                onClick  = { onSelectCategory(cat) },
            )
        }
    }
}

@Composable
private fun CategoryCard(
    category: AppCategory,
    style: CategoryStyle,
    appCount: Int,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SatriaColors.SurfaceMid)
            .clickable { onClick() }
            .padding(14.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        // Preview icon — dummies menggunakan null bitmap (shape saja)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            repeat(3) {
                StyledAppIcon(
                    bitmap  = null,
                    style   = style.copy(
                        primaryColor = if (it == 0) style.primaryColor
                                       else style.primaryColor and 0x88FFFFFF or 0x44000000,
                    ),
                    sizeDp  = 26.dp,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text     = "${category.emoji} ${category.displayName}",
            color    = SatriaColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text     = "$appCount apps",
            color    = SatriaColors.TextTertiary,
            fontSize = 11.sp,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// StyleEditor — editor visual untuk satu kategori
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StyleEditor(
    category: AppCategory,
    style: CategoryStyle,
    onSave: (CategoryStyle) -> Unit,
) {
    var currentStyle by remember(category) { mutableStateOf(style) }

    // Auto-save setiap kali style berubah (500ms debounce via LaunchedEffect)
    LaunchedEffect(currentStyle) {
        kotlinx.coroutines.delay(300)
        onSave(currentStyle)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── Live Preview ────────────────────────────────────────────────────
        StylePreviewRow(style = currentStyle)

        // ── Shape ───────────────────────────────────────────────────────────
        SLabel("SHAPE")
        ShapePicker(
            selected  = currentStyle.shapeEnum(),
            onChange  = { currentStyle = currentStyle.copy(shape = it.name) },
        )

        // ── Effect ──────────────────────────────────────────────────────────
        SLabel("EFFECT")
        EffectPicker(
            selected = currentStyle.effectEnum(),
            onChange = { currentStyle = currentStyle.copy(effect = it.name) },
        )

        // ── Colors ──────────────────────────────────────────────────────────
        SLabel("COLORS")
        ColorPalettePicker(
            primaryColor   = Color(currentStyle.primaryColor),
            secondaryColor = Color(currentStyle.secondaryColor),
            onPrimary      = { currentStyle = currentStyle.copy(primaryColor = it.value.toLong()) },
            onSecondary    = { currentStyle = currentStyle.copy(secondaryColor = it.value.toLong()) },
        )

        // ── Border ─────────────────────────────────────────────────────────
        SLabel("BORDER THICKNESS  (${currentStyle.borderThickness.toInt()} dp)")
        Slider(
            value              = currentStyle.borderThickness,
            onValueChange      = { currentStyle = currentStyle.copy(borderThickness = it) },
            valueRange         = 0f..8f,
            steps              = 7,
            colors             = SliderDefaults.colors(
                thumbColor      = SatriaColors.Accent,
                activeTrackColor = SatriaColors.Accent,
                inactiveTrackColor = SatriaColors.SurfaceMid,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        // ── Opacity ────────────────────────────────────────────────────────
        SLabel("OPACITY  (${(currentStyle.opacity * 100).toInt()}%)")
        Slider(
            value              = currentStyle.opacity,
            onValueChange      = { currentStyle = currentStyle.copy(opacity = it) },
            valueRange         = 0.3f..1f,
            steps              = 13,
            colors             = SliderDefaults.colors(
                thumbColor      = SatriaColors.Accent,
                activeTrackColor = SatriaColors.Accent,
                inactiveTrackColor = SatriaColors.SurfaceMid,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(32.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// StylePreviewRow — 5 preview icons dengan style yang sedang diedit
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StylePreviewRow(style: CategoryStyle) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SatriaColors.SurfaceMid)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            repeat(5) {
                StyledAppIcon(
                    bitmap = null,
                    style  = style.copy(
                        primaryColor   = (style.primaryColor and 0x00FFFFFF) or
                            when (it) {
                                0 -> 0xFF000000
                                1 -> 0xCC000000
                                2 -> 0xFF000000
                                3 -> 0xAA000000
                                else -> 0xFF000000
                            },
                    ),
                    sizeDp = 48.dp,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ShapePicker
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ShapePicker(
    selected: IconShape,
    onChange: (IconShape) -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconShape.values().forEach { shape ->
            val active = shape == selected
            val dummyStyle = CategoryStyle(
                shape        = shape.name,
                effect       = "NONE",
                primaryColor = if (active) 0xFF27AE60 else 0xFF555555,
            )
            Column(
                modifier            = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) SatriaColors.Accent.copy(alpha = 0.15f) else SatriaColors.SurfaceMid)
                    .clickable { onChange(shape) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                StyledAppIcon(bitmap = null, style = dummyStyle, sizeDp = 32.dp)
                Text(
                    text      = shape.label,
                    color     = if (active) SatriaColors.Accent else SatriaColors.TextSecondary,
                    fontSize  = 9.sp,
                    textAlign = TextAlign.Center,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EffectPicker
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EffectPicker(
    selected: IconEffect,
    onChange: (IconEffect) -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconEffect.values().forEach { effect ->
            val active = effect == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) SatriaColors.Accent else SatriaColors.SurfaceMid)
                    .clickable { onChange(effect) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = effect.label,
                    color      = if (active) Color.White else SatriaColors.TextSecondary,
                    fontSize   = 12.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign  = TextAlign.Center,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ColorPalettePicker — preset palette + active indicator
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ColorPalettePicker(
    primaryColor: Color,
    secondaryColor: Color,
    onPrimary: (Color) -> Unit,
    onSecondary: (Color) -> Unit,
) {
    val palette = listOf(
        Color(0xFF27AE60) to Color(0xFF1A5C38),
        Color(0xFF1877F2) to Color(0xFF0A5DC9),
        Color(0xFFE53935) to Color(0xFFB71C1C),
        Color(0xFFFF6F00) to Color(0xFFE65100),
        Color(0xFF6A1B9A) to Color(0xFF4A148C),
        Color(0xFFAD1457) to Color(0xFF880E4F),
        Color(0xFF00897B) to Color(0xFF004D40),
        Color(0xFF1565C0) to Color(0xFF0D47A1),
        Color(0xFF546E7A) to Color(0xFF263238),
        Color(0xFFBF360C) to Color(0xFF870000),
        Color(0xFFF57C00) to Color(0xFFE65100),
        Color(0xFF37474F) to Color(0xFF212121),
    )

    LazyVerticalGrid(
        columns              = GridCells.Fixed(6),
        modifier             = Modifier.height(84.dp),
        verticalArrangement  = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        userScrollEnabled    = false,
    ) {
        items(palette) { (p, s) ->
            val selected = p == primaryColor
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(p)
                    .border(
                        width  = if (selected) 2.5.dp else 0.dp,
                        color  = Color.White,
                        shape  = CircleShape,
                    )
                    .clickable { onPrimary(p); onSecondary(s) },
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✓", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AppsInCategoryList — list app dalam kategori tertentu + opsi pindah kategori
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AppsInCategoryList(
    category: AppCategory,
    appCategories: Map<String, AppCategory>,
    allApps: List<AppData>,
    onMoveApp: (String, AppCategory) -> Unit,
) {
    val appsInCategory = remember(category, appCategories, allApps) {
        allApps.filter { appCategories[it.packageName] == category }
            .sortedBy { it.label.lowercase() }
    }

    var expandedPkg by remember { mutableStateOf<String?>(null) }

    if (appsInCategory.isEmpty()) {
        Box(
            modifier         = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "No apps in this category",
                color    = SatriaColors.TextTertiary,
                fontSize = 14.sp,
            )
        }
        return
    }

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(appsInCategory, key = { it.packageName }) { app ->
            val bitmap   = remember(app.packageName) { iconCache.get(app.packageName) }
            val expanded = expandedPkg == app.packageName

            Column {
                // App row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedPkg = if (expanded) null else app.packageName }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (bitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap             = bitmap,
                            contentDescription = null,
                            modifier           = Modifier.size(38.dp).clip(
                                androidx.compose.foundation.shape.RoundedCornerShape(9.dp)
                            ),
                        )
                    }
                    Text(
                        text     = app.label,
                        color    = SatriaColors.TextPrimary,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text     = if (expanded) "▲" else "▼",
                        color    = SatriaColors.TextTertiary,
                        fontSize = 12.sp,
                    )
                }

                // Expandable category picker
                AnimatedVisibility(visible = expanded) {
                    CategoryPickerRow(
                        currentCategory = category,
                        onSelect        = { newCat ->
                            onMoveApp(app.packageName, newCat)
                            expandedPkg = null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryPickerRow(
    currentCategory: AppCategory,
    onSelect: (AppCategory) -> Unit,
) {
    LazyRow(
        modifier       = Modifier.fillMaxWidth().padding(start = 16.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        items(AppCategory.values()) { cat ->
            val active = cat == currentCategory
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (active) SatriaColors.Accent else SatriaColors.SurfaceMid)
                    .clickable(enabled = !active) { onSelect(cat) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text     = "${cat.emoji} ${cat.displayName}",
                    color    = if (active) Color.White else SatriaColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────
@Composable private fun SLabel(text: String) =
    Text(
        text          = text,
        color         = SatriaColors.TextSecondary,
        fontSize      = 11.sp,
        fontWeight    = FontWeight.Medium,
        letterSpacing = 0.6.sp,
    )
