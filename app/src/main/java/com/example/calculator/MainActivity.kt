package com.example.calculator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Movie
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val PREFS_NAME = "calculator_user_prefs"
private const val KEY_THEME_MODE = "theme_mode"
private const val KEY_THEME_PALETTE = "theme_palette"
private const val KEY_BUTTON_SHAPE = "button_shape"
private const val KEY_LANGUAGE = "app_language"
private const val KEY_DEGREE_MODE = "degree_mode"
private const val KEY_AUTO_SCI_LANDSCAPE = "auto_sci_landscape"
private const val KEY_TOP_BAR_ORDER = "top_bar_order"
private const val KEY_GRID_LAYOUT = "grid_layout_v1"
private const val KEY_UI_ALPHA = "ui_alpha"
private const val KEY_LANDSCAPE_BG_MODE = "landscape_bg_mode"
private const val KEY_HAS_CUSTOM_BG = "has_custom_bg"
private const val CUSTOM_BG_FILENAME = "custom_background_media"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: CalculatorViewModel = viewModel()
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val prefs = remember(context) {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }

            // Load persisted settings synchronously before first composition
            remember(vm, prefs) {
                val savedMode = runCatching {
                    ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name) ?: ThemeMode.DARK.name)
                }.getOrDefault(ThemeMode.DARK)

                val savedPalette = runCatching {
                    ThemePalette.valueOf(prefs.getString(KEY_THEME_PALETTE, ThemePalette.OCEAN.name) ?: ThemePalette.OCEAN.name)
                }.getOrDefault(ThemePalette.OCEAN)

                val savedShape = runCatching {
                    ButtonShapeMode.valueOf(prefs.getString(KEY_BUTTON_SHAPE, ButtonShapeMode.ROUNDED.name) ?: ButtonShapeMode.ROUNDED.name)
                }.getOrDefault(ButtonShapeMode.ROUNDED)

                val savedLang = runCatching {
                    AppLanguage.valueOf(prefs.getString(KEY_LANGUAGE, AppLanguage.RU.name) ?: AppLanguage.RU.name)
                }.getOrDefault(AppLanguage.RU)

                val savedDeg = prefs.getBoolean(KEY_DEGREE_MODE, true)
                val savedAutoSci = prefs.getBoolean(KEY_AUTO_SCI_LANDSCAPE, true)
                val savedTopBar = vm.parseTopBarOrder(prefs.getString(KEY_TOP_BAR_ORDER, null))
                val savedGrid = vm.parseGridLayout(prefs.getString(KEY_GRID_LAYOUT, null))
                val savedAlpha = prefs.getFloat(KEY_UI_ALPHA, 0.72f)
                val savedLandBg = runCatching {
                    LandscapeBgMode.valueOf(prefs.getString(KEY_LANDSCAPE_BG_MODE, LandscapeBgMode.TRIPLE.name) ?: LandscapeBgMode.TRIPLE.name)
                }.getOrDefault(LandscapeBgMode.TRIPLE)
                val bgFileExists = File(context.filesDir, CUSTOM_BG_FILENAME).exists()
                val savedHasBg = prefs.getBoolean(KEY_HAS_CUSTOM_BG, false) && bgFileExists

                vm.loadSavedSettings(
                    savedThemeMode = savedMode,
                    savedPalette = savedPalette,
                    savedShape = savedShape,
                    savedLang = savedLang,
                    savedDegreeMode = savedDeg,
                    savedAutoSciOnLandscape = savedAutoSci,
                    savedTopBarOrder = savedTopBar,
                    savedGridLayout = savedGrid,
                    savedUiAlpha = savedAlpha,
                    savedLandscapeBgMode = savedLandBg,
                    savedHasCustomBg = savedHasBg
                )
                true
            }

            // Persist settings whenever any user preference or layout customization changes
            LaunchedEffect(
                vm.themeMode,
                vm.themePalette,
                vm.buttonShapeMode,
                vm.language,
                vm.isDegreeMode,
                vm.autoScientificOnLandscape,
                vm.topBarOrder,
                vm.gridLayout,
                vm.uiAlpha,
                vm.landscapeBgMode,
                vm.hasCustomBackground
            ) {
                prefs.edit()
                    .putString(KEY_THEME_MODE, vm.themeMode.name)
                    .putString(KEY_THEME_PALETTE, vm.themePalette.name)
                    .putString(KEY_BUTTON_SHAPE, vm.buttonShapeMode.name)
                    .putString(KEY_LANGUAGE, vm.language.name)
                    .putBoolean(KEY_DEGREE_MODE, vm.isDegreeMode)
                    .putBoolean(KEY_AUTO_SCI_LANDSCAPE, vm.autoScientificOnLandscape)
                    .putString(KEY_TOP_BAR_ORDER, vm.serializeTopBarOrder())
                    .putString(KEY_GRID_LAYOUT, vm.serializeGridLayout())
                    .putFloat(KEY_UI_ALPHA, vm.uiAlpha)
                    .putString(KEY_LANDSCAPE_BG_MODE, vm.landscapeBgMode.name)
                    .putBoolean(KEY_HAS_CUSTOM_BG, vm.hasCustomBackground)
                    .apply()
            }

            // System media picker for .png, .jpg, .jpeg, .gif
            val mediaPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                if (uri != null) {
                    scope.launch {
                        val copied = withContext(Dispatchers.IO) {
                            runCatching {
                                val targetFile = File(context.filesDir, CUSTOM_BG_FILENAME)
                                context.contentResolver.openInputStream(uri)?.use { input ->
                                    targetFile.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                targetFile.exists() && targetFile.length() > 0L
                            }.getOrDefault(false)
                        }
                        if (copied) {
                            vm.onCustomBackgroundSaved()
                        }
                    }
                }
            }

            val isDark = vm.themeMode != ThemeMode.LIGHT
            CalculatorAppTheme(
                darkTheme = isDark,
                palette = vm.themePalette
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Full-screen hardware-accelerated background view for custom PNG / JPEG / animated GIF
                    if (vm.hasCustomBackground) {
                        AndroidView(
                            factory = { ctx ->
                                CalculatorBackgroundView(ctx).apply {
                                    updateConfig(
                                        file = File(ctx.filesDir, CUSTOM_BG_FILENAME),
                                        mode = vm.landscapeBgMode,
                                        reloadToken = vm.backgroundReloadToken
                                    )
                                }
                            },
                            update = { view ->
                                view.updateConfig(
                                    file = File(view.context.filesDir, CUSTOM_BG_FILENAME),
                                    mode = vm.landscapeBgMode,
                                    reloadToken = vm.backgroundReloadToken
                                )
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        // Subtle readability scrim over custom wallpaper so display digits remain crisp
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    MaterialTheme.colorScheme.background.copy(
                                        alpha = if (isDark) 0.28f else 0.22f
                                    )
                                )
                        )
                    }

                    CalculatorScreen(
                        vm = vm,
                        isDarkTheme = isDark,
                        onPickBackgroundMedia = { mediaPickerLauncher.launch("image/*") },
                        onClearBackgroundMedia = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    runCatching { File(context.filesDir, CUSTOM_BG_FILENAME).delete() }
                                }
                                vm.onCustomBackgroundCleared()
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Hardware-accelerated View that renders static images (.png, .jpeg) and animated .gif files
 * full-screen without triggering any Jetpack Compose recompositions.
 */
class CalculatorBackgroundView(context: Context) : View(context) {

    private var currentToken: Int = -1
    private var landscapeMode: LandscapeBgMode = LandscapeBgMode.TRIPLE

    @Volatile
    private var movie: Movie? = null
    private var movieStart: Long = 0L

    @Volatile
    private var staticBitmap: Bitmap? = null

    @Volatile
    private var blurredBitmap: Bitmap? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33000000
        strokeWidth = 2f
    }
    private val drawMatrix = Matrix()
    private val executor = Executors.newSingleThreadExecutor()

    fun updateConfig(file: File, mode: LandscapeBgMode, reloadToken: Int) {
        var needsRedraw = false
        if (this.landscapeMode != mode) {
            this.landscapeMode = mode
            needsRedraw = true
        }
        if (this.currentToken != reloadToken) {
            this.currentToken = reloadToken
            loadMediaAsync(file)
        } else if (needsRedraw) {
            invalidate()
        }
    }

    private fun loadMediaAsync(file: File) {
        executor.execute {
            if (!file.exists() || file.length() == 0L) {
                movie = null
                staticBitmap = null
                blurredBitmap = null
                postInvalidate()
                return@execute
            }

            try {
                val isGif = file.inputStream().use { ins ->
                    val header = ByteArray(6)
                    val read = ins.read(header)
                    read == 6 && (String(header, Charsets.US_ASCII).startsWith("GIF87a") ||
                            String(header, Charsets.US_ASCII).startsWith("GIF89a"))
                }

                var loadedMovie: Movie? = null
                if (isGif && file.length() <= 35L * 1024L * 1024L) {
                    val bytes = file.readBytes()
                    val m = Movie.decodeByteArray(bytes, 0, bytes.size)
                    if (m != null && m.width() > 0 && m.height() > 0 && m.duration() > 0) {
                        loadedMovie = m
                    }
                }

                // Always decode a downsampled static bitmap (used for static PNG/JPEG and for Blur Wings background)
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, boundsOpts)
                var sampleSize = 1
                val maxDim = max(boundsOpts.outWidth, boundsOpts.outHeight)
                while (maxDim / sampleSize > 1920) {
                    sampleSize *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val bmp = BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
                val blurBmp = if (bmp != null) createFastBlurredBitmap(bmp) else null

                movie = loadedMovie
                movieStart = 0L
                staticBitmap = bmp
                blurredBitmap = blurBmp
                postInvalidate()
            } catch (_: Exception) {
                // Keep previous valid state or clear safely
            }
        }
    }

    private fun createFastBlurredBitmap(src: Bitmap): Bitmap? {
        return try {
            val smallW = 64
            val smallH = max(1, (src.height.toFloat() / src.width.toFloat() * smallW).roundToInt()).coerceAtMost(128)
            val small = Bitmap.createScaledBitmap(src, smallW, smallH, true)
            val pixels = IntArray(smallW * smallH)
            val temp = IntArray(smallW * smallH)
            small.getPixels(pixels, 0, smallW, 0, 0, smallW, smallH)

            // 2-pass fast box blur on downscaled 64px bitmap for smooth ambient wings
            repeat(2) {
                for (y in 1 until smallH - 1) {
                    for (x in 1 until smallW - 1) {
                        var r = 0
                        var g = 0
                        var b = 0
                        for (dy in -1..1) {
                            for (dx in -1..1) {
                                val c = pixels[(y + dy) * smallW + (x + dx)]
                                r += (c shr 16) and 0xFF
                                g += (c shr 8) and 0xFF
                                b += c and 0xFF
                            }
                        }
                        temp[y * smallW + x] = (0xFF shl 24) or ((r / 9) shl 16) or ((g / 9) shl 8) or (b / 9)
                    }
                }
                System.arraycopy(temp, 0, pixels, 0, pixels.size)
            }
            val out = Bitmap.createBitmap(smallW, smallH, Bitmap.Config.ARGB_8888)
            out.setPixels(pixels, 0, smallW, 0, 0, smallW, smallH)
            out
        } catch (_: Exception) {
            src
        }
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val isLandscape = w > h
        val activeMovie = movie
        val activeBmp = staticBitmap

        if (activeMovie != null && activeMovie.width() > 0 && activeMovie.height() > 0) {
            val now = SystemClock.uptimeMillis()
            if (movieStart == 0L) movieStart = now
            val dur = activeMovie.duration().coerceAtLeast(1)
            val relTime = ((now - movieStart) % dur).toInt()
            activeMovie.setTime(relTime)

            if (!isLandscape || landscapeMode == LandscapeBgMode.CROP) {
                drawMovieCenterCrop(canvas, activeMovie, 0f, 0f, w, h)
            } else if (landscapeMode == LandscapeBgMode.TRIPLE) {
                val tileW = w / 3f
                for (i in 0 until 3) {
                    val left = i * tileW
                    val right = left + tileW
                    drawMovieCenterCrop(canvas, activeMovie, left, 0f, right, h)
                    if (i > 0) {
                        canvas.drawLine(left, 0f, left, h, dividerPaint)
                    }
                }
            } else {
                // BLUR_WINGS: blurred side wings + uncropped full-height center GIF
                blurredBitmap?.let { drawBitmapCenterCrop(canvas, it, 0f, 0f, w, h) }
                drawMovieFitCenter(canvas, activeMovie, 0f, 0f, w, h)
            }

            postInvalidateOnAnimation()
        } else if (activeBmp != null && !activeBmp.isRecycled) {
            if (!isLandscape || landscapeMode == LandscapeBgMode.CROP) {
                drawBitmapCenterCrop(canvas, activeBmp, 0f, 0f, w, h)
            } else if (landscapeMode == LandscapeBgMode.TRIPLE) {
                val tileW = w / 3f
                for (i in 0 until 3) {
                    val left = i * tileW
                    val right = left + tileW
                    drawBitmapCenterCrop(canvas, activeBmp, left, 0f, right, h)
                    if (i > 0) {
                        canvas.drawLine(left, 0f, left, h, dividerPaint)
                    }
                }
            } else {
                // BLUR_WINGS: blurred side wings + uncropped full-height center image
                (blurredBitmap ?: activeBmp).let { drawBitmapCenterCrop(canvas, it, 0f, 0f, w, h) }
                drawBitmapFitCenter(canvas, activeBmp, 0f, 0f, w, h)
            }
        }
    }

    private fun drawMovieCenterCrop(
        canvas: android.graphics.Canvas,
        m: Movie,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ) {
        val targetW = right - left
        val targetH = bottom - top
        val mw = m.width().toFloat()
        val mh = m.height().toFloat()
        val scale = max(targetW / mw, targetH / mh)
        val dx = left + (targetW - mw * scale) * 0.5f
        val dy = top + (targetH - mh * scale) * 0.5f

        canvas.save()
        canvas.clipRect(left, top, right, bottom)
        canvas.translate(dx, dy)
        canvas.scale(scale, scale)
        m.draw(canvas, 0f, 0f)
        canvas.restore()
    }

    private fun drawMovieFitCenter(
        canvas: android.graphics.Canvas,
        m: Movie,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ) {
        val targetW = right - left
        val targetH = bottom - top
        val mw = m.width().toFloat()
        val mh = m.height().toFloat()
        val scale = min(targetW / mw, targetH / mh)
        val dx = left + (targetW - mw * scale) * 0.5f
        val dy = top + (targetH - mh * scale) * 0.5f

        canvas.save()
        canvas.clipRect(left, top, right, bottom)
        canvas.translate(dx, dy)
        canvas.scale(scale, scale)
        m.draw(canvas, 0f, 0f)
        canvas.restore()
    }

    private fun drawBitmapCenterCrop(
        canvas: android.graphics.Canvas,
        bmp: Bitmap,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ) {
        val targetW = right - left
        val targetH = bottom - top
        val bw = bmp.width.toFloat()
        val bh = bmp.height.toFloat()
        val scale = max(targetW / bw, targetH / bh)
        val dx = left + (targetW - bw * scale) * 0.5f
        val dy = top + (targetH - bh * scale) * 0.5f

        drawMatrix.reset()
        drawMatrix.postScale(scale, scale)
        drawMatrix.postTranslate(dx, dy)

        canvas.save()
        canvas.clipRect(left, top, right, bottom)
        canvas.drawBitmap(bmp, drawMatrix, paint)
        canvas.restore()
    }

    private fun drawBitmapFitCenter(
        canvas: android.graphics.Canvas,
        bmp: Bitmap,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ) {
        val targetW = right - left
        val targetH = bottom - top
        val bw = bmp.width.toFloat()
        val bh = bmp.height.toFloat()
        val scale = min(targetW / bw, targetH / bh)
        val dx = left + (targetW - bw * scale) * 0.5f
        val dy = top + (targetH - bh * scale) * 0.5f

        drawMatrix.reset()
        drawMatrix.postScale(scale, scale)
        drawMatrix.postTranslate(dx, dy)

        canvas.save()
        canvas.clipRect(RectF(dx, dy, dx + bw * scale, dy + bh * scale))
        canvas.drawBitmap(bmp, drawMatrix, paint)
        canvas.restore()
    }
}

@Composable
fun CalculatorAppTheme(
    darkTheme: Boolean,
    palette: ThemePalette,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        when (palette) {
            ThemePalette.OCEAN -> darkColorScheme(
                primary = Color(0xFF38BDF8),
                onPrimary = Color(0xFF00263A),
                primaryContainer = Color(0xFF0284C7),
                onPrimaryContainer = Color(0xFFE0F2FE),
                secondaryContainer = Color(0xFF1E293B),
                onSecondaryContainer = Color(0xFFE2E8F0),
                tertiaryContainer = Color(0xFF283548),
                onTertiaryContainer = Color(0xFF7DD3FC),
                errorContainer = Color(0xFF7F1D1D),
                onErrorContainer = Color(0xFFFECACA),
                background = Color(0xFF0B1120),
                onBackground = Color(0xFFF8FAFC),
                surface = Color(0xFF111827),
                onSurface = Color(0xFFF8FAFC),
                surfaceVariant = Color(0xFF1E293B),
                onSurfaceVariant = Color(0xFF94A3B8)
            )
            ThemePalette.EMERALD -> darkColorScheme(
                primary = Color(0xFF34D399),
                onPrimary = Color(0xFF022C22),
                primaryContainer = Color(0xFF059669),
                onPrimaryContainer = Color(0xFFD1FAE5),
                secondaryContainer = Color(0xFF142924),
                onSecondaryContainer = Color(0xFFECFDF5),
                tertiaryContainer = Color(0xFF1F3A33),
                onTertiaryContainer = Color(0xFF6EE7B7),
                errorContainer = Color(0xFF7F1D1D),
                onErrorContainer = Color(0xFFFECACA),
                background = Color(0xFF071512),
                onBackground = Color(0xFFF0FDF4),
                surface = Color(0xFF0D1F1B),
                onSurface = Color(0xFFF0FDF4),
                surfaceVariant = Color(0xFF162E28),
                onSurfaceVariant = Color(0xFF99B8AF)
            )
            ThemePalette.AMETHYST -> darkColorScheme(
                primary = Color(0xFFC084FC),
                onPrimary = Color(0xFF2E1065),
                primaryContainer = Color(0xFF7C3AED),
                onPrimaryContainer = Color(0xFFF3E8FF),
                secondaryContainer = Color(0xFF231B38),
                onSecondaryContainer = Color(0xFFF5F3FF),
                tertiaryContainer = Color(0xFF342654),
                onTertiaryContainer = Color(0xFFD8B4FE),
                errorContainer = Color(0xFF7F1D1D),
                onErrorContainer = Color(0xFFFECACA),
                background = Color(0xFF0F0A1C),
                onBackground = Color(0xFFFAF5FF),
                surface = Color(0xFF17112B),
                onSurface = Color(0xFFFAF5FF),
                surfaceVariant = Color(0xFF251C42),
                onSurfaceVariant = Color(0xFFB5A8D1)
            )
            ThemePalette.AMBER -> darkColorScheme(
                primary = Color(0xFFFBBF24),
                onPrimary = Color(0xFF3F2305),
                primaryContainer = Color(0xFFD97706),
                onPrimaryContainer = Color(0xFFFEF3C7),
                secondaryContainer = Color(0xFF292116),
                onSecondaryContainer = Color(0xFFFFFBEB),
                tertiaryContainer = Color(0xFF3D301D),
                onTertiaryContainer = Color(0xFFFCD34D),
                errorContainer = Color(0xFF7F1D1D),
                onErrorContainer = Color(0xFFFECACA),
                background = Color(0xFF14100A),
                onBackground = Color(0xFFFFFBEB),
                surface = Color(0xFF1F1910),
                onSurface = Color(0xFFFFFBEB),
                surfaceVariant = Color(0xFF2E2418),
                onSurfaceVariant = Color(0xFFC4B59B)
            )
            ThemePalette.ROSE -> darkColorScheme(
                primary = Color(0xFFFB7185),
                onPrimary = Color(0xFF4C0519),
                primaryContainer = Color(0xFFE11D48),
                onPrimaryContainer = Color(0xFFFFE4E6),
                secondaryContainer = Color(0xFF2D1922),
                onSecondaryContainer = Color(0xFFFFF1F2),
                tertiaryContainer = Color(0xFF422230),
                onTertiaryContainer = Color(0xFFFDA4AF),
                errorContainer = Color(0xFF7F1D1D),
                onErrorContainer = Color(0xFFFECACA),
                background = Color(0xFF170B10),
                onBackground = Color(0xFFFFF1F2),
                surface = Color(0xFF221118),
                onSurface = Color(0xFFFFF1F2),
                surfaceVariant = Color(0xFF331B26),
                onSurfaceVariant = Color(0xFFC9A3B0)
            )
        }
    } else {
        when (palette) {
            ThemePalette.OCEAN -> lightColorScheme(
                primary = Color(0xFF0284C7),
                onPrimary = Color.White,
                primaryContainer = Color(0xFF0284C7),
                onPrimaryContainer = Color.White,
                secondaryContainer = Color(0xFFE2E8F0),
                onSecondaryContainer = Color(0xFF0F172A),
                tertiaryContainer = Color(0xFFBAE6FD),
                onTertiaryContainer = Color(0xFF0369A1),
                errorContainer = Color(0xFFFEE2E2),
                onErrorContainer = Color(0xFFB91C1C),
                background = Color(0xFFF1F5F9),
                onBackground = Color(0xFF0F172A),
                surface = Color.White,
                onSurface = Color(0xFF0F172A),
                surfaceVariant = Color(0xFFE2E8F0),
                onSurfaceVariant = Color(0xFF475569)
            )
            ThemePalette.EMERALD -> lightColorScheme(
                primary = Color(0xFF059669),
                onPrimary = Color.White,
                primaryContainer = Color(0xFF059669),
                onPrimaryContainer = Color.White,
                secondaryContainer = Color(0xFFD1FAE5),
                onSecondaryContainer = Color(0xFF064E3B),
                tertiaryContainer = Color(0xFFA7F3D0),
                onTertiaryContainer = Color(0xFF047857),
                errorContainer = Color(0xFFFEE2E2),
                onErrorContainer = Color(0xFFB91C1C),
                background = Color(0xFFECFDF5),
                onBackground = Color(0xFF064E3B),
                surface = Color.White,
                onSurface = Color(0xFF064E3B),
                surfaceVariant = Color(0xFFD1FAE5),
                onSurfaceVariant = Color(0xFF047857)
            )
            ThemePalette.AMETHYST -> lightColorScheme(
                primary = Color(0xFF7C3AED),
                onPrimary = Color.White,
                primaryContainer = Color(0xFF7C3AED),
                onPrimaryContainer = Color.White,
                secondaryContainer = Color(0xFFEDE9FE),
                onSecondaryContainer = Color(0xFF2E1065),
                tertiaryContainer = Color(0xFFDDD6FE),
                onTertiaryContainer = Color(0xFF5B21B6),
                errorContainer = Color(0xFFFEE2E2),
                onErrorContainer = Color(0xFFB91C1C),
                background = Color(0xFFF5F3FF),
                onBackground = Color(0xFF2E1065),
                surface = Color.White,
                onSurface = Color(0xFF2E1065),
                surfaceVariant = Color(0xFFEDE9FE),
                onSurfaceVariant = Color(0xFF5B21B6)
            )
            ThemePalette.AMBER -> lightColorScheme(
                primary = Color(0xFFD97706),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFD97706),
                onPrimaryContainer = Color.White,
                secondaryContainer = Color(0xFFFEF3C7),
                onSecondaryContainer = Color(0xFF451A03),
                tertiaryContainer = Color(0xFFFDE68A),
                onTertiaryContainer = Color(0xFFB45309),
                errorContainer = Color(0xFFFEE2E2),
                onErrorContainer = Color(0xFFB91C1C),
                background = Color(0xFFFFFBEB),
                onBackground = Color(0xFF451A03),
                surface = Color.White,
                onSurface = Color(0xFF451A03),
                surfaceVariant = Color(0xFFFEF3C7),
                onSurfaceVariant = Color(0xFF92400E)
            )
            ThemePalette.ROSE -> lightColorScheme(
                primary = Color(0xFFE11D48),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFE11D48),
                onPrimaryContainer = Color.White,
                secondaryContainer = Color(0xFFFFE4E6),
                onSecondaryContainer = Color(0xFF4C0519),
                tertiaryContainer = Color(0xFFFECDD3),
                onTertiaryContainer = Color(0xFFBE123C),
                errorContainer = Color(0xFFFEE2E2),
                onErrorContainer = Color(0xFFB91C1C),
                background = Color(0xFFFFF1F2),
                onBackground = Color(0xFF4C0519),
                surface = Color.White,
                onSurface = Color(0xFF4C0519),
                surfaceVariant = Color(0xFFFFE4E6),
                onSurfaceVariant = Color(0xFF9F1239)
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

enum class CalcButtonKind {
    NUMBER, OPERATOR, ACTION, SCIENTIFIC, CLEAR, EQUALS
}

data class CalcButtonSpec(
    val id: String,
    val label: String,
    val kind: CalcButtonKind,
    val onClick: () -> Unit
)

fun resolveButtonSpec(id: String, vm: CalculatorViewModel): CalcButtonSpec {
    return when (id) {
        "DEG" -> CalcButtonSpec(id, if (vm.isDegreeMode) "DEG" else "RAD", CalcButtonKind.SCIENTIFIC) { vm.toggleDegreeMode() }
        "sin" -> CalcButtonSpec(id, "sin", CalcButtonKind.SCIENTIFIC) { vm.onFunction("sin") }
        "cos" -> CalcButtonSpec(id, "cos", CalcButtonKind.SCIENTIFIC) { vm.onFunction("cos") }
        "tan" -> CalcButtonSpec(id, "tan", CalcButtonKind.SCIENTIFIC) { vm.onFunction("tan") }
        "ln" -> CalcButtonSpec(id, "ln", CalcButtonKind.SCIENTIFIC) { vm.onFunction("ln") }
        "sqrt" -> CalcButtonSpec(id, "√x", CalcButtonKind.SCIENTIFIC) { vm.onFunction("sqrt") }
        "pow" -> CalcButtonSpec(id, "xʸ", CalcButtonKind.SCIENTIFIC) { vm.onPower() }
        "log" -> CalcButtonSpec(id, "log", CalcButtonKind.SCIENTIFIC) { vm.onFunction("log") }
        "fact" -> CalcButtonSpec(id, "n!", CalcButtonKind.SCIENTIFIC) { vm.onFactorial() }
        "pi" -> CalcButtonSpec(id, "π", CalcButtonKind.SCIENTIFIC) { vm.onConstant("π") }
        "AC" -> CalcButtonSpec(id, "AC", CalcButtonKind.CLEAR) { vm.onClear() }
        "paren" -> CalcButtonSpec(id, "( )", CalcButtonKind.ACTION) { vm.onParentheses() }
        "percent" -> CalcButtonSpec(id, "%", CalcButtonKind.ACTION) { vm.onPercent() }
        "div" -> CalcButtonSpec(id, "÷", CalcButtonKind.OPERATOR) { vm.onOperator("÷") }
        "mul" -> CalcButtonSpec(id, "×", CalcButtonKind.OPERATOR) { vm.onOperator("×") }
        "sub" -> CalcButtonSpec(id, "−", CalcButtonKind.OPERATOR) { vm.onOperator("−") }
        "add" -> CalcButtonSpec(id, "+", CalcButtonKind.OPERATOR) { vm.onOperator("+") }
        "dot" -> CalcButtonSpec(id, ".", CalcButtonKind.NUMBER) { vm.onDecimalPoint() }
        "eq" -> CalcButtonSpec(id, "=", CalcButtonKind.EQUALS) { vm.onEquals() }
        else -> CalcButtonSpec(id, id, CalcButtonKind.NUMBER) { vm.onDigit(id) }
    }
}

@Composable
fun CalculatorScreen(
    vm: CalculatorViewModel,
    isDarkTheme: Boolean,
    onPickBackgroundMedia: () -> Unit,
    onClearBackgroundMedia: () -> Unit
) {
    var showHistory by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val isRu = vm.language == AppLanguage.RU

    val historyChipIndex = vm.topBarOrder.indexOf(TopBarItem.HISTORY).coerceIn(0, 2)
    val settingsChipIndex = vm.topBarOrder.indexOf(TopBarItem.SETTINGS).coerceIn(0, 2)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        val screenW = maxWidth
        val screenH = maxHeight
        val isLandscape = screenW > screenH

        SideEffect {
            vm.onOrientationChanged(isLandscape)
        }

        val (targetStartRow, targetStartCol) = vm.computeActiveGridBounds(vm.isScientificMode)

        val startRowState = animateFloatAsState(
            targetValue = targetStartRow.toFloat(),
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            label = "animatedStartRow"
        )
        val startColState = animateFloatAsState(
            targetValue = targetStartCol.toFloat(),
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            label = "animatedStartCol"
        )

        if (!isLandscape) {
            val fixedKeypadHeight: Dp = minOf(screenH * 0.58f, screenW * 1.20f)

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                CalculatorTopBar(
                    vm = vm,
                    isRu = isRu,
                    showHistory = showHistory,
                    showSettings = showSettings,
                    onToggleHistory = {
                        showHistory = !showHistory
                        if (showHistory) showSettings = false
                    },
                    onToggleSettings = {
                        showSettings = !showSettings
                        if (showSettings) showHistory = false
                    }
                )

                CustomizationBanner(
                    vm = vm,
                    isRu = isRu,
                    chipSlotIndex = settingsChipIndex
                )

                HistoryDropdownPanel(
                    vm = vm,
                    isRu = isRu,
                    visible = showHistory && !vm.isCustomizationMode,
                    chipSlotIndex = historyChipIndex,
                    maxCardHeight = 190.dp,
                    onSelectHistory = { item ->
                        vm.onHistoryItemSelected(item)
                        showHistory = false
                    }
                )

                SettingsDropdownPanel(
                    vm = vm,
                    isRu = isRu,
                    isDarkTheme = isDarkTheme,
                    visible = showSettings && !vm.isCustomizationMode,
                    chipSlotIndex = settingsChipIndex,
                    maxCardHeight = 290.dp,
                    onPickBackgroundMedia = onPickBackgroundMedia,
                    onClearBackgroundMedia = onClearBackgroundMedia,
                    onStartCustomization = {
                        showSettings = false
                        showHistory = false
                        vm.toggleCustomizationMode()
                    }
                )

                CalculatorDisplaySection(
                    vm = vm,
                    isRu = isRu,
                    isCompact = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 4.dp)
                )

                AnimatedKeypadGrid(
                    vm = vm,
                    startRowState = startRowState,
                    startColState = startColState,
                    isDarkTheme = isDarkTheme,
                    isLandscape = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(fixedKeypadHeight)
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    CalculatorTopBar(
                        vm = vm,
                        isRu = isRu,
                        showHistory = showHistory,
                        showSettings = showSettings,
                        onToggleHistory = {
                            showHistory = !showHistory
                            if (showHistory) showSettings = false
                        },
                        onToggleSettings = {
                            showSettings = !showSettings
                            if (showSettings) showHistory = false
                        }
                    )

                    CustomizationBanner(
                        vm = vm,
                        isRu = isRu,
                        chipSlotIndex = settingsChipIndex
                    )

                    HistoryDropdownPanel(
                        vm = vm,
                        isRu = isRu,
                        visible = showHistory && !vm.isCustomizationMode,
                        chipSlotIndex = historyChipIndex,
                        maxCardHeight = screenH * 0.72f,
                        onSelectHistory = { item ->
                            vm.onHistoryItemSelected(item)
                            showHistory = false
                        }
                    )

                    SettingsDropdownPanel(
                        vm = vm,
                        isRu = isRu,
                        isDarkTheme = isDarkTheme,
                        visible = showSettings && !vm.isCustomizationMode,
                        chipSlotIndex = settingsChipIndex,
                        maxCardHeight = screenH * 0.82f,
                        onPickBackgroundMedia = onPickBackgroundMedia,
                        onClearBackgroundMedia = onClearBackgroundMedia,
                        onStartCustomization = {
                            showSettings = false
                            showHistory = false
                            vm.toggleCustomizationMode()
                        }
                    )

                    CalculatorDisplaySection(
                        vm = vm,
                        isRu = isRu,
                        isCompact = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 2.dp)
                    )
                }

                AnimatedKeypadGrid(
                    vm = vm,
                    startRowState = startRowState,
                    startColState = startColState,
                    isDarkTheme = isDarkTheme,
                    isLandscape = true,
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
fun CalculatorTopBar(
    vm: CalculatorViewModel,
    isRu: Boolean,
    showHistory: Boolean,
    showSettings: Boolean,
    onToggleHistory: () -> Unit,
    onToggleSettings: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val customMode = vm.isCustomizationMode
    val chipAlpha = vm.uiAlpha

    val infiniteTransition = rememberInfiniteTransition(label = "topBarFloat")
    val floatBob by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "topBarBob"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for ((index, item) in vm.topBarOrder.withIndex()) {
            val isSelectedForSwap = customMode && vm.selectedTopBarIndex == index
            val liftScale by animateFloatAsState(
                targetValue = if (isSelectedForSwap) 1.07f else 1f,
                animationSpec = tween(durationMillis = 160),
                label = "chipLiftScale"
            )
            val liftOffsetY by animateFloatAsState(
                targetValue = if (isSelectedForSwap) -4f else 0f,
                animationSpec = tween(durationMillis = 160),
                label = "chipLiftY"
            )

            val chipModifier = Modifier
                .weight(1f)
                .zIndex(if (isSelectedForSwap) 5f else 0f)
                .graphicsLayer {
                    scaleX = liftScale
                    scaleY = liftScale
                    translationY = (liftOffsetY + if (isSelectedForSwap) floatBob else 0f).dp.toPx()
                    shadowElevation = if (isSelectedForSwap) 10.dp.toPx() else 0f
                }
                .then(
                    if (customMode) {
                        Modifier.border(
                            width = if (isSelectedForSwap) 2.dp else 1.dp,
                            color = if (isSelectedForSwap)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(50)
                        )
                    } else {
                        Modifier
                    }
                )

            when (item) {
                TopBarItem.HISTORY -> {
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (customMode) {
                                vm.onTopBarChipTappedInCustomMode(index)
                            } else {
                                onToggleHistory()
                            }
                        },
                        modifier = chipModifier,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = chipAlpha),
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        val prefix = if (customMode) "⇄ " else ""
                        val histText = if (isRu) {
                            if (showHistory && !customMode) "Скрыть" else "${prefix}История (${vm.history.size})"
                        } else {
                            if (showHistory && !customMode) "Hide" else "${prefix}History (${vm.history.size})"
                        }
                        Text(
                            text = histText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                TopBarItem.MODE -> {
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (customMode) {
                                vm.onTopBarChipTappedInCustomMode(index)
                            } else {
                                vm.toggleScientificMode()
                            }
                        },
                        modifier = chipModifier,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = (if (vm.isScientificMode)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.secondaryContainer).copy(alpha = chipAlpha),
                            contentColor = if (vm.isScientificMode)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        val prefix = if (customMode) "⇄ " else ""
                        val sciText = if (isRu) {
                            if (vm.isScientificMode) "${prefix}f(x) Обычн." else "${prefix}f(x) Инж."
                        } else {
                            if (vm.isScientificMode) "${prefix}f(x) Basic" else "${prefix}f(x) Sci"
                        }
                        Text(
                            text = sciText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                TopBarItem.SETTINGS -> {
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (customMode) {
                                vm.onTopBarChipTappedInCustomMode(index)
                            } else {
                                onToggleSettings()
                            }
                        },
                        modifier = chipModifier,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = chipAlpha),
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        val prefix = if (customMode) "⇄ " else ""
                        val settingsText = if (isRu) {
                            if (showSettings && !customMode) "⚙ Скрыть" else "${prefix}⚙ Настройки"
                        } else {
                            if (showSettings && !customMode) "⚙ Hide" else "${prefix}⚙ Settings"
                        }
                        Text(
                            text = settingsText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomizationBanner(
    vm: CalculatorViewModel,
    isRu: Boolean,
    chipSlotIndex: Int
) {
    val pivotX = when (chipSlotIndex) {
        0 -> 0.16f
        1 -> 0.50f
        else -> 0.84f
    }

    val enterAnim = remember(chipSlotIndex) {
        scaleIn(
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            initialScale = 0.18f,
            transformOrigin = TransformOrigin(pivotX, 0f)
        ) + expandVertically(
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            expandFrom = Alignment.Top
        ) + fadeIn(
            animationSpec = tween(durationMillis = 170)
        )
    }

    val exitAnim = remember(chipSlotIndex) {
        scaleOut(
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            targetScale = 0.18f,
            transformOrigin = TransformOrigin(pivotX, 0f)
        ) + shrinkVertically(
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            shrinkTowards = Alignment.Top
        ) + fadeOut(
            animationSpec = tween(durationMillis = 150)
        )
    }

    val cardAlpha = (vm.uiAlpha + 0.16f).coerceIn(0.30f, 0.96f)

    AnimatedVisibility(
        visible = vm.isCustomizationMode,
        enter = enterAnim,
        exit = exitAnim
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = cardAlpha)
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isRu)
                        "Режим кастомизации: нажмите 2 элемента для обмена местами, × — удалить кнопку"
                    else
                        "Customization: tap 2 items to swap places, × to delete a button",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsOptionChip(
                        label = if (isRu) {
                            if (vm.isScientificMode) "Вид: Инж." else "Вид: Обычн."
                        } else {
                            if (vm.isScientificMode) "View: Sci" else "View: Basic"
                        },
                        selected = vm.isScientificMode,
                        modifier = Modifier.weight(1f),
                        onClick = { vm.toggleScientificMode() }
                    )
                    SettingsOptionChip(
                        label = if (isRu) "↺ Сбросить" else "↺ Reset",
                        selected = false,
                        modifier = Modifier.weight(1f),
                        onClick = { vm.resetCustomizations() }
                    )
                    SettingsOptionChip(
                        label = if (isRu) "✓ Готово" else "✓ Done",
                        selected = true,
                        modifier = Modifier.weight(1f),
                        onClick = { vm.exitCustomizationMode() }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryDropdownPanel(
    vm: CalculatorViewModel,
    isRu: Boolean,
    visible: Boolean,
    chipSlotIndex: Int,
    maxCardHeight: Dp,
    onSelectHistory: (HistoryItem) -> Unit
) {
    val pivotX = when (chipSlotIndex) {
        0 -> 0.16f
        1 -> 0.50f
        else -> 0.84f
    }

    val enterAnim = remember(chipSlotIndex) {
        scaleIn(
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            initialScale = 0.18f,
            transformOrigin = TransformOrigin(pivotX, 0f)
        ) + expandVertically(
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            expandFrom = Alignment.Top
        ) + fadeIn(
            animationSpec = tween(durationMillis = 170)
        )
    }

    val exitAnim = remember(chipSlotIndex) {
        scaleOut(
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            targetScale = 0.18f,
            transformOrigin = TransformOrigin(pivotX, 0f)
        ) + shrinkVertically(
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            shrinkTowards = Alignment.Top
        ) + fadeOut(
            animationSpec = tween(durationMillis = 150)
        )
    }

    val cardAlpha = (vm.uiAlpha + 0.14f).coerceIn(0.28f, 0.95f)

    AnimatedVisibility(
        visible = visible,
        enter = enterAnim,
        exit = exitAnim
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp, max = maxCardHeight)
                .padding(top = 6.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = cardAlpha)
            )
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRu) "Журнал вычислений" else "Calculation History",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (vm.history.isNotEmpty()) {
                        Text(
                            text = if (isRu) "Очистить" else "Clear",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { vm.clearHistory() }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                if (vm.history.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRu) "Пока нет вычислений" else "No calculations yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(vm.history) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelectHistory(item) }
                                    .padding(vertical = 5.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.expression,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "= ${item.result}",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDropdownPanel(
    vm: CalculatorViewModel,
    isRu: Boolean,
    isDarkTheme: Boolean,
    visible: Boolean,
    chipSlotIndex: Int,
    maxCardHeight: Dp,
    onPickBackgroundMedia: () -> Unit,
    onClearBackgroundMedia: () -> Unit,
    onStartCustomization: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val pivotX = when (chipSlotIndex) {
        0 -> 0.16f
        1 -> 0.50f
        else -> 0.84f
    }

    val enterAnim = remember(chipSlotIndex) {
        scaleIn(
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            initialScale = 0.18f,
            transformOrigin = TransformOrigin(pivotX, 0f)
        ) + expandVertically(
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            expandFrom = Alignment.Top
        ) + fadeIn(
            animationSpec = tween(durationMillis = 170)
        )
    }

    val exitAnim = remember(chipSlotIndex) {
        scaleOut(
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            targetScale = 0.18f,
            transformOrigin = TransformOrigin(pivotX, 0f)
        ) + shrinkVertically(
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            shrinkTowards = Alignment.Top
        ) + fadeOut(
            animationSpec = tween(durationMillis = 150)
        )
    }

    val cardAlpha = (vm.uiAlpha + 0.16f).coerceIn(0.32f, 0.96f)

    AnimatedVisibility(
        visible = visible,
        enter = enterAnim,
        exit = exitAnim
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxCardHeight)
                .padding(top = 6.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = cardAlpha)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 9.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                // 1. Compact Top Row: [Theme Icon | Language RU/EN | Auto-Sci On/Off]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = if (isRu) "Тема" else "Theme",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        AnimatedThemeIconButton(
                            isDark = isDarkTheme,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.toggleThemeMode()
                            }
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = if (isRu) "Язык" else "Language",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        AnimatedSingleModeButton(
                            currentText = if (isRu) "RU" else "EN",
                            isHighlighted = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.toggleLanguage()
                            }
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1.25f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = if (isRu) "Авто-инж." else "Auto-Sci",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        val autoSciLabel = if (vm.autoScientificOnLandscape) {
                            if (isRu) "Вкл" else "On"
                        } else {
                            if (isRu) "Выкл" else "Off"
                        }
                        AnimatedSingleModeButton(
                            currentText = autoSciLabel,
                            isHighlighted = vm.autoScientificOnLandscape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.toggleAutoScientificOnLandscape()
                            }
                        )
                    }
                }

                // 2. Visual Color Palette Swatches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRu) "Палитра" else "Palette",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val swatchColors = listOf(
                            Triple(ThemePalette.OCEAN, Color(0xFF38BDF8), Color(0xFF0284C7)),
                            Triple(ThemePalette.EMERALD, Color(0xFF34D399), Color(0xFF059669)),
                            Triple(ThemePalette.AMETHYST, Color(0xFFC084FC), Color(0xFF7C3AED)),
                            Triple(ThemePalette.AMBER, Color(0xFFFBBF24), Color(0xFFD97706)),
                            Triple(ThemePalette.ROSE, Color(0xFFFB7185), Color(0xFFE11D48))
                        )
                        for ((palette, topCol, botCol) in swatchColors) {
                            ColorPaletteSwatch(
                                topColor = topCol,
                                bottomColor = botCol,
                                selected = vm.themePalette == palette,
                                onClick = { vm.updateThemePalette(palette) }
                            )
                        }
                    }
                }

                // 3. UI Elements Opacity / Transparency Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isRu)
                            "Прозрачность: ${(vm.uiAlpha * 100).roundToInt()}%"
                        else
                            "Opacity: ${(vm.uiAlpha * 100).roundToInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(118.dp)
                    )
                    Slider(
                        value = vm.uiAlpha,
                        onValueChange = { vm.updateUiAlpha(it) },
                        valueRange = 0.15f..1.0f,
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp)
                    )
                }

                // 4. Custom Background Image / GIF & Landscape Rotation Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsOptionChip(
                        label = if (isRu) "🖼 Фон (PNG/JPG/GIF)" else "🖼 BG (PNG/JPG/GIF)",
                        selected = vm.hasCustomBackground,
                        modifier = Modifier.weight(1.35f),
                        onClick = onPickBackgroundMedia
                    )

                    val landModeLabel = when (vm.landscapeBgMode) {
                        LandscapeBgMode.TRIPLE -> if (isRu) "Гориз: 3× Триптих" else "Land: 3× Tile"
                        LandscapeBgMode.BLUR_WINGS -> if (isRu) "Гориз: Размытие" else "Land: Blur"
                        LandscapeBgMode.CROP -> if (isRu) "Гориз: Заполн." else "Land: Crop"
                    }
                    AnimatedSingleModeButton(
                        currentText = landModeLabel,
                        isHighlighted = vm.hasCustomBackground,
                        modifier = Modifier
                            .weight(1.25f)
                            .height(30.dp),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.toggleLandscapeBgMode()
                        }
                    )

                    if (vm.hasCustomBackground) {
                        SettingsOptionChip(
                            label = "✕",
                            selected = false,
                            modifier = Modifier.width(34.dp),
                            onClick = onClearBackgroundMedia
                        )
                    }
                }

                // 5. Button Shape (Round / Rounded / Square)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val shapes = if (isRu) {
                        listOf(
                            ButtonShapeMode.ROUND to "Круглые",
                            ButtonShapeMode.ROUNDED to "Скруглённые",
                            ButtonShapeMode.SQUARE to "Квадратные"
                        )
                    } else {
                        listOf(
                            ButtonShapeMode.ROUND to "Round",
                            ButtonShapeMode.ROUNDED to "Rounded",
                            ButtonShapeMode.SQUARE to "Square"
                        )
                    }
                    for ((shapeOption, label) in shapes) {
                        SettingsOptionChip(
                            label = label,
                            selected = vm.buttonShapeMode == shapeOption,
                            modifier = Modifier.weight(1f),
                            onClick = { vm.updateButtonShapeMode(shapeOption) }
                        )
                    }
                }

                // 6. Customization Mode Entry & Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsOptionChip(
                        label = if (isRu) "✎ Кастомизация" else "✎ Customize",
                        selected = vm.isCustomizationMode,
                        modifier = Modifier.weight(1.5f),
                        onClick = onStartCustomization
                    )
                    SettingsOptionChip(
                        label = if (isRu) "↺ Сброс" else "↺ Reset",
                        selected = false,
                        modifier = Modifier.weight(1f),
                        onClick = { vm.resetCustomizations() }
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedThemeIconButton(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val darkProgress by animateFloatAsState(
        targetValue = if (isDark) 1f else 0f,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "themeIconProgress"
    )

    val bgColor = lerp(
        Color(0xFFFFF7ED),
        Color(0xFF1E293B),
        darkProgress
    )
    val borderColor = lerp(
        Color(0xFFF59E0B).copy(alpha = 0.65f),
        Color(0xFF38BDF8).copy(alpha = 0.65f),
        darkProgress
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer {
                    rotationZ = darkProgress * 180f
                }
        ) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.5f
            val r = size.minDimension * 0.28f

            val orbColor = lerp(
                Color(0xFFF59E0B),
                Color(0xFF7DD3FC),
                darkProgress
            )

            val rayFactor = (1f - darkProgress).coerceIn(0f, 1f)
            if (rayFactor > 0.04f) {
                val innerR = r * 1.32f
                val outerR = r * (1.32f + 0.52f * rayFactor)
                for (i in 0 until 8) {
                    val angle = i * (Math.PI / 4.0)
                    val cosA = cos(angle).toFloat()
                    val sinA = sin(angle).toFloat()
                    drawLine(
                        color = orbColor.copy(alpha = rayFactor),
                        start = Offset(cx + cosA * innerR, cy + sinA * innerR),
                        end = Offset(cx + cosA * outerR, cy + sinA * outerR),
                        strokeWidth = 1.6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            drawCircle(
                color = orbColor,
                radius = r * (1f + 0.12f * darkProgress),
                center = Offset(cx, cy)
            )

            if (darkProgress > 0.04f) {
                val maskOffset = (1f - darkProgress) * r * 1.4f
                drawCircle(
                    color = bgColor,
                    radius = r * 0.92f,
                    center = Offset(
                        cx + r * 0.44f + maskOffset,
                        cy - r * 0.36f - maskOffset
                    )
                )

                drawCircle(
                    color = Color(0xFFE0F2FE).copy(alpha = darkProgress),
                    radius = 1.1.dp.toPx(),
                    center = Offset(cx - r * 1.15f, cy - r * 0.75f)
                )
                drawCircle(
                    color = Color(0xFFBAE6FD).copy(alpha = darkProgress * 0.85f),
                    radius = 0.9.dp.toPx(),
                    center = Offset(cx - r * 0.75f, cy + r * 1.05f)
                )
            }
        }
    }
}

@Composable
fun AnimatedSingleModeButton(
    currentText: String,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val highlightProgress by animateFloatAsState(
        targetValue = if (isHighlighted) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "modeBtnHighlight"
    )

    val bg = lerp(
        MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
        MaterialTheme.colorScheme.primaryContainer,
        highlightProgress
    )
    val fg = lerp(
        MaterialTheme.colorScheme.onSurface,
        MaterialTheme.colorScheme.onPrimaryContainer,
        highlightProgress
    )
    val borderCol = lerp(
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f),
        MaterialTheme.colorScheme.primary,
        highlightProgress
    )
    val dotColor = lerp(
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
        MaterialTheme.colorScheme.primary,
        highlightProgress
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.2.dp, borderCol, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            AnimatedContent(
                targetState = currentText,
                transitionSpec = {
                    (slideInVertically(animationSpec = tween(200)) { h -> h / 2 } +
                            fadeIn(animationSpec = tween(200)) +
                            scaleIn(initialScale = 0.85f, animationSpec = tween(200)))
                        .togetherWith(
                            slideOutVertically(animationSpec = tween(160)) { h -> -h / 2 } +
                                    fadeOut(animationSpec = tween(150))
                        )
                },
                label = "singleModeTextAnim"
            ) { label ->
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = fg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CalculatorDisplaySection(
    vm: CalculatorViewModel,
    isRu: Boolean,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End
    ) {
        val exprText = if (vm.expression.isEmpty()) "0" else vm.expression
        val exprFontSize = when {
            exprText.length > 22 -> if (isCompact) 20.sp else 24.sp
            exprText.length > 14 -> if (isCompact) 26.sp else 34.sp
            else -> if (isCompact) 34.sp else 44.sp
        }
        Text(
            text = exprText,
            fontSize = exprFontSize,
            lineHeight = exprFontSize * 1.12f,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(if (isCompact) 2.dp else 4.dp))

        val previewStr = vm.localizedPreview()
        Text(
            text = if (previewStr.isNotEmpty()) "= $previewStr" else "",
            fontSize = if (isCompact) 18.sp else 22.sp,
            fontWeight = FontWeight.Medium,
            color = if (vm.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompact) 24.dp else 28.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    vm.onToggleSign()
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isRu) "+/− Знак" else "+/− Sign",
                    fontSize = 13.sp
                )
            }

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    vm.onBackspace()
                },
                enabled = vm.expression.isNotEmpty() || vm.isError
            ) {
                Text(
                    text = "⌫",
                    fontSize = if (isCompact) 22.sp else 24.sp,
                    color = if (vm.expression.isNotEmpty() || vm.isError)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun AnimatedKeypadGrid(
    vm: CalculatorViewModel,
    startRowState: State<Float>,
    startColState: State<Float>,
    isDarkTheme: Boolean,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val shapeMode = vm.buttonShapeMode
    val customMode = vm.isCustomizationMode
    val selectedCell = vm.selectedGridIndex
    val gridIds = vm.gridLayout
    val uiAlpha = vm.uiAlpha

    val buttonShape: Shape = remember(shapeMode) {
        when (shapeMode) {
            ButtonShapeMode.ROUND -> CircleShape
            ButtonShapeMode.ROUNDED -> RoundedCornerShape(18.dp)
            ButtonShapeMode.SQUARE -> RoundedCornerShape(5.dp)
        }
    }

    val sciFont = if (isLandscape) 13.sp else 14.sp
    val mainOpFont = if (isLandscape) 20.sp else 24.sp
    val mainNumFont = if (isLandscape) 17.sp else 20.sp

    val (activeStartRow, activeStartCol) = vm.computeActiveGridBounds(vm.isScientificMode)

    Layout(
        modifier = modifier,
        content = {
            for (cellIdx in 0 until CalculatorViewModel.GRID_SIZE) {
                val r = cellIdx / CalculatorViewModel.GRID_COLS
                val c = cellIdx % CalculatorViewModel.GRID_COLS
                val isCellVisibleInMode = r >= activeStartRow && c >= activeStartCol
                val btnId = gridIds[cellIdx]
                val isSelected = customMode && selectedCell == cellIdx

                if (btnId != null) {
                    val spec = resolveButtonSpec(btnId, vm)
                    val baseFont = when (spec.kind) {
                        CalcButtonKind.OPERATOR, CalcButtonKind.EQUALS -> mainOpFont
                        CalcButtonKind.SCIENTIFIC -> sciFont
                        else -> mainNumFont
                    }
                    CalculatorKeyButton(
                        spec = spec,
                        shape = buttonShape,
                        shapeMode = shapeMode,
                        isDarkTheme = isDarkTheme,
                        uiAlpha = uiAlpha,
                        enabled = isCellVisibleInMode,
                        isCustomizationMode = customMode,
                        isSelectedForSwap = isSelected,
                        fontSizeOverride = baseFont,
                        onTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (customMode) {
                                vm.onGridCellTappedInCustomMode(cellIdx)
                            } else {
                                spec.onClick()
                            }
                        },
                        onDelete = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.deleteGridButtonAt(cellIdx)
                        }
                    )
                } else {
                    EmptyGridSlot(
                        shape = buttonShape,
                        visible = customMode && isCellVisibleInMode,
                        isSelected = isSelected,
                        onClick = {
                            if (customMode && isCellVisibleInMode) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.onGridCellTappedInCustomMode(cellIdx)
                            }
                        }
                    )
                }
            }
        }
    ) { measurables, constraints ->
        val startR = startRowState.value
        val startC = startColState.value
        val areaW = constraints.maxWidth.toFloat()
        val areaH = constraints.maxHeight.toFloat()

        val gapPx = if (isLandscape) {
            6.dp.toPx()
        } else {
            val sciFactor = (1f - startR.coerceIn(0f, 1f))
            (10.dp.toPx() * (1f - sciFactor)) + (8.dp.toPx() * sciFactor)
        }

        val visibleCols = (CalculatorViewModel.GRID_COLS.toFloat() - startC).coerceAtLeast(1f)
        val visibleRows = (CalculatorViewModel.GRID_ROWS.toFloat() - startR).coerceAtLeast(1f)

        val cellW = (areaW - gapPx * (visibleCols - 1f)) / visibleCols
        val cellH = (areaH - gapPx * (visibleRows - 1f)) / visibleRows
        val stepX = cellW + gapPx
        val stepY = cellH + gapPx

        val isRound = shapeMode == ButtonShapeMode.ROUND
        val btnW = if (isRound) min(cellW, cellH).roundToInt().coerceAtLeast(1) else cellW.roundToInt().coerceAtLeast(1)
        val btnH = if (isRound) btnW else cellH.roundToInt().coerceAtLeast(1)
        val offsetInCellX = (cellW - btnW) * 0.5f
        val offsetInCellY = (cellH - btnH) * 0.5f

        val childConstraints = Constraints.fixed(btnW, btnH)
        val placeables = measurables.map { it.measure(childConstraints) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            for (cellIdx in 0 until CalculatorViewModel.GRID_SIZE) {
                val r = cellIdx / CalculatorViewModel.GRID_COLS
                val c = cellIdx % CalculatorViewModel.GRID_COLS

                val rowVis = (r.toFloat() - startR + 1f).coerceIn(0f, 1f)
                val colVis = (c.toFloat() - startC + 1f).coerceIn(0f, 1f)
                val visibility = min(rowVis, colVis)

                if (visibility > 0.005f) {
                    val x = stepX * (c.toFloat() - startC) + offsetInCellX
                    val y = stepY * (r.toFloat() - startR) + offsetInCellY
                    val isSelectedCell = customMode && selectedCell == cellIdx
                    val z = if (isSelectedCell) 10f else 0f

                    if (visibility < 0.999f || isSelectedCell) {
                        val s = 0.72f + 0.28f * visibility
                        placeables[cellIdx].placeWithLayer(x.roundToInt(), y.roundToInt(), zIndex = z) {
                            alpha = visibility
                            scaleX = s
                            scaleY = s
                        }
                    } else {
                        placeables[cellIdx].place(x.roundToInt(), y.roundToInt(), zIndex = 0f)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyGridSlot(
    shape: Shape,
    visible: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    if (!visible) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "⇄",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ColorPaletteSwatch(
    topColor: Color,
    bottomColor: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "swatchScale"
    )
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(30.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(topColor, bottomColor),
                    start = Offset.Zero,
                    end = Offset(100f, 100f)
                )
            )
            .border(
                width = if (selected) 2.2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.4f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Text(
                text = "✓",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SettingsOptionChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
    }
    val fg = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CalculatorKeyButton(
    spec: CalcButtonSpec,
    shape: Shape,
    shapeMode: ButtonShapeMode,
    isDarkTheme: Boolean,
    uiAlpha: Float = 0.72f,
    enabled: Boolean = true,
    isCustomizationMode: Boolean = false,
    isSelectedForSwap: Boolean = false,
    modifier: Modifier = Modifier,
    fontSizeOverride: TextUnit = 20.sp,
    onTap: () -> Unit,
    onDelete: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val glowState = animateFloatAsState(
        targetValue = if (isPressed || isSelectedForSwap) 1f else 0f,
        animationSpec = tween(durationMillis = if (isPressed) 65 else 220),
        label = "buttonGlow"
    )
    val liftState = animateFloatAsState(
        targetValue = if (isSelectedForSwap) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "buttonLift"
    )
    val scaleState = animateFloatAsState(
        targetValue = when {
            isPressed -> 0.95f
            isSelectedForSwap -> 1.08f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
        label = "buttonScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "hoverFloat")
    val hoverBobDp by infiniteTransition.animateFloat(
        initialValue = -1.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hoverBob"
    )

    val (baseBgColor, contentColor) = when (spec.kind) {
        CalcButtonKind.NUMBER -> Pair(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        CalcButtonKind.ACTION -> Pair(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        CalcButtonKind.SCIENTIFIC -> Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.primary
        )
        CalcButtonKind.OPERATOR -> Pair(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        CalcButtonKind.CLEAR -> Pair(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        CalcButtonKind.EQUALS -> Pair(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
    }

    val glowColor = if (isSelectedForSwap) {
        MaterialTheme.colorScheme.primary
    } else if (isDarkTheme) {
        Color(0xFFE8F4FF)
    } else {
        Color(0xFF0D2B6B)
    }

    val topHighlightTint = if (isDarkTheme) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.65f)
    val bottomShadeTint = if (isDarkTheme) Color.Black.copy(alpha = 0.42f) else Color(0xFF0F172A).copy(alpha = 0.18f)

    // Base opaque surface colors cached in Brush; actual user uiAlpha is applied in onDrawBehind at 120 FPS
    val topSurfaceColor = lerp(baseBgColor, topHighlightTint, 0.22f)
    val bottomSurfaceColor = lerp(baseBgColor, bottomShadeTint, 0.28f)

    val topRimBase = if (isDarkTheme) Color.White.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.90f)
    val bottomRimBase = if (isDarkTheme) Color.Black.copy(alpha = 0.65f) else Color(0xFF1E293B).copy(alpha = 0.28f)

    Box(
        modifier = modifier
            .graphicsLayer {
                val s = scaleState.value
                val lift = liftState.value
                scaleX = s
                scaleY = s
                translationY = (-5.5f * lift + if (isSelectedForSwap) hoverBobDp else 0f).dp.toPx()
            }
            .drawWithCache {
                val btnCenter = Offset(size.width * 0.5f, size.height * 0.5f)
                val baseShadowRadius = size.maxDimension * 0.58f
                val baseShadowCenter = Offset(btnCenter.x, btnCenter.y + 3.dp.toPx())
                val baseShadowBrush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = if (isDarkTheme) 0.34f else 0.13f),
                        Color.Transparent
                    ),
                    center = baseShadowCenter,
                    radius = baseShadowRadius
                )

                val liftedShadowRadius = size.maxDimension * 0.76f
                val liftedShadowCenter = Offset(btnCenter.x, btnCenter.y + 10.dp.toPx())
                val liftedShadowBrush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = if (isDarkTheme) 0.58f else 0.28f),
                        Color.Black.copy(alpha = if (isDarkTheme) 0.22f else 0.10f),
                        Color.Transparent
                    ),
                    center = liftedShadowCenter,
                    radius = liftedShadowRadius
                )

                val glowRadius = size.maxDimension * 0.72f
                val outerGlowBrush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = if (isDarkTheme) 0.56f else 0.46f),
                        glowColor.copy(alpha = if (isDarkTheme) 0.22f else 0.18f),
                        Color.Transparent
                    ),
                    center = btnCenter,
                    radius = glowRadius
                )
                val cornerPx = when (shapeMode) {
                    ButtonShapeMode.ROUND -> size.minDimension * 0.5f
                    ButtonShapeMode.ROUNDED -> 18.dp.toPx()
                    ButtonShapeMode.SQUARE -> 5.dp.toPx()
                }
                val cornerRadius = CornerRadius(cornerPx, cornerPx)
                val surfaceBrush = Brush.verticalGradient(
                    colors = listOf(topSurfaceColor, bottomSurfaceColor)
                )
                val borderBrush = Brush.linearGradient(
                    colors = listOf(topRimBase, bottomRimBase),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
                val glowBorderBrush = Brush.linearGradient(
                    colors = listOf(glowColor.copy(alpha = 0.95f), glowColor.copy(alpha = 0.75f)),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
                val borderStroke = Stroke(width = if (isSelectedForSwap) 2.4.dp.toPx() else 1.3.dp.toPx())

                onDrawBehind {
                    val gp = glowState.value
                    val lift = liftState.value
                    val surfaceAlpha = uiAlpha.coerceIn(0.15f, 1.0f)

                    if (lift > 0.01f) {
                        drawCircle(
                            brush = liftedShadowBrush,
                            radius = liftedShadowRadius,
                            center = liftedShadowCenter,
                            alpha = lift
                        )
                    } else {
                        drawCircle(
                            brush = baseShadowBrush,
                            radius = baseShadowRadius,
                            center = baseShadowCenter,
                            alpha = surfaceAlpha
                        )
                    }

                    if (gp > 0.01f) {
                        drawCircle(
                            brush = outerGlowBrush,
                            radius = glowRadius,
                            center = center,
                            alpha = gp
                        )
                    }
                    drawRoundRect(
                        brush = surfaceBrush,
                        cornerRadius = cornerRadius,
                        alpha = surfaceAlpha
                    )
                    if (gp > 0.01f) {
                        drawRoundRect(
                            color = glowColor.copy(alpha = (if (isDarkTheme) 0.26f else 0.20f) * gp),
                            cornerRadius = cornerRadius
                        )
                    }
                    drawRoundRect(
                        brush = borderBrush,
                        cornerRadius = cornerRadius,
                        style = borderStroke,
                        alpha = (surfaceAlpha + 0.15f).coerceAtMost(1f)
                    )
                    if (gp > 0.01f) {
                        drawRoundRect(
                            brush = glowBorderBrush,
                            cornerRadius = cornerRadius,
                            style = borderStroke,
                            alpha = gp * 0.85f
                        )
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onTap
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = spec.label,
                color = contentColor,
                fontSize = fontSizeOverride,
                fontWeight = if (spec.kind == CalcButtonKind.NUMBER) FontWeight.Normal else FontWeight.SemiBold,
                maxLines = 1
            )
        }

        if (isCustomizationMode && enabled) {
            Canvas(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 1.dp, end = 1.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDelete)
            ) {
                val radius = size.minDimension * 0.5f
                val badgeCenter = Offset(size.width * 0.5f, size.height * 0.5f)

                drawCircle(
                    color = Color(0xFF2A0410).copy(alpha = 0.45f),
                    radius = radius,
                    center = badgeCenter
                )

                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFD6335C).copy(alpha = 0.92f),
                            Color(0xFF9E1B3E).copy(alpha = 0.95f),
                            Color(0xFF6E0F29).copy(alpha = 0.96f)
                        )
                    ),
                    radius = radius - 0.5.dp.toPx(),
                    center = badgeCenter
                )

                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = size.height * 0.55f
                    ),
                    radius = radius - 1.dp.toPx(),
                    center = badgeCenter
                )

                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFD1DC).copy(alpha = 0.80f),
                            Color(0xFF4A081C).copy(alpha = 0.75f)
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    ),
                    radius = radius - 0.6.dp.toPx(),
                    center = badgeCenter,
                    style = Stroke(width = 1.1.dp.toPx())
                )

                val arm = size.minDimension * 0.25f
                val strokeW = 1.8.dp.toPx()
                val crossColor = Color(0xFFFFF0F3)
                drawLine(
                    color = crossColor,
                    start = Offset(badgeCenter.x - arm, badgeCenter.y - arm),
                    end = Offset(badgeCenter.x + arm, badgeCenter.y + arm),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = crossColor,
                    start = Offset(badgeCenter.x + arm, badgeCenter.y - arm),
                    end = Offset(badgeCenter.x - arm, badgeCenter.y + arm),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
