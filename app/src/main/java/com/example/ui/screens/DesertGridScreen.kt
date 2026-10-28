package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.FieldBiome
import com.example.data.model.FieldBoard
import com.example.data.model.Pyramid
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sqrt

fun parseHexColor(hex: String, fallback: Color = Color(0xFF2997FF)): Color {
    return try {
        val clean = hex.trim().removePrefix("#")
        val value = clean.toLong(16)
        if (clean.length == 6) {
            Color(0xFF000000 or value)
        } else if (clean.length == 8) {
            Color(value)
        } else {
            fallback
        }
    } catch (_: Exception) {
        fallback
    }
}

fun shadeColor(color: Color, factor: Float): Color {
    return if (factor <= 1f) {
        Color(
            red = (color.red * factor).coerceIn(0f, 1f),
            green = (color.green * factor).coerceIn(0f, 1f),
            blue = (color.blue * factor).coerceIn(0f, 1f),
            alpha = color.alpha
        )
    } else {
        val t = (factor - 1f).coerceIn(0f, 1f)
        Color(
            red = (color.red + (1f - color.red) * t).coerceIn(0f, 1f),
            green = (color.green + (1f - color.green) * t).coerceIn(0f, 1f),
            blue = (color.blue + (1f - color.blue) * t).coerceIn(0f, 1f),
            alpha = color.alpha
        )
    }
}

private data class ProjectedPyramidHit(
    val pyramid: Pyramid,
    val basePos: Offset,
    val centerX: Float,
    val centerY: Float,
    val hitRadiusPx: Float
)

