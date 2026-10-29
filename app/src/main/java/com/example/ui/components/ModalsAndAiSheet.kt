package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FieldBiome
import com.example.data.model.FieldBoard
import com.example.data.model.NewFloorDraft
import com.example.data.model.PYRAMID_COLOR_PALETTE
import com.example.ui.screens.parseHexColor
import com.example.ui.theme.JetBrainsMonoFontFamily

private class EditableFloorDraftState(
    initialTitle: String,
    initialTopic: String,
    initialWeight: String
) {
    var title by mutableStateOf(initialTitle)
    var topic by mutableStateOf(initialTopic)
    var weightText by mutableStateOf(initialWeight)
}

@Composable
fun CreatePyramidDialog(
    initialGridX: Float,
    initialGridY: Float,
    onDismiss: () -> Unit,
    onCreate: (
        title: String,
        baseColor: String,
        gridX: Float,
        gridY: Float,
        floors: List<NewFloorDraft>
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedHex by remember { mutableStateOf(PYRAMID_COLOR_PALETTE.first().first) }

    val floorDrafts = remember {
        mutableStateListOf(
            EditableFloorDraftState(
                initialTitle = "Этаж 1: База (Точка А)",
                initialTopic = "Определить стартовую точку А и базовые навыки",
                initialWeight = "3"
            ),
            EditableFloorDraftState(
                initialTitle = "Этаж 2: Первая практика",
                initialTopic = "Применить базовые знания на реальной задаче",
                initialWeight = "5"
            )
        )
    }

    var showPlanningWhyDialog by remember { mutableStateOf(false) }

    val isFormValid = title.isNotBlank() &&
        floorDrafts.size >= 2 &&
        floorDrafts.all { it.title.isNotBlank() && it.topic.isNotBlank() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF171F2E),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFE2E8F0),
        title = {
            Column {
                Text(
                    text = "Создать пирамиду личностного роста",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Минимум 2 этажа обязательно • можно добавить больше этажей",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E2D46),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .clickable { showPlanningWhyDialog = true }
                        .testTag("why_plan_ahead_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Почему нужно планировать наперёд?",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Нажмите, чтобы узнать, зачем сразу задавать 2+ этажа и явные шаги",
                                color = Color(0xFF93C5FD),
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название пирамиды (долгосрочная цель) *") },
                    placeholder = { Text("Например: Системный дизайн") },
                    singleLine = true,
                    colors = dialogTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_pyramid_title_input")
                )

                Text(
                    text = "Цвет пирамиды (10 оттенков):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFCBD5E1)
                )

                PyramidColorGridSelector(
                    selectedHex = selectedHex,
                    onSelectHex = { selectedHex = it }
                )

                floorDrafts.forEachIndexed { index, draft ->
                    val floorNum = index + 1
                    val isMandatory = index < 2
                    val accentColor = when (index) {
                        0 -> Color(0xFF2ECC71)
                        1 -> Color(0xFFF59E0B)
                        else -> Color(0xFF38BDF8)
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF212B3E),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, accentColor.copy(alpha = 0.42f), RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isMandatory) {
                                        "ЭТАЖ $floorNum (Обязательно)"
                                    } else {
                                        "ЭТАЖ $floorNum (Дополнительный)"
                                    },
                                    color = accentColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )

                                if (!isMandatory) {
                                    IconButton(
                                        onClick = { floorDrafts.removeAt(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Удалить этаж $floorNum",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = draft.title,
                                onValueChange = { draft.title = it },
                                label = { Text("Название $floorNum-го этажа *") },
                                singleLine = true,
                                colors = dialogTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("create_floor${floorNum}_title_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = draft.topic,
                                    onValueChange = { draft.topic = it },
                                    label = { Text("Первый явный шаг $floorNum-го этажа *") },
                                    singleLine = true,
                                    colors = dialogTextFieldColors(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("create_floor${floorNum}_topic_input")
                                )
                                OutlinedTextField(
                                    value = draft.weightText,
                                    onValueChange = { draft.weightText = it },
                                    label = { Text("Вес") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = dialogTextFieldColors(),
                                    modifier = Modifier.width(74.dp)
                                )
                            }
                        }
                    }
                }

                // "+ Добавить этаж" button inside CreatePyramidDialog
                OutlinedButton(
                    onClick = {
                        val nextNum = floorDrafts.size + 1
                        floorDrafts.add(
                            EditableFloorDraftState(
                                initialTitle = "Этаж $nextNum: Новый рубеж",
                                initialTopic = "Ключевая задача $nextNum-го этажа",
                                initialWeight = (nextNum * 3).coerceAtMost(25).toString()
                            )
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("create_dialog_add_floor_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Добавить этаж (Этаж ${floorDrafts.size + 1})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val builtFloors = floorDrafts.mapIndexed { idx, item ->
                        NewFloorDraft(
                            title = item.title.trim(),
                            firstTopicTitle = item.topic.trim(),
                            firstTopicWeight = item.weightText.toLongOrNull()?.coerceIn(1L, 1000L)
                                ?: ((idx + 1) * 3L)
                        )
                    }
                    onCreate(
                        title.trim(),
                        selectedHex,
                        initialGridX,
                        initialGridY,
                        builtFloors
                    )
                },
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(containerColor = parseHexColor(selectedHex)),
                modifier = Modifier.testTag("confirm_create_pyramid_button")
            ) {
                Text(
                    text = "Создать (${floorDrafts.size} эт.)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = Color(0xFF94A3B8))
            }
        }
    )

    if (showPlanningWhyDialog) {
        GoalPlanningExplanationDialog(
            onDismiss = { showPlanningWhyDialog = false }
        )
    }
}

@Composable
fun PyramidColorGridSelector(
    selectedHex: String,
    onSelectHex: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (rowItems in PYRAMID_COLOR_PALETTE.chunked(5)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for ((hex, label) in rowItems) {
                    val c = parseHexColor(hex)
                    val isSelected = selectedHex.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.32f),
                                shape = CircleShape
                            )
                            .clickable { onSelectHex(hex) }
                            .testTag("color_swatch_${hex.removePrefix("#")}"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = label,
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

@Composable
fun GoalPlanningExplanationDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141C2B),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFE2E8F0),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Зачем планировать наперёд и задавать явные шаги?",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    lineHeight = 22.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExplanationStepCard(
                    number = "1",
                    title = "Связь «Точки А» и горизонта роста",
                    body = "Один этаж фиксирует лишь текущее состояние, но не создаёт движения. Когда вы сразу задаёте Этаж 1 (базу) и Этаж 2+ (следующие ступени), мозг видит не просто рутину, а маршрут: зачем именно нужно освоить базовые шаги."
                )
                ExplanationStepCard(
                    number = "2",
                    title = "Бесшовный переход без потери темпа",
                    body = "Большинство долгосрочных целей забрасываются в момент, когда первый этап завершён, а план следующего ещё не придуман. Заранее созданный Этаж 2 мгновенно разблокируется после выполнения базы, сохраняя ваш импульс."
                )
                ExplanationStepCard(
                    number = "3",
                    title = "Явные шаги против прокрастинации",
                    body = "Абстрактные цели («выучить программирование», «привести себя в форму») вызывают тревогу из-за неопределённости. Конкретные проверяемые задачи с весом сложности [Вес: X] делают прогресс осязаемым и честным."
                )
                ExplanationStepCard(
                    number = "4",
                    title = "Фундамент перед вершиной",
                    body = "В «Пирамидах личностного роста» верхние этажи заблокированы, пока не закрыты нижние. Это защищает от распыления внимания и гарантирует, что каждый новый уровень опирается на 100% освоенную базу."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2997FF)),
                modifier = Modifier.testTag("close_why_plan_ahead_button")
            ) {
                Text("Понятно, к планированию!", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun ExplanationStepCard(
    number: String,
    title: String,
    body: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1F293D),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2997FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = number,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = body,
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
fun FieldSettingsDialog(
    activeBoard: FieldBoard,
    onUpdateBoard: (boardId: String, newTitle: String, newBiome: FieldBiome) -> Unit,
    onDismiss: () -> Unit
) {
    var boardTitle by remember(activeBoard.id) { mutableStateOf(activeBoard.title) }
    var selectedBiome by remember(activeBoard.id, activeBoard.biomeId) {
        mutableStateOf(activeBoard.biome)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF171F2E),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFE2E8F0),
        title = {
            Column {
                Text(
                    text = "Настройка поля",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp
                )
                Text(
                    text = "Индивидуальный вид для «${activeBoard.title}»",
                    color = Color(0xFF93C5FD),
                    fontSize = 12.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = boardTitle,
                    onValueChange = { boardTitle = it },
                    label = { Text("Название бесконечного поля") },
                    singleLine = true,
                    colors = dialogTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("board_title_input")
                )

                Text(
                    text = "Тема локации этого поля:",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                BiomeOptionList(
                    selectedBiome = selectedBiome,
                    onSelectBiome = { selectedBiome = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpdateBoard(activeBoard.id, boardTitle, selectedBiome)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2997FF)),
                modifier = Modifier.testTag("save_board_settings_button")
            ) {
                Text("Сохранить вид", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
fun CreateBoardDialog(
    onDismiss: () -> Unit,
    onCreateBoard: (title: String, biome: FieldBiome) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedBiome by remember { mutableStateOf(FieldBiome.SNOW) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF171F2E),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFE2E8F0),
        title = {
            Text(
                text = "Новое бесконечное поле",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 19.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название поля (например: Физическая подготовка)") },
                    singleLine = true,
                    colors = dialogTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_board_title_input")
                )

                Text(
                    text = "Выберите тему локации:",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                BiomeOptionList(
                    selectedBiome = selectedBiome,
                    onSelectBiome = { selectedBiome = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreateBoard(title.ifBlank { "Новое поле" }, selectedBiome)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2997FF)),
                modifier = Modifier.testTag("confirm_create_board_button")
            ) {
                Text("Создать поле", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
private fun BiomeOptionList(
    selectedBiome: FieldBiome,
    onSelectBiome: (FieldBiome) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (biome in FieldBiome.entries) {
            val isSelected = biome == selectedBiome
            val previewColors = when (biome) {
                FieldBiome.DESERT -> listOf(Color(0xFFF6CE9B), Color(0xFFD5934E))
                FieldBiome.SNOW -> listOf(Color(0xFFEFF6FF), Color(0xFF93C5FD))
                FieldBiome.GRASS -> listOf(Color(0xFF86EFAC), Color(0xFF16A34A))
                FieldBiome.UNDERWATER -> listOf(Color(0xFF0EA5E9), Color(0xFF0F172A))
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Color(0xFF24344D) else Color(0xFF1E2636),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelectBiome(biome) }
                    .testTag("biome_option_${biome.id}")
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(previewColors))
                            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = biome.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = biome.subtitle,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddFloorDialog(
    nextLevelNumber: Long,
    onDismiss: () -> Unit,
    onConfirm: (customTitle: String) -> Unit
) {
    var title by remember { mutableStateOf("Этаж $nextLevelNumber: Проектирование & деплой") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A2232),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFE2E8F0),
        title = {
            Text(
                text = "+ Добавить этаж выше",
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Новый ярус будет добавлен ближе к вершине треугольной пирамиды (Этаж $nextLevelNumber).",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название этажа") },
                    singleLine = true,
                    colors = dialogTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_floor_title_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2997FF)),
                modifier = Modifier.testTag("confirm_add_floor_button")
            ) {
                Text("Добавить этаж", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
fun AddTopicDialog(
    floorTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, weight: Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedWeight by remember { mutableLongStateOf(5L) }
    var customWeightText by remember { mutableStateOf("5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A2232),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFE2E8F0),
        title = {
            Text(
                text = "+ Добавить тему",
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = floorTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6ED3FF)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название задачи / темы") },
                    placeholder = { Text("Например: Спроектировать индексы БД") },
                    singleLine = true,
                    colors = dialogTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_topic_title_input")
                )

                Text(
                    text = "Вес сложности [Вес: X]:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFCBD5E1)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (preset in listOf(1L, 2L, 3L, 5L, 8L, 12L)) {
                        val isSelected = selectedWeight == preset
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF2997FF) else Color(0xFF273248),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedWeight = preset
                                    customWeightText = preset.toString()
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$preset",
                                    color = Color.White,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = customWeightText,
                    onValueChange = {
                        customWeightText = it
                        it.toLongOrNull()?.let { v -> selectedWeight = v.coerceIn(1L, 1000L) }
                    },
                    label = { Text("Точный вес (1 – 1000)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = dialogTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_topic_weight_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = customWeightText.toLongOrNull()?.coerceIn(1L, 1000L) ?: selectedWeight
                    if (title.isNotBlank()) {
                        onConfirm(title.trim(), w)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2997FF)),
                modifier = Modifier.testTag("confirm_add_topic_button")
            ) {
                Text("Добавить", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
fun EditPyramidTitleDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (newTitle: String) -> Unit
) {
    var title by remember(currentTitle) { mutableStateOf(currentTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A2232),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFE2E8F0),
        title = {
            Text(
                text = "Редактировать название пирамиды",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название пирамиды (долгосрочная цель)") },
                    singleLine = true,
                    colors = dialogTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_pyramid_title_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title.trim())
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2997FF)),
                modifier = Modifier.testTag("confirm_edit_pyramid_title_button")
            ) {
                Text("Сохранить", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
fun EditFloorTitleDialog(
    levelNumber: Long,
    currentTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (newTitle: String) -> Unit
) {
    var title by remember(currentTitle) { mutableStateOf(currentTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A2232),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFE2E8F0),
        title = {
            Text(
                text = "Редактировать название этажа $levelNumber",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название этажа") },
                    singleLine = true,
                    colors = dialogTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_floor_title_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title.trim())
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2997FF)),
                modifier = Modifier.testTag("confirm_edit_floor_title_button")
            ) {
                Text("Сохранить", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
fun EditTopicDialog(
    currentTitle: String,
    currentWeight: Long,
    onDismiss: () -> Unit,
    onConfirm: (newTitle: String, newWeight: Long) -> Unit
) {
    var title by remember(currentTitle) { mutableStateOf(currentTitle) }
    var selectedWeight by remember(currentWeight) { mutableLongStateOf(currentWeight) }
    var customWeightText by remember(currentWeight) { mutableStateOf(currentWeight.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A2232),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFE2E8F0),
        title = {
            Text(
                text = "Редактировать цель / задачу",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название цели / задачи") },
                    singleLine = true,
                    colors = dialogTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_topic_title_input")
                )

                Text(
                    text = "Вес сложности [Вес: X]:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFCBD5E1)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (preset in listOf(1L, 2L, 3L, 5L, 8L, 12L)) {
                        val isSelected = selectedWeight == preset
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF2997FF) else Color(0xFF273248),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedWeight = preset
                                    customWeightText = preset.toString()
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$preset",
                                    color = Color.White,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = customWeightText,
                    onValueChange = {
                        customWeightText = it
                        it.toLongOrNull()?.let { v -> selectedWeight = v.coerceIn(1L, 10000L) }
                    },
                    label = { Text("Точный вес (1 – 10000)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = dialogTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_topic_weight_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = customWeightText.toLongOrNull()?.coerceIn(1L, 10000L) ?: selectedWeight
                    if (title.isNotBlank()) {
                        onConfirm(title.trim(), w)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2997FF)),
                modifier = Modifier.testTag("confirm_edit_topic_button")
            ) {
                Text("Сохранить", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
fun BackupRestoreDialog(
    onSaveBackupToFile: () -> Unit,
    onLoadBackupFromFile: (replaceExisting: Boolean) -> Unit,
    onGenerateBackupText: ((String) -> Unit) -> Unit,
    onImportFromRawText: (rawJson: String, replaceExisting: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var replaceExisting by remember { mutableStateOf(true) }
    var showRawJsonSection by remember { mutableStateOf(false) }
    var rawJsonText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161E2E),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFE2E8F0),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Backup,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Резервная копия данных",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Pyraminds • Пирамиды личностного роста",
                        color = Color(0xFF93C5FD),
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1F2A3E),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = "Формат резервной копии универсален и обратно совместим между версиями приложения: сохранённый файл (.json) содержит все ваши бесконечные поля, пирамиды, этажи и прогресс задач.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // 1. Export to .json file button
                Button(
                    onClick = onSaveBackupToFile,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("export_backup_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Сохранить резервную копию в файл (.json)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Import mode selector
                Text(
                    text = "Режим восстановления при загрузке:",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (replaceExisting) Color(0xFF233858) else Color(0xFF1E2636),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (replaceExisting) 1.6.dp else 1.dp,
                                color = if (replaceExisting) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { replaceExisting = true }
                            .padding(vertical = 8.dp, horizontal = 8.dp)
                    ) {
                        Text(
                            text = "Заменить текущие",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (!replaceExisting) Color(0xFF233858) else Color(0xFF1E2636),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (!replaceExisting) 1.6.dp else 1.dp,
                                color = if (!replaceExisting) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { replaceExisting = false }
                            .padding(vertical = 8.dp, horizontal = 8.dp)
                    ) {
                        Text(
                            text = "Объединить (добавить)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 2. Import from .json file button
                OutlinedButton(
                    onClick = { onLoadBackupFromFile(replaceExisting) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("import_backup_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Загрузить резервную копию из файла",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Optional text fallback (copy/paste JSON directly)
                TextButton(
                    onClick = {
                        showRawJsonSection = !showRawJsonSection
                        if (showRawJsonSection && rawJsonText.isBlank()) {
                            onGenerateBackupText { generated ->
                                rawJsonText = generated
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = if (showRawJsonSection) {
                            "Скрыть текстовый JSON-код копии"
                        } else {
                            "Показать / вставить JSON-текст вручную"
                        },
                        color = Color(0xFF93C5FD),
                        fontSize = 12.sp
                    )
                }

                if (showRawJsonSection) {
                    OutlinedTextField(
                        value = rawJsonText,
                        onValueChange = { rawJsonText = it },
                        label = { Text("JSON-данные резервной копии") },
                        colors = dialogTextFieldColors(),
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("raw_backup_json_input")
                    )
                    Button(
                        onClick = {
                            if (rawJsonText.isNotBlank()) {
                                onImportFromRawText(rawJsonText, replaceExisting)
                            }
                        },
                        enabled = rawJsonText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restore_from_raw_json_button")
                    ) {
                        Text(
                            text = "Восстановить из вставленного JSON",
                            color = Color(0xFF062814),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть", color = Color(0xFF93C5FD), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun dialogTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Color(0xFF2997FF),
    unfocusedBorderColor = Color(0xFF475569),
    focusedLabelColor = Color(0xFF6ED3FF),
    unfocusedLabelColor = Color(0xFF94A3B8)
)
