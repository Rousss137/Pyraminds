package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.FloorStatus
import com.example.data.model.FloorWithTopics
import com.example.data.model.PYRAMID_COLOR_PALETTE
import com.example.data.model.PyramidDetail
import com.example.data.model.Topic
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusCompletedGreen
import com.example.ui.theme.StatusInProgressAmber

private const val APEX_BASE_FRACTION = 0.30f

@Composable
fun PyramidDetailScreen(
    detail: PyramidDetail,
    onBackToBoard: () -> Unit,
    onAddFloorAbove: () -> Unit,
    onUpdatePyramidColor: (String) -> Unit,
    onEditPyramidTitle: () -> Unit,
    onEditFloorTitle: (floorId: String, levelNumber: Long, currentTitle: String) -> Unit,
    onEditTopic: (Topic) -> Unit,
    onOpenAddTopicModal: (floorId: String, levelNumber: Long, floorTitle: String) -> Unit,
    onToggleTopic: (topicId: String) -> Unit,
    onLockedFloorTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackToBoard()
    }

    val pyramid = detail.pyramid
    val baseColor = parseHexColor(pyramid.baseColor)
    var showColorPicker by remember { mutableStateOf(false) }

    // Display floors from top (highest levelNumber) down to bottom (Floor 1)
    val floorsBottomUp = detail.floorsWithTopics.sortedBy { it.floor.levelNumber }
    val floorsTopDown = floorsBottomUp.asReversed()
    val totalFloors = floorsTopDown.size.coerceAtLeast(1)

    // Precompute which floors have all lower floors 100% completed
    val unlockedFloorIds = mutableSetOf<String>()
    var allLowerDoneSoFar = true
    for (flrItem in floorsBottomUp) {
        if (allLowerDoneSoFar) {
            unlockedFloorIds.add(flrItem.floor.id)
        }
        val isThisFloorCompleted = flrItem.topics.isNotEmpty() && flrItem.topics.all { it.isCompleted }
        if (!isThisFloorCompleted) {
            allLowerDoneSoFar = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF101520),
                        Color(0xFF161D2B),
                        Color(0xFF0E131D)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("pyramid_detail_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Navigation Row: "< Назад к доске" + "Цвет пирамиды" toggle
            item(key = "header_nav") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF252C3B),
                        modifier = Modifier
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onBackToBoard() }
                            .testTag("back_to_board_button")
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.back_to_board),
                                color = Color(0xFFE2E8F0),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF252C3B),
                        modifier = Modifier
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, baseColor.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                            .clickable { showColorPicker = !showColorPicker }
                            .testTag("toggle_detail_color_picker_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(baseColor)
                                    .border(1.dp, Color.White, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Цвет (10)",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Pyramid Title (with Edit button) & Total Weight + "+ Добавить этаж выше"
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.pyramid_prefix, pyramid.title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF252C3B),
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
                                .clickable { onEditPyramidTitle() }
                                .testTag("edit_pyramid_title_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Редактировать название пирамиды",
                                    tint = Color(0xFF93C5FD),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.total_weight_format, pyramid.totalWeight),
                            color = Color(0xFF94A3B8),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.testTag("detail_total_weight")
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF252C3B),
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.14f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onAddFloorAbove() }
                                .testTag("add_floor_above_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Добавить этаж выше",
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // 10-Color Palette Bar in Pyramid Detail View
                    AnimatedVisibility(visible = showColorPicker) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1C2434),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.10f),
                                    RoundedCornerShape(12.dp)
                                )
                                .testTag("detail_color_palette_bar")
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Text(
                                    text = "Цвет пирамиды (10 цветов):",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for ((hex, colorName) in PYRAMID_COLOR_PALETTE) {
                                        val swatchColor = parseHexColor(hex)
                                        val isSelected = pyramid.baseColor.equals(hex, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(swatchColor)
                                                .border(
                                                    width = if (isSelected) 2.5.dp else 1.dp,
                                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.32f),
                                                    shape = CircleShape
                                                )
                                                .clickable { onUpdatePyramidColor(hex) }
                                                .testTag("detail_color_${hex.removePrefix("#")}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = colorName,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 3. Pointed Triangular Apex ("Вершина [Ожидает]") forming the top tip of the Triangle
            item(key = "pyramid_apex") {
                PyramidApexHeader(
                    baseWidthFraction = APEX_BASE_FRACTION,
                    baseColor = baseColor,
                    isAllCompleted = allLowerDoneSoFar
                )
            }

            // 4. Continuous Triangular Pyramid Floors from Top to Bottom
            itemsIndexed(
                items = floorsTopDown,
                key = { _, item -> item.floor.id }
            ) { indexFromTop, floorWithTopics ->
                val span = 1.0f - APEX_BASE_FRACTION
                val topFraction = APEX_BASE_FRACTION + (indexFromTop.toFloat() / totalFloors) * span
                val bottomFraction = APEX_BASE_FRACTION + ((indexFromTop + 1).toFloat() / totalFloors) * span
                val isFloorUnlocked = floorWithTopics.floor.id in unlockedFloorIds

                TriangularFloorSliceItem(
                    floorWithTopics = floorWithTopics,
                    baseColor = baseColor,
                    topWidthFraction = topFraction,
                    bottomWidthFraction = bottomFraction,
                    isUnlocked = isFloorUnlocked,
                    onToggleTopic = { topicId ->
                        if (isFloorUnlocked) {
                            onToggleTopic(topicId)
                        } else {
                            onLockedFloorTap()
                        }
                    },
                    onEditFloorTitle = {
                        onEditFloorTitle(
                            floorWithTopics.floor.id,
                            floorWithTopics.floor.levelNumber,
                            floorWithTopics.floor.title
                        )
                    },
                    onEditTopic = onEditTopic,
                    onAddTopic = {
                        onOpenAddTopicModal(
                            floorWithTopics.floor.id,
                            floorWithTopics.floor.levelNumber,
                            floorWithTopics.floor.title
                        )
                    }
                )
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}

@Composable
private fun PyramidApexHeader(
    baseWidthFraction: Float,
    baseColor: Color,
    isAllCompleted: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(98.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val triBaseWidth = w * baseWidthFraction
            val cx = w * 0.5f

            val path = Path().apply {
                moveTo(cx, 2f)
                lineTo(cx + triBaseWidth * 0.5f, h)
                lineTo(cx - triBaseWidth * 0.5f, h)
                close()
            }

            val topColors = if (isAllCompleted) {
                listOf(shadeColor(baseColor, 1.35f), shadeColor(baseColor, 1.05f))
            } else {
                listOf(Color(0xFF2B3242), Color(0xFF222836))
            }

            drawPath(
                path = path,
                brush = Brush.verticalGradient(colors = topColors)
            )

            val strokeColor = if (isAllCompleted) {
                shadeColor(baseColor, 1.45f)
            } else {
                Color(0xFF475569).copy(alpha = 0.65f)
            }

            drawLine(
                color = strokeColor,
                start = Offset(cx, 2f),
                end = Offset(cx - triBaseWidth * 0.5f, h),
                strokeWidth = 2.2f
            )
            drawLine(
                color = strokeColor,
                start = Offset(cx, 2f),
                end = Offset(cx + triBaseWidth * 0.5f, h),
                strokeWidth = 2.2f
            )
            drawLine(
                color = strokeColor.copy(alpha = 0.45f),
                start = Offset(cx - triBaseWidth * 0.5f, h),
                end = Offset(cx + triBaseWidth * 0.5f, h),
                strokeWidth = 1.5f
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 28.dp)
        ) {
            Icon(
                imageVector = if (isAllCompleted) Icons.Default.Check else Icons.Default.Lock,
                contentDescription = null,
                tint = if (isAllCompleted) Color.White else Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.apex_title),
                color = if (isAllCompleted) Color.White else Color(0xFFCBD5E1),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isAllCompleted) "[Открыта]" else stringResource(R.string.apex_waiting),
                color = if (isAllCompleted) Color(0xFFE2E8F0) else Color(0xFF64748B),
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun TriangularFloorSliceItem(
    floorWithTopics: FloorWithTopics,
    baseColor: Color,
    topWidthFraction: Float,
    bottomWidthFraction: Float,
    isUnlocked: Boolean,
    onToggleTopic: (String) -> Unit,
    onEditFloorTitle: () -> Unit,
    onEditTopic: (Topic) -> Unit,
    onAddTopic: () -> Unit
) {
    val floor = floorWithTopics.floor
    val topics = floorWithTopics.topics

    val isCompleted = isUnlocked && floor.status == FloorStatus.COMPLETED
    val isInProgress = isUnlocked && !isCompleted
    val isLocked = !isUnlocked

    val (tierFillTop, tierFillBottom, borderColor) = when {
        isInProgress -> Triple(
            shadeColor(baseColor, 1.08f),
            shadeColor(baseColor, 0.86f),
            shadeColor(baseColor, 1.38f)
        )
        isCompleted -> {
            val depthFactor = if (floor.levelNumber <= 1L) 0.38f else 0.54f
            Triple(
                shadeColor(baseColor, depthFactor + 0.08f),
                shadeColor(baseColor, depthFactor),
                shadeColor(baseColor, depthFactor + 0.28f)
            )
        }
        else -> Triple(
            Color(0xFF282F3D),
            Color(0xFF1F2531),
            Color(0xFF475569)
        )
    }

    val verticalPad = when {
        isInProgress -> 16.dp
        isCompleted -> 11.dp
        else -> 10.dp
    }

    // Equal vertical distance between all tasks on the same floor
    val topicRowSpacing = 8.dp

    val avgSliceFraction = ((topWidthFraction + bottomWidthFraction) * 0.5f).coerceIn(0.42f, 0.94f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .drawBehind {
                val w = size.width
                val h = size.height
                val topHalf = (w * topWidthFraction) * 0.5f
                val bottomHalf = (w * bottomWidthFraction) * 0.5f
                val cx = w * 0.5f

                val trapPath = Path().apply {
                    moveTo(cx - topHalf, 0f)
                    lineTo(cx + topHalf, 0f)
                    lineTo(cx + bottomHalf, h)
                    lineTo(cx - bottomHalf, h)
                    close()
                }

                drawPath(
                    path = trapPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(tierFillTop, tierFillBottom)
                    )
                )

                drawLine(
                    color = borderColor.copy(alpha = if (isInProgress) 0.9f else 0.5f),
                    start = Offset(cx - topHalf, 1f),
                    end = Offset(cx + topHalf, 1f),
                    strokeWidth = if (isInProgress) 3.5f else 1.8f
                )

                val edgeAlpha = if (isInProgress) 0.95f else 0.65f
                drawLine(
                    color = borderColor.copy(alpha = edgeAlpha),
                    start = Offset(cx - topHalf, 0f),
                    end = Offset(cx - bottomHalf, h),
                    strokeWidth = 2.4f
                )
                drawLine(
                    color = borderColor.copy(alpha = edgeAlpha),
                    start = Offset(cx + topHalf, 0f),
                    end = Offset(cx + bottomHalf, h),
                    strokeWidth = 2.4f
                )

                if (floor.levelNumber == 1L) {
                    drawLine(
                        color = borderColor.copy(alpha = 0.7f),
                        start = Offset(cx - bottomHalf, h - 1.5f),
                        end = Offset(cx + bottomHalf, h - 1.5f),
                        strokeWidth = 2.5f
                    )
                }
            }
            .padding(vertical = verticalPad, horizontal = 8.dp)
            .testTag("floor_tier_${floor.levelNumber}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(avgSliceFraction),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val effectiveStatus = when {
                isCompleted -> FloorStatus.COMPLETED
                isInProgress -> FloorStatus.IN_PROGRESS
                else -> FloorStatus.LOCKED
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = floor.title,
                        color = if (isLocked) Color(0xFFCBD5E1) else Color.White,
                        fontSize = if (isInProgress) 15.sp else 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.25f))
                            .clickable { onEditFloorTitle() }
                            .testTag("edit_floor_title_${floor.levelNumber}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Редактировать название этажа",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                FloorStatusBadge(status = effectiveStatus)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(topicRowSpacing),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                for (topic in topics) {
                    TopicCheckboxRow(
                        topic = topic,
                        isFloorLocked = isLocked,
                        onToggle = { onToggleTopic(topic.id) },
                        onEdit = { onEditTopic(topic) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0x660B111E),
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
                    .clickable { onAddTopic() }
                    .testTag("add_topic_floor_${floor.levelNumber}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.add_topic_btn),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun FloorStatusBadge(status: String) {
    val (bgColor, textColor, showLock) = when (status) {
        FloorStatus.COMPLETED -> Triple(StatusCompletedGreen, Color(0xFF062814), false)
        FloorStatus.IN_PROGRESS -> Triple(StatusInProgressAmber, Color(0xFF2B1C00), false)
        else -> Triple(Color(0xFF3E4552), Color(0xFFD1D5DB), true)
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showLock) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = status,
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TopicCheckboxRow(
    topic: Topic,
    isFloorLocked: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0x3D0B111E),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = 1.dp,
                color = if (topic.isCompleted && !isFloorLocked) {
                    Color.White.copy(alpha = 0.35f)
                } else {
                    Color.White.copy(alpha = 0.14f)
                },
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onToggle() }
            .testTag("topic_row_${topic.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        when {
                            isFloorLocked -> Color.White.copy(alpha = 0.08f)
                            topic.isCompleted -> Color.White.copy(alpha = 0.24f)
                            else -> Color.Transparent
                        }
                    )
                    .border(
                        width = 1.5.dp,
                        color = when {
                            isFloorLocked -> Color.White.copy(alpha = 0.35f)
                            topic.isCompleted -> Color.White
                            else -> Color.White.copy(alpha = 0.75f)
                        },
                        shape = RoundedCornerShape(5.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isFloorLocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Этаж заблокирован",
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(11.dp)
                    )
                } else if (topic.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = topic.title,
                color = if (isFloorLocked) {
                    Color.White.copy(alpha = 0.65f)
                } else {
                    Color.White
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 16.sp,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = stringResource(R.string.weight_bracket_format, topic.weight),
                color = if (isFloorLocked) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.92f),
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable { onEdit() }
                    .testTag("edit_topic_${topic.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Редактировать задачу",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