@Composable
fun DesertGridScreen(
    boards: List<FieldBoard>,
    activeBoard: FieldBoard,
    pyramids: List<Pyramid>,
    isMoveMode: Boolean,
    onSelectBoard: (String) -> Unit,
    onOpenBoardSettings: (FieldBoard) -> Unit,
    onOpenCreateBoard: () -> Unit,
    onSelectPyramid: (String) -> Unit,
    onToggleMoveMode: () -> Unit,
    onMovePyramid: (pyramidId: String, targetGridX: Float, targetGridY: Float) -> Unit,
    onOpenCreatePyramid: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val biome = activeBoard.biome
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var zoom by remember { mutableFloatStateOf(1f) }

    // Active drag state when dragging a pyramid in Move Mode
    var draggingPyramidId by remember(isMoveMode, activeBoard.id) { mutableStateOf<String?>(null) }
    var dragScreenOffset by remember(isMoveMode, activeBoard.id) { mutableStateOf<Offset?>(null) }

    val density = LocalDensity.current

    // Suggested free grid slot for newly created pyramids on this board
    val suggestedCreateCoords = remember(pyramids) {
        val candidates = listOf(
            0f to 0f,
            1f to 0f,
            0f to 1f,
            -1f to 0f,
            0f to -1f,
            1f to 1f,
            -1f to 1f,
            1f to -1f,
            -1f to -1f,
            2f to 0f,
            0f to 2f
        )
        candidates.firstOrNull { (cx, cy) ->
            pyramids.none { hypot(it.gridX - cx, it.gridY - cy) < 0.65f }
        } ?: ((pyramids.size % 3 - 1).toFloat() to (pyramids.size / 3).toFloat())
    }

    val backgroundFallback = when (biome) {
        FieldBiome.DESERT -> Color(0xFFDDA161)
        FieldBiome.SNOW -> Color(0xFFDCEBFA)
        FieldBiome.GRASS -> Color(0xFF5FA855)
        FieldBiome.UNDERWATER -> Color(0xFF093B5A)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundFallback)
            .testTag("desert_grid_screen")
    ) {
        val canvasWidthPx = with(density) { maxWidth.toPx() }
        val canvasHeightPx = with(density) { maxHeight.toPx() }

        val baseTileW = (canvasWidthPx.coerceAtMost(canvasHeightPx) * 0.32f).coerceIn(160f, 320f)
        val tileW = baseTileW * zoom
        val tileH = tileW * 0.56f
        val originX = canvasWidthPx * 0.50f + panX
        val originY = canvasHeightPx * 0.42f + panY

        fun gridToScreen(gx: Float, gy: Float): Offset {
            val sx = originX + (gx - gy) * (tileW * 0.5f)
            val sy = originY + (gx + gy) * (tileH * 0.5f)
            return Offset(sx, sy)
        }

        val sortedPyramids = remember(pyramids) {
            pyramids.sortedWith(compareBy<Pyramid> { it.gridX + it.gridY }.thenBy { it.gridX })
        }

        val hitTargets = sortedPyramids.map { pyr ->
            val pos = if (pyr.id == draggingPyramidId && dragScreenOffset != null) {
                dragScreenOffset!!
            } else {
                gridToScreen(pyr.gridX, pyr.gridY)
            }
            val weightScale = computeWeightScale(pyr.totalWeight)
            val baseRadius = tileW * 0.44f * weightScale
            ProjectedPyramidHit(
                pyramid = pyr,
                basePos = pos,
                centerX = pos.x,
                centerY = pos.y - baseRadius * 0.25f,
                hitRadiusPx = baseRadius.coerceAtLeast(64f)
            )
        }

        // Keep updated references for pointerInput so gestures are NEVER cancelled on recomposition
        val currentHitTargets by rememberUpdatedState(hitTargets)
        val currentOriginX by rememberUpdatedState(originX)
        val currentOriginY by rememberUpdatedState(originY)
        val currentTileW by rememberUpdatedState(tileW)
        val currentTileH by rememberUpdatedState(tileH)
        val currentOnMovePyramid by rememberUpdatedState(onMovePyramid)
        val currentOnSelectPyramid by rememberUpdatedState(onSelectPyramid)

        fun screenToExactGrid(screenPt: Offset): Pair<Float, Float> {
            val halfW = (currentTileW * 0.5f).coerceAtLeast(1f)
            val halfH = (currentTileH * 0.5f).coerceAtLeast(1f)
            val dx = (screenPt.x - currentOriginX) / halfW
            val dy = (screenPt.y - currentOriginY) / halfH
            val gx = ((dx + dy) * 0.5f).coerceIn(-10f, 10f)
            val gy = ((dy - dx) * 0.5f).coerceIn(-10f, 10f)
            // Snap gently to 0.1f increments so movement is completely fluid and exact
            val roundedGx = (gx * 10f).roundToInt() / 10f
            val roundedGy = (gy * 10f).roundToInt() / 10f
            return roundedGx to roundedGy
        }

        val previewGridCell: Pair<Float, Float>? = if (draggingPyramidId != null && dragScreenOffset != null) {
            screenToExactGrid(dragScreenOffset!!)
        } else {
            null
        }

        // Interactive 3D Isometric Field Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // Stable keys ONLY: gesture coroutine never restarts during pan or drag!
                .pointerInput(isMoveMode, activeBoard.id) {
                    if (isMoveMode) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                val hit = currentHitTargets.asReversed().firstOrNull { target ->
                                    hypot(startOffset.x - target.centerX, startOffset.y - target.centerY) <= target.hitRadiusPx * 1.25f ||
                                        hypot(startOffset.x - target.basePos.x, startOffset.y - target.basePos.y) <= target.hitRadiusPx * 1.25f
                                }
                                if (hit != null) {
                                    draggingPyramidId = hit.pyramid.id
                                    dragScreenOffset = hit.basePos
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val activeId = draggingPyramidId
                                val curOffset = dragScreenOffset
                                if (activeId != null && curOffset != null) {
                                    dragScreenOffset = curOffset + dragAmount
                                } else {
                                    panX = (panX + dragAmount.x).coerceIn(-canvasWidthPx * 2.5f, canvasWidthPx * 2.5f)
                                    panY = (panY + dragAmount.y).coerceIn(-canvasHeightPx * 2.5f, canvasHeightPx * 2.5f)
                                }
                            },
                            onDragEnd = {
                                val activeId = draggingPyramidId
                                val finalOffset = dragScreenOffset
                                if (activeId != null && finalOffset != null) {
                                    val (gx, gy) = screenToExactGrid(finalOffset)
                                    currentOnMovePyramid(activeId, gx, gy)
                                }
                                draggingPyramidId = null
                                dragScreenOffset = null
                            },
                            onDragCancel = {
                                draggingPyramidId = null
                                dragScreenOffset = null
                            }
                        )
                    } else {
                        detectTransformGestures { _, pan, gestureZoom, _ ->
                            zoom = (zoom * gestureZoom).coerceIn(0.55f, 2.2f)
                            panX = (panX + pan.x).coerceIn(-canvasWidthPx * 2.5f, canvasWidthPx * 2.5f)
                            panY = (panY + pan.y).coerceIn(-canvasHeightPx * 2.5f, canvasHeightPx * 2.5f)
                        }
                    }
                }
                .pointerInput(isMoveMode, activeBoard.id) {
                    if (!isMoveMode) {
                        detectTapGestures(
                            onTap = { tapOffset ->
                                val tappedPyramid = currentHitTargets.asReversed().firstOrNull { target ->
                                    hypot(tapOffset.x - target.centerX, tapOffset.y - target.centerY) <= target.hitRadiusPx
                                }
                                if (tappedPyramid != null) {
                                    currentOnSelectPyramid(tappedPyramid.pyramid.id)
                                }
                            }
                        )
                    }
                }
        ) {
            // 1. Biome Terrain Background (Desert, Snow, Grass, or Underwater)
            drawFieldBiomeTerrain(biome, originX, originY, zoom)

            // 2. Isometric Square Grid Projected Over the Terrain
            drawIsometricGrid(
                biome = biome,
                originX = originX,
                originY = originY,
                tileW = tileW,
                tileH = tileH,
                gridRadius = 10
            )

            // 3. In Move Mode, highlight grid cells and the live target position under dragged pyramid
            if (isMoveMode) {
                drawMoveGridOverlay(
                    previewGridCell = previewGridCell,
                    gridToScreen = ::gridToScreen,
                    tileW = tileW,
                    tileH = tileH
                )
            }

            // 4. Render 3D Monochromatic Stepped Pyramids Scaled by Weight
            for (pyr in sortedPyramids) {
                val isDraggingThis = pyr.id == draggingPyramidId && dragScreenOffset != null
                val center = if (isDraggingThis) {
                    dragScreenOffset!!
                } else {
                    gridToScreen(pyr.gridX, pyr.gridY)
                }
                drawIsometricSteppedPyramid(
                    center = center,
                    totalWeight = pyr.totalWeight,
                    baseColor = parseHexColor(pyr.baseColor),
                    biome = biome,
                    isHighlightedForMove = isMoveMode,
                    isActivelyDragging = isDraggingThis,
                    tileW = tileW,
                    tileH = tileH
                )
            }

            // 5. If Underwater biome is active, render submerged water tint, caustic rays & rising bubbles OVER pyramids
            if (biome == FieldBiome.UNDERWATER) {
                drawUnderwaterSubmergedOverlay(
                    pyramids = sortedPyramids,
                    gridToScreen = ::gridToScreen,
                    originX = originX,
                    originY = originY,
                    zoom = zoom,
                    tileW = tileW
                )
            }
        }

        // Compact Floating Labels on Each 3D Pyramid
        for (pyr in sortedPyramids) {
            val isDraggingThis = pyr.id == draggingPyramidId && dragScreenOffset != null
            val pos = if (isDraggingThis) {
                dragScreenOffset!!
            } else {
                gridToScreen(pyr.gridX, pyr.gridY)
            }
            val currentPyrPos by rememberUpdatedState(pos)
            val weightScale = computeWeightScale(pyr.totalWeight)
            val compactWidthDp = when {
                pyr.totalWeight >= 100L -> 114.dp
                pyr.totalWeight >= 40L -> 102.dp
                else -> 94.dp
            }
            val labelOffsetY = (tileH * 0.14f * weightScale).roundToInt()

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (pos.x - with(density) { (compactWidthDp / 2).toPx() }).roundToInt(),
                            y = (pos.y + labelOffsetY - with(density) { 14.dp.toPx() }).roundToInt()
                        )
                    }
                    .width(compactWidthDp)
                    .shadow(if (isDraggingThis) 10.dp else 4.dp, RoundedCornerShape(7.dp))
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (isDraggingThis) Color(0xEE0284C7) else Color(0xCC161E2B))
                    .border(
                        width = if (isMoveMode) 1.4.dp else 0.8.dp,
                        color = if (isMoveMode) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(7.dp)
                    )
                    // Stable keys ONLY (`isMoveMode, pyr.id`) so dragging the label never resets mid-drag!
                    .pointerInput(isMoveMode, pyr.id) {
                        if (isMoveMode) {
                            detectDragGestures(
                                onDragStart = {
                                    draggingPyramidId = pyr.id
                                    dragScreenOffset = currentPyrPos
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val cur = dragScreenOffset ?: currentPyrPos
                                    dragScreenOffset = cur + dragAmount
                                },
                                onDragEnd = {
                                    val finalOffset = dragScreenOffset
                                    if (finalOffset != null) {
                                        val (gx, gy) = screenToExactGrid(finalOffset)
                                        currentOnMovePyramid(pyr.id, gx, gy)
                                    }
                                    draggingPyramidId = null
                                    dragScreenOffset = null
                                },
                                onDragCancel = {
                                    draggingPyramidId = null
                                    dragScreenOffset = null
                                }
                            )
                        } else {
                            detectTapGestures(onTap = { currentOnSelectPyramid(pyr.id) })
                        }
                    }
                    .padding(horizontal = 6.dp, vertical = 3.dp)
                    .testTag("pyramid_card_${pyr.id}"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pyr.title,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 11.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.weight_bracket_format, pyr.totalWeight),
                        color = if (isMoveMode) Color(0xFFE0F2FE) else Color(0xFFCBD5E1),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // HUD Layer:
        // - Move, Create Pyramid, and Zoom buttons are placed ABOVE the Field Switcher buttons!
        // - Field Switcher buttons are placed at the bottom of the screen!
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Subtle empty-state or move-mode banner at top
            if (isMoveMode) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xEE10233A),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.65f), RoundedCornerShape(50))
                        .testTag("move_mode_drag_hint")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenWith,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Зажмите пирамиду и перетащите её в любое место поля",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else if (pyramids.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xD9172030),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                ) {
                    Text(
                        text = "Поле «${activeBoard.title}» пока пусто. Нажмите «+ Создать пирамиду» ниже, чтобы заложить первую цель.",
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            // Bottom Dock:
            // Upper tier: Переместить (Left) + "+ Создать пирамиду" (Center) + Масштаб +/- (Right)
            // Bottom tier: Кнопки смены бесконечных полей
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Controls Row ABOVE the Field Switcher Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // 1. Move Mode Toggle Button (Left, above field switcher)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isMoveMode) Color(0xFF0284C7) else Color(0xD9232936),
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isMoveMode) 1.8.dp else 1.dp,
                                    color = if (isMoveMode) Color.White else Color.White.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onToggleMoveMode() }
                                .testTag("move_pyramids_mode_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isMoveMode) Icons.Default.Check else Icons.Default.OpenWith,
                                    contentDescription = stringResource(R.string.cd_move_pyramids),
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isMoveMode) Color(0xFF0284C7) else Color(0xD9232936),
                            shadowElevation = 4.dp
                        ) {
                            Text(
                                text = if (isMoveMode) "Готово" else stringResource(R.string.move_pyramid_label),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // 2. "+ Создать пирамиду" Button (Center, above field switcher)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xE6232A38),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(14.dp))
                            .clickable {
                                onOpenCreatePyramid(suggestedCreateCoords.first, suggestedCreateCoords.second)
                            }
                            .testTag("create_pyramid_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Создать пирамиду",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // 3. Single Zoom Control Pill (+ / -) (Right, above field switcher)
                    ZoomControlPill(
                        onZoomIn = { zoom = (zoom * 1.18f).coerceAtMost(2.2f) },
                        onZoomOut = { zoom = (zoom / 1.18f).coerceAtLeast(0.55f) }
                    )
                }

                // Bottom Bar: Infinite Fields Switcher ("Кнопки со сменой просмотра полей")
                FieldSwitcherBottomBar(
                    boards = boards,
                    activeBoard = activeBoard,
                    onSelectBoard = onSelectBoard,
                    onOpenBoardSettings = onOpenBoardSettings,
                    onOpenCreateBoard = onOpenCreateBoard
                )
            }
        }
    }
}

@Composable
private fun FieldSwitcherBottomBar(
    boards: List<FieldBoard>,
    activeBoard: FieldBoard,
    onSelectBoard: (String) -> Unit,
    onOpenBoardSettings: (FieldBoard) -> Unit,
    onOpenCreateBoard: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xD9151C28),
        shadowElevation = 10.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (board in boards) {
                val isSelected = board.id == activeBoard.id
                val biomeBadgeColors = when (board.biome) {
                    FieldBiome.DESERT -> listOf(Color(0xFFF6CE9B), Color(0xFFD5934E))
                    FieldBiome.SNOW -> listOf(Color(0xFFEFF6FF), Color(0xFF60A5FA))
                    FieldBiome.GRASS -> listOf(Color(0xFF86EFAC), Color(0xFF16A34A))
                    FieldBiome.UNDERWATER -> listOf(Color(0xFF38BDF8), Color(0xFF0C4A6E))
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFF223552) else Color(0xFF1E2636),
                    modifier = Modifier
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = if (isSelected) 1.6.dp else 1.dp,
                            color = if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectBoard(board.id) }
                        .testTag("board_tab_${board.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(start = 10.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(biomeBadgeColors))
                                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(verticalArrangement = Arrangement.Center) {
                            Text(
                                text = board.title,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                lineHeight = 13.sp
                            )
                            Text(
                                text = board.biome.title,
                                color = if (isSelected) Color(0xFF7DD3FC) else Color(0xFF94A3B8),
                                fontSize = 9.sp,
                                lineHeight = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent
                                )
                                .clickable {
                                    onSelectBoard(board.id)
                                    onOpenBoardSettings(board)
                                }
                                .testTag("board_settings_${board.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Настроить вид поля ${board.title}",
                                tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFFCBD5E1),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // "+ Поле" button to add a new Infinite Field
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable { onOpenCreateBoard() }
                    .testTag("add_new_board_top_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Добавить новое поле",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Поле",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomControlPill(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xD9232936),
        shadowElevation = 6.dp
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = onZoomIn,
                modifier = Modifier
                    .size(46.dp)
                    .testTag("zoom_in_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_zoom_in),
                    tint = Color.White
                )
            }
            HorizontalDivider(
                modifier = Modifier.width(28.dp),
                color = Color.White.copy(alpha = 0.2f)
            )
            IconButton(
                onClick = onZoomOut,
                modifier = Modifier
                    .size(46.dp)
                    .testTag("zoom_out_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = stringResource(R.string.cd_zoom_out),
                    tint = Color.White
                )
            }
        }
    }
}

