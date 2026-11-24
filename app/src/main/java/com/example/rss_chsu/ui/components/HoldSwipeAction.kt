package com.example.rss_chsu.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

/** Описание одного действия в веере. */
data class SwipeActionConfig(
    val id: String,
    val icon: ImageVector,
    val label: String,
    /** Оставлено для совместимости; компонент рисует монохром независимо от значения. */
    val tint: Color = Color(0xFF1E1E24)
)

/**
 * Обёртка: long-press по контенту → вокруг пальца раскрывается веер из actions.
 * Пользователь ведёт палец к иконке, отпускает — onAction срабатывает.
 *
 * Ключевые свойства:
 *  — Веер рисуется через [Popup] в оконных координатах, поэтому не режется
 *    соседними элементами LazyGrid.
 *  — Направление веера адаптивное: если палец в правой половине экрана,
 *    иконки уходят влево-вверх; если в левой — вправо-вверх; если по центру —
 *    ровно вверх. Это «по-пинтерестовски» и удобно правшам.
 *  — Позиции иконок клампятся в границы экрана, чтобы никогда не уходить за край.
 *  — Между иконками выдержан зазор ~44 dp (радиус 128 dp, разброс ±45°,
 *    диаметр иконки 56 dp), чтобы кнопки не слипались.
 *
 * Параметр [fanRotationDeg] задаёт максимальный наклон от вертикали (по модулю).
 */