private fun computeWeightScale(totalWeight: Long): Float {
    val w = totalWeight.coerceAtLeast(5L).toFloat()
    return (0.42f + sqrt(w) * 0.08f).coerceIn(0.58f, 1.65f)
}

private fun DrawScope.drawFieldBiomeTerrain(
    biome: FieldBiome,
    originX: Float,
    originY: Float,
    zoom: Float
) {
    val w = size.width
    val h = size.height

    val (bgGradient, ridgeTopColor, ridgeMidColor) = when (biome) {
        FieldBiome.DESERT -> Triple(
            listOf(
                Color(0xFFF6CE9B),
                Color(0xFFE7AD6B),
                Color(0xFFD5934E),
                Color(0xFFECB87C)
            ),
            Color(0x55B97432),
            Color(0x289B5B1E)
        )
        FieldBiome.SNOW -> Triple(
            listOf(
                Color(0xFFF8FAFC),
                Color(0xFFE2E8F0),
                Color(0xFFBFDBFE),
                Color(0xFFEFF6FF)
            ),
            Color(0x5560A5FA),
            Color(0x243B82F6)
        )
        FieldBiome.GRASS -> Triple(
            listOf(
                Color(0xFFA7F3D0),
                Color(0xFF6EE7B7),
                Color(0xFF34D399),
                Color(0xFF4ADE80)
            ),
            Color(0x4D047857),
            Color(0x24065F46)
        )
        FieldBiome.UNDERWATER -> Triple(
            listOf(
                Color(0xFF0C4A6E),
                Color(0xFF075985),
                Color(0xFF082F49),
                Color(0xFF0F172A)
            ),
            Color(0x550284C7),
            Color(0x2E0369A1)
        )
    }

    drawRect(
        brush = Brush.linearGradient(
            colors = bgGradient,
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )
    )

    val shiftX = (originX - w * 0.5f) * 0.35f
    val shiftY = (originY - h * 0.5f) * 0.35f

    for (i in -1..5) {
        val bandY = h * (i * 0.22f) + (shiftY % (h * 0.22f))
        val duneCrest = Path().apply {
            moveTo(-w * 0.2f, bandY)
            cubicTo(
                w * 0.25f + shiftX * 0.2f,
                bandY - 65f * zoom,
                w * 0.65f - shiftX * 0.2f,
                bandY + 55f * zoom,
                w * 1.2f,
                bandY - 25f * zoom
            )
            lineTo(w * 1.2f, bandY + 130f * zoom)
            cubicTo(
                w * 0.65f,
                bandY + 185f * zoom,
                w * 0.25f,
                bandY + 75f * zoom,
                -w * 0.2f,
                bandY + 120f * zoom
            )
            close()
        }
        drawPath(
            path = duneCrest,
            brush = Brush.verticalGradient(
                colors = listOf(
                    ridgeTopColor,
                    ridgeMidColor,
                    Color.Transparent
                ),
                startY = bandY - 40f * zoom,
                endY = bandY + 160f * zoom
            )
        )
    }

    if (biome == FieldBiome.SNOW) {
        for (k in 0..28) {
            val fx = ((k * 137f + shiftX * 1.2f) % w + w) % w
            val fy = ((k * 89f + shiftY * 1.2f) % h + h) % h
            drawCircle(
                color = Color.White.copy(alpha = 0.75f),
                radius = (2.2f + (k % 3) * 1.3f) * zoom.coerceIn(0.8f, 1.3f),
                center = Offset(fx, fy)
            )
        }
    } else if (biome == FieldBiome.GRASS) {
        for (k in 0..22) {
            val fx = ((k * 163f + shiftX) % w + w) % w
            val fy = ((k * 107f + shiftY) % h + h) % h
            drawCircle(
                color = Color(0xFFFEF08A).copy(alpha = 0.45f),
                radius = (2.5f + (k % 2) * 1.5f) * zoom.coerceIn(0.8f, 1.3f),
                center = Offset(fx, fy)
            )
        }
    }
}

private fun DrawScope.drawIsometricGrid(
    biome: FieldBiome,
    originX: Float,
    originY: Float,
    tileW: Float,
    tileH: Float,
    gridRadius: Int
) {
    val gridColor = when (biome) {
        FieldBiome.DESERT -> Color(0xFF4A2E14).copy(alpha = 0.42f)
        FieldBiome.SNOW -> Color(0xFF1E40AF).copy(alpha = 0.28f)
        FieldBiome.GRASS -> Color(0xFF064E3B).copy(alpha = 0.32f)
        FieldBiome.UNDERWATER -> Color(0xFF38BDF8).copy(alpha = 0.30f)
    }
    val halfW = tileW * 0.5f
    val halfH = tileH * 0.5f

    for (i in -gridRadius..gridRadius) {
        val fi = i.toFloat()
        val r = gridRadius.toFloat()

        val startX1 = originX + (fi - (-r)) * halfW
        val startY1 = originY + (fi + (-r)) * halfH
        val endX1 = originX + (fi - r) * halfW
        val endY1 = originY + (fi + r) * halfH
        drawLine(
            color = gridColor,
            start = Offset(startX1, startY1),
            end = Offset(endX1, endY1),
            strokeWidth = 1.4f
        )

        val startX2 = originX + (-r - fi) * halfW
        val startY2 = originY + (-r + fi) * halfH
        val endX2 = originX + (r - fi) * halfW
        val endY2 = originY + (r + fi) * halfH
        drawLine(
            color = gridColor,
            start = Offset(startX2, startY2),
            end = Offset(endX2, endY2),
            strokeWidth = 1.4f
        )
    }
}

private fun DrawScope.drawMoveGridOverlay(
    previewGridCell: Pair<Float, Float>?,
    gridToScreen: (Float, Float) -> Offset,
    tileW: Float,
    tileH: Float
) {
    val rx = tileW * 0.34f
    val ry = tileH * 0.34f
    for (gx in -4..4) {
        for (gy in -4..4) {
            val center = gridToScreen(gx.toFloat(), gy.toFloat())
            val diamond = Path().apply {
                moveTo(center.x, center.y - ry)
                lineTo(center.x + rx, center.y)
                lineTo(center.x, center.y + ry)
                lineTo(center.x - rx, center.y)
                close()
            }
            drawPath(
                path = diamond,
                color = Color(0x18FFFFFF)
            )
            drawPath(
                path = diamond,
                color = Color.White.copy(alpha = 0.28f),
                style = Stroke(
                    width = 1.1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )
            )
        }
    }

    if (previewGridCell != null) {
        val targetCenter = gridToScreen(previewGridCell.first, previewGridCell.second)
        val snapDiamond = Path().apply {
            moveTo(targetCenter.x, targetCenter.y - ry)
            lineTo(targetCenter.x + rx, targetCenter.y)
            lineTo(targetCenter.x, targetCenter.y + ry)
            lineTo(targetCenter.x - rx, targetCenter.y)
            close()
        }
        drawPath(
            path = snapDiamond,
            color = Color(0x5538BDF8)
        )
        drawPath(
            path = snapDiamond,
            color = Color(0xFF38BDF8),
            style = Stroke(width = 3f)
        )
    }
}

private fun DrawScope.drawIsometricSteppedPyramid(
    center: Offset,
    totalWeight: Long,
    baseColor: Color,
    biome: FieldBiome,
    isHighlightedForMove: Boolean,
    isActivelyDragging: Boolean,
    tileW: Float,
    tileH: Float
) {
    val weightScale = computeWeightScale(totalWeight)
    val baseRadiusX = tileW * 0.44f * weightScale
    val baseRadiusY = tileH * 0.44f * weightScale

    val tierCount = when {
        totalWeight >= 100L -> 4
        totalWeight >= 40L -> 3
        else -> 3
    }

    val shadowColor = when (biome) {
        FieldBiome.DESERT -> Color(0x4D5E3410)
        FieldBiome.SNOW -> Color(0x381E3A8A)
        FieldBiome.GRASS -> Color(0x45064E3B)
        FieldBiome.UNDERWATER -> Color(0x55020617)
    }
    val shadowOffset = Offset(-baseRadiusX * 0.26f, baseRadiusY * 0.22f)
    val shadowPath = Path().apply {
        moveTo(center.x + shadowOffset.x, center.y - baseRadiusY + shadowOffset.y)
        lineTo(center.x + baseRadiusX, center.y)
        lineTo(center.x + shadowOffset.x, center.y + baseRadiusY + shadowOffset.y)
        lineTo(center.x - baseRadiusX + shadowOffset.x * 1.15f, center.y + shadowOffset.y)
        close()
    }
    drawPath(path = shadowPath, color = shadowColor)

    if (isHighlightedForMove) {
        val ringRx = baseRadiusX * 1.12f
        val ringRy = baseRadiusY * 1.12f
        val ringPath = Path().apply {
            moveTo(center.x, center.y - ringRy)
            lineTo(center.x + ringRx, center.y)
            lineTo(center.x, center.y + ringRy)
            lineTo(center.x - ringRx, center.y)
            close()
        }
        drawPath(
            path = ringPath,
            color = if (isActivelyDragging) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.72f),
            style = Stroke(width = if (isActivelyDragging) 3.5f else 2.2f)
        )
    }

    var currentBaseY = center.y
    val stepHeight = baseRadiusY * 0.34f

    for (tier in 0 until tierCount) {
        val ratio = 1f - tier * (0.68f / tierCount)
        val rx = baseRadiusX * ratio
        val ry = baseRadiusY * ratio
        val h = stepHeight * (1f - tier * 0.08f)

        val toneProgress = tier.toFloat() / tierCount.toFloat()
        val tierBase = shadeColor(baseColor, 0.65f + toneProgress * 0.45f)
        val leftFaceColor = shadeColor(tierBase, 0.74f)
        val rightFaceColor = shadeColor(tierBase, 0.94f)
        val topFaceColor = shadeColor(tierBase, 1.15f)

        val topCenterY = currentBaseY - h

        val leftFace = Path().apply {
            moveTo(center.x - rx, topCenterY)
            lineTo(center.x, topCenterY + ry)
            lineTo(center.x, currentBaseY + ry)
            lineTo(center.x - rx, currentBaseY)
            close()
        }
        drawPath(leftFace, leftFaceColor)

        val rightFace = Path().apply {
            moveTo(center.x, topCenterY + ry)
            lineTo(center.x + rx, topCenterY)
            lineTo(center.x + rx, currentBaseY)
            lineTo(center.x, currentBaseY + ry)
            close()
        }
        drawPath(rightFace, rightFaceColor)

        val topFace = Path().apply {
            moveTo(center.x, topCenterY - ry)
            lineTo(center.x + rx, topCenterY)
            lineTo(center.x, topCenterY + ry)
            lineTo(center.x - rx, topCenterY)
            close()
        }
        drawPath(topFace, topFaceColor)

        drawLine(
            color = Color.White.copy(alpha = 0.25f),
            start = Offset(center.x - rx, topCenterY),
            end = Offset(center.x, topCenterY + ry),
            strokeWidth = 1.2f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.35f),
            start = Offset(center.x, topCenterY + ry),
            end = Offset(center.x + rx, topCenterY),
            strokeWidth = 1.2f
        )

        currentBaseY = topCenterY
    }

    val apexRatio = 1f - tierCount * (0.68f / tierCount)
    val apexRx = baseRadiusX * apexRatio
    val apexRy = baseRadiusY * apexRatio
    val apexHeight = stepHeight * 1.45f
    val apexTip = Offset(center.x, currentBaseY - apexHeight)

    val apexLeftColor = shadeColor(baseColor, 1.12f)
    val apexRightColor = shadeColor(baseColor, 1.38f)

    val apexLeftFace = Path().apply {
        moveTo(apexTip.x, apexTip.y)
        lineTo(center.x - apexRx, currentBaseY)
        lineTo(center.x, currentBaseY + apexRy)
        close()
    }
    drawPath(apexLeftFace, apexLeftColor)

    val apexRightFace = Path().apply {
        moveTo(apexTip.x, apexTip.y)
        lineTo(center.x, currentBaseY + apexRy)
        lineTo(center.x + apexRx, currentBaseY)
        close()
    }
    drawPath(apexRightFace, apexRightColor)
}

private fun DrawScope.drawUnderwaterSubmergedOverlay(
    pyramids: List<Pyramid>,
    gridToScreen: (Float, Float) -> Offset,
    originX: Float,
    originY: Float,
    zoom: Float,
    tileW: Float
) {
    val w = size.width
    val h = size.height

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0x2E38BDF8),
                Color(0x380284C7),
                Color(0x480C4A6E)
            )
        )
    )

    val shiftX = (originX - w * 0.5f) * 0.15f
    for (rayIdx in 0..4) {
        val topX = w * (0.12f + rayIdx * 0.22f) + shiftX * 0.3f
        val beamWidthTop = w * 0.07f
        val beamWidthBottom = w * 0.16f
        val rayPath = Path().apply {
            moveTo(topX, 0f)
            lineTo(topX + beamWidthTop, 0f)
            lineTo(topX + beamWidthBottom + w * 0.12f, h)
            lineTo(topX - w * 0.04f, h)
            close()
        }
        drawPath(
            path = rayPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0x307DD3FC),
                    Color(0x1438BDF8),
                    Color.Transparent
                )
            )
        )
    }

    for ((idx, pyr) in pyramids.withIndex()) {
        val center = gridToScreen(pyr.gridX, pyr.gridY)
        val spread = tileW * 0.28f
        val bubbleOffsets = listOf(
            Offset(-spread * 0.55f, -spread * 0.35f),
            Offset(spread * 0.45f, -spread * 0.65f),
            Offset(-spread * 0.18f, -spread * 0.95f),
            Offset(spread * 0.22f, -spread * 1.25f),
            Offset(-spread * 0.35f, -spread * 1.45f)
        )
        for ((bIdx, rel) in bubbleOffsets.withIndex()) {
            val radius = (3.2f + ((idx + bIdx) % 3) * 2.1f) * zoom.coerceIn(0.75f, 1.35f)
            val bx = center.x + rel.x
            val by = center.y + rel.y
            drawCircle(
                color = Color(0x99E0F2FE),
                radius = radius,
                center = Offset(bx, by),
                style = Stroke(width = 1.4f)
            )
            drawCircle(
                color = Color(0x44BAE6FD),
                radius = radius * 0.7f,
                center = Offset(bx - radius * 0.2f, by - radius * 0.2f)
            )
        }
    }
}