@Composable
fun HoldSwipeAction(
    actions: List<SwipeActionConfig>,
    onAction: (SwipeActionConfig) -> Unit,
    modifier: Modifier = Modifier,
    fanRotationDeg: Float = -45f,
    content: @Composable () -> Unit
) {
    var cardPosInRoot by remember { mutableStateOf(Offset.Zero) }
    var isActive by remember { mutableStateOf(false) }
    var touchInRoot by remember { mutableStateOf(Offset.Zero) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var highlightedId by remember { mutableStateOf<String?>(null) }

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current

    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    val screenWidthPx = with(density) { screenWidthDp.toPx() }
    val screenHeightPx = with(density) { screenHeightDp.toPx() }
    val screenCenterX = screenWidthPx / 2f

    // ── Геометрия веера ──────────────────────────────────────────────
    // Радиус и разброс подобраны так, чтобы между соседними иконками
    // был заметный зазор: arc = R × Δangle ≈ 128 × (π/4) ≈ 100 dp,
    // зазор = arc − diameter ≈ 100 − 56 ≈ 44 dp.
    val radiusPx = with(density) { 128.dp.toPx() }
    val hitRadiusPx = with(density) { 44.dp.toPx() }
    val iconSizeDp = 56.dp
    val iconHalfPx = with(density) { (iconSizeDp / 2).toPx() }
    val edgePaddingPx = with(density) { 8.dp.toPx() }

    // ── Адаптивное направление веера ─────────────────────────────────
    // Направление «вверх» в экранных координатах = 270° (0° = вправо, 90° = вниз).
    // Наклоняем в сторону центра: в правой половине экрана → влево-вверх,
    // в левой → вправо-вверх, по центру → ровно вверх.
    val maxTiltDeg = abs(fanRotationDeg).coerceAtLeast(0f)
    val baseAngleDeg = remember(touchInRoot.x, screenCenterX, maxTiltDeg) {
        val bias = if (screenCenterX > 0f) {
            ((touchInRoot.x - screenCenterX) / screenCenterX).coerceIn(-1f, 1f)
        } else 0f
        270f + (-bias * maxTiltDeg)
    }

    // ±45° для трёх кнопок и ±25° для двух — даёт зазор ~44 dp / ~24 dp
    // соответственно при радиусе 128 dp и диаметре 56 dp.
    val spreadDeg = when (actions.size) {
        1 -> listOf(0f)
        2 -> listOf(-25f, 25f)
        else -> listOf(-45f, 0f, 45f).take(actions.size)
    }

    val anglesDeg = remember(baseAngleDeg, spreadDeg) {
        spreadDeg.map { baseAngleDeg + it }
    }

    val actionPositions = remember(
        touchInRoot,
        radiusPx,
        anglesDeg,
        screenWidthPx,
        screenHeightPx
    ) {
        actions.mapIndexed { index, action ->
            val deg = anglesDeg.getOrElse(index) { 270f }
            val rad = Math.toRadians(deg.toDouble())
            val rawX = touchInRoot.x + (cos(rad) * radiusPx).toFloat()
            val rawY = touchInRoot.y + (sin(rad) * radiusPx).toFloat()

            val minX = edgePaddingPx + iconHalfPx
            val maxX = (screenWidthPx - edgePaddingPx - iconHalfPx).coerceAtLeast(minX)
            val minY = edgePaddingPx + iconHalfPx
            val maxY = (screenHeightPx - edgePaddingPx - iconHalfPx).coerceAtLeast(minY)

            action to Offset(
                rawX.coerceIn(minX, maxX),
                rawY.coerceIn(minY, maxY)
            )
        }
    }

    val currentActionPositions by rememberUpdatedState(actionPositions)
    val currentOnAction by rememberUpdatedState(onAction)

    val groupScale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = if (isActive) {
            spring(dampingRatio = 0.6f, stiffness = 500f)
        } else {
            tween(durationMillis = 140)
        },
        label = "fanGroup"
    )

    // ── Жест на карточке ─────────────────────────────────────────────
    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                cardPosInRoot = coords.positionInRoot()
            }
            .pointerInput(actions) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { localStart ->
                        touchInRoot = cardPosInRoot + localStart
                        dragOffset = Offset.Zero
                        highlightedId = null
                        isActive = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        dragOffset += amount
                        val finger = touchInRoot + dragOffset

                        val nearest = currentActionPositions.minByOrNull { (_, pos) ->
                            (pos - finger).getDistance()
                        }
                        val newHighlight = nearest
                            ?.takeIf { (_, pos) ->
                                (pos - finger).getDistance() < hitRadiusPx
                            }
                            ?.first
                            ?.id

                        if (newHighlight != highlightedId) {
                            highlightedId = newHighlight
                            if (newHighlight != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    },
                    onDragEnd = {
                        val id = highlightedId
                        if (id != null) {
                            currentActionPositions
                                .firstOrNull { (action, _) -> action.id == id }
                                ?.let { (action, _) -> currentOnAction(action) }
                        }
                        isActive = false
                        highlightedId = null
                        dragOffset = Offset.Zero
                    },
                    onDragCancel = {
                        isActive = false
                        highlightedId = null
                        dragOffset = Offset.Zero
                    }
                )
            }
    ) {
        content()
    }

    // ── Веер в отдельном окне (поверх карточек) ──────────────────────
    if (isActive || groupScale > 0.01f) {
        Popup(
            popupPositionProvider = ZeroOffsetPopupPositionProvider,
            properties = PopupProperties(
                focusable = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Box(Modifier.size(screenWidthDp, screenHeightDp)) {
                actionPositions.forEach { (action, pos) ->
                    FanActionIcon(
                        action = action,
                        position = pos,
                        isHighlighted = highlightedId == action.id,
                        groupScale = groupScale,
                        iconSizeDp = iconSizeDp,
                        iconHalfPx = iconHalfPx
                    )
                }
            }
        }
    }
}

/**
 * Popup, который ставится в (0, 0) окна. Тогда координаты иконок,
 * вычисленные через positionInRoot(), совпадают с координатами внутри Popup.
 */
private val ZeroOffsetPopupPositionProvider = object : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset = IntOffset.Zero
}

@Composable
private fun FanActionIcon(
    action: SwipeActionConfig,
    position: Offset,
    isHighlighted: Boolean,
    groupScale: Float,
    iconSizeDp: Dp,
    iconHalfPx: Float
) {
    val highlightScale by animateFloatAsState(
        targetValue = if (isHighlighted) 1.25f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "fanHighlight"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isHighlighted) Color(0xFF3D3D46) else Color(0xFF1E1E24),
        animationSpec = tween(durationMillis = 180),
        label = "fanBgColor"
    )

    val totalScale = groupScale * highlightScale

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (position.x - iconHalfPx).roundToInt(),
                    (position.y - iconHalfPx).roundToInt()
                )
            }
            .size(iconSizeDp)
            .graphicsLayer {
                scaleX = totalScale
                scaleY = totalScale
                alpha = groupScale.coerceIn(0f, 1f)
            }
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = action.label,
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
    }
}