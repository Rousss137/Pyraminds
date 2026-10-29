package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.FieldBiome
import com.example.data.model.FieldBoard
import com.example.data.model.Topic
import com.example.data.repository.PyramidRepository
import com.example.ui.components.AddFloorDialog
import com.example.ui.components.AddTopicDialog
import com.example.ui.components.BackupRestoreDialog
import com.example.ui.components.CreateBoardDialog
import com.example.ui.components.CreatePyramidDialog
import com.example.ui.components.EditFloorTitleDialog
import com.example.ui.components.EditPyramidTitleDialog
import com.example.ui.components.EditTopicDialog
import com.example.ui.components.FieldSettingsDialog
import com.example.ui.screens.DesertGridScreen
import com.example.ui.screens.PyramidDetailScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GrowthPyramidsViewModel
import com.example.ui.viewmodel.UiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GrowthPyramidsMainContent()
                }
            }
        }
    }
}

private data class AddTopicTarget(
    val floorId: String,
    val levelNumber: Long,
    val floorTitle: String
)

private data class EditFloorTarget(
    val floorId: String,
    val levelNumber: Long,
    val currentTitle: String
)

@Composable
fun GrowthPyramidsMainContent() {
    val viewModel: GrowthPyramidsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                GrowthPyramidsViewModel(PyramidRepository(app))
            }
        }
    )

    val boards by viewModel.boardsState.collectAsStateWithLifecycle()
    val activeBoardId by viewModel.activeBoardId.collectAsStateWithLifecycle()
    val pyramidsState by viewModel.pyramidsState.collectAsStateWithLifecycle()
    val detailState by viewModel.selectedPyramidDetailState.collectAsStateWithLifecycle()
    val selectedPyramidId by viewModel.selectedPyramidId.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val isMoveMode by viewModel.isMoveMode.collectAsStateWithLifecycle()

    val activeBoard = remember(boards, activeBoardId) {
        boards.firstOrNull { it.id == activeBoardId }
            ?: boards.firstOrNull()
            ?: FieldBoard(
                id = "board_main",
                title = "Главное поле",
                biomeId = FieldBiome.DESERT.id,
                orderIndex = 0L
            )
    }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val lockedFloorWarning = stringResource(R.string.locked_floor_warning)

    var createModalCoords by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var showAddFloorModal by remember { mutableStateOf(false) }
    var addTopicTarget by remember { mutableStateOf<AddTopicTarget?>(null) }
    var settingsTargetBoard by remember { mutableStateOf<FieldBoard?>(null) }
    var showCreateBoardModal by remember { mutableStateOf(false) }
    var showBackupModal by remember { mutableStateOf(false) }

    var showEditPyramidTitleModal by remember { mutableStateOf(false) }
    var editFloorTarget by remember { mutableStateOf<EditFloorTarget?>(null) }
    var editTopicTarget by remember { mutableStateOf<Topic?>(null) }

    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    var pendingImportReplaceMode by remember { mutableStateOf(true) }

    val saveBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val jsonToSave = pendingExportJson
        if (uri != null && jsonToSave != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(jsonToSave.toByteArray(Charsets.UTF_8))
                }
                viewModel.showMessage("Резервная копия успешно сохранена в файл")
                showBackupModal = false
            } catch (e: Exception) {
                viewModel.showMessage("Не удалось сохранить файл: ${e.localizedMessage ?: "ошибка записи"}")
            }
        }
        pendingExportJson = null
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                }
                if (!content.isNullOrBlank()) {
                    viewModel.importBackupJson(
                        rawJson = content,
                        replaceExisting = pendingImportReplaceMode,
                        onCompleted = { showBackupModal = false }
                    )
                } else {
                    viewModel.showMessage("Выбранный файл пуст")
                }
            } catch (e: Exception) {
                viewModel.showMessage("Не удалось прочитать файл: ${e.localizedMessage ?: "ошибка чтения"}")
            }
        }
    }

    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0E131F)
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isExpandedScreen = maxWidth >= 840.dp
            val pyramids = (pyramidsState as? UiState.Success)?.data ?: emptyList()
            val activeDetail = (detailState as? UiState.Success)?.data

            if (isExpandedScreen && selectedPyramidId != null && activeDetail != null) {
                Row(modifier = Modifier.fillMaxSize()) {
                    DesertGridScreen(
                        boards = boards,
                        activeBoard = activeBoard,
                        pyramids = pyramids,
                        isMoveMode = isMoveMode,
                        onSelectBoard = { viewModel.selectBoard(it) },
                        onOpenBoardSettings = { settingsTargetBoard = it },
                        onOpenCreateBoard = { showCreateBoardModal = true },
                        onOpenBackupDialog = { showBackupModal = true },
                        onSelectPyramid = { viewModel.selectPyramid(it) },
                        onToggleMoveMode = { viewModel.toggleMoveMode() },
                        onMovePyramid = { id, gx, gy -> viewModel.movePyramid(id, gx, gy) },
                        onOpenCreatePyramid = { gx, gy -> createModalCoords = gx to gy },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color.White.copy(alpha = 0.14f))
                    )
                    PyramidDetailScreen(
                        detail = activeDetail,
                        onBackToBoard = { viewModel.selectPyramid(null) },
                        onAddFloorAbove = { showAddFloorModal = true },
                        onUpdatePyramidColor = { newColor ->
                            viewModel.updatePyramidColor(activeDetail.pyramid.id, newColor)
                        },
                        onEditPyramidTitle = { showEditPyramidTitleModal = true },
                        onEditFloorTitle = { fId, lvl, curTitle ->
                            editFloorTarget = EditFloorTarget(fId, lvl, curTitle)
                        },
                        onEditTopic = { topic -> editTopicTarget = topic },
                        onOpenAddTopicModal = { fId, lvl, fTitle ->
                            addTopicTarget = AddTopicTarget(fId, lvl, fTitle)
                        },
                        onToggleTopic = { topicId ->
                            viewModel.toggleTopic(activeDetail.pyramid.id, topicId)
                        },
                        onLockedFloorTap = {
                            viewModel.showMessage(lockedFloorWarning)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            } else {
                Crossfade(
                    targetState = selectedPyramidId,
                    label = "screen_crossfade"
                ) { targetId ->
                    if (targetId == null) {
                        when (val pState = pyramidsState) {
                            is UiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color(0xFF2997FF))
                                }
                            }
                            is UiState.Error -> {
                                DesertGridScreen(
                                    boards = boards,
                                    activeBoard = activeBoard,
                                    pyramids = emptyList(),
                                    isMoveMode = isMoveMode,
                                    onSelectBoard = { viewModel.selectBoard(it) },
                                    onOpenBoardSettings = { settingsTargetBoard = it },
                                    onOpenCreateBoard = { showCreateBoardModal = true },
                                    onOpenBackupDialog = { showBackupModal = true },
                                    onSelectPyramid = { viewModel.selectPyramid(it) },
                                    onToggleMoveMode = { viewModel.toggleMoveMode() },
                                    onMovePyramid = { id, gx, gy -> viewModel.movePyramid(id, gx, gy) },
                                    onOpenCreatePyramid = { gx, gy -> createModalCoords = gx to gy }
                                )
                            }
                            is UiState.Success -> {
                                DesertGridScreen(
                                    boards = boards,
                                    activeBoard = activeBoard,
                                    pyramids = pState.data,
                                    isMoveMode = isMoveMode,
                                    onSelectBoard = { viewModel.selectBoard(it) },
                                    onOpenBoardSettings = { settingsTargetBoard = it },
                                    onOpenCreateBoard = { showCreateBoardModal = true },
                                    onOpenBackupDialog = { showBackupModal = true },
                                    onSelectPyramid = { viewModel.selectPyramid(it) },
                                    onToggleMoveMode = { viewModel.toggleMoveMode() },
                                    onMovePyramid = { id, gx, gy -> viewModel.movePyramid(id, gx, gy) },
                                    onOpenCreatePyramid = { gx, gy -> createModalCoords = gx to gy }
                                )
                            }
                        }
                    } else {
                        when (val dState = detailState) {
                            is UiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color(0xFF2997FF))
                                }
                            }
                            is UiState.Error -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dState.message,
                                        color = Color(0xFFFF8A80)
                                    )
                                }
                            }
                            is UiState.Success -> {
                                val detail = dState.data
                                if (detail != null) {
                                    PyramidDetailScreen(
                                        detail = detail,
                                        onBackToBoard = { viewModel.selectPyramid(null) },
                                        onAddFloorAbove = { showAddFloorModal = true },
                                        onUpdatePyramidColor = { newColor ->
                                            viewModel.updatePyramidColor(detail.pyramid.id, newColor)
                                        },
                                        onEditPyramidTitle = { showEditPyramidTitleModal = true },
                                        onEditFloorTitle = { fId, lvl, curTitle ->
                                            editFloorTarget = EditFloorTarget(fId, lvl, curTitle)
                                        },
                                        onEditTopic = { topic -> editTopicTarget = topic },
                                        onOpenAddTopicModal = { fId, lvl, fTitle ->
                                            addTopicTarget = AddTopicTarget(fId, lvl, fTitle)
                                        },
                                        onToggleTopic = { topicId ->
                                            viewModel.toggleTopic(detail.pyramid.id, topicId)
                                        },
                                        onLockedFloorTap = {
                                            viewModel.showMessage(lockedFloorWarning)
                                        }
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = Color(0xFF2997FF))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modals
    createModalCoords?.let { (gx, gy) ->
        CreatePyramidDialog(
            initialGridX = gx,
            initialGridY = gy,
            onDismiss = { createModalCoords = null },
            onCreate = { title, color, x, y, floors ->
                createModalCoords = null
                viewModel.createPyramidWithFloors(
                    title = title,
                    baseColor = color,
                    gridX = x,
                    gridY = y,
                    floors = floors
                )
            }
        )
    }

    settingsTargetBoard?.let { board ->
        FieldSettingsDialog(
            activeBoard = board,
            onUpdateBoard = { bId, newTitle, newBiome ->
                viewModel.updateBoard(bId, newTitle, newBiome)
            },
            onDismiss = { settingsTargetBoard = null }
        )
    }

    if (showCreateBoardModal) {
        CreateBoardDialog(
            onDismiss = { showCreateBoardModal = false },
            onCreateBoard = { title, biome ->
                viewModel.createBoard(title, biome)
            }
        )
    }

    val currentDetail = (detailState as? UiState.Success)?.data
    if (showAddFloorModal && currentDetail != null) {
        val nextLevel = (currentDetail.floorsWithTopics.maxOfOrNull { it.floor.levelNumber } ?: 0L) + 1L
        AddFloorDialog(
            nextLevelNumber = nextLevel,
            onDismiss = { showAddFloorModal = false },
            onConfirm = { customTitle ->
                showAddFloorModal = false
                viewModel.addFloorAbove(currentDetail.pyramid.id, customTitle)
            }
        )
    }

    addTopicTarget?.let { target ->
        if (currentDetail != null) {
            AddTopicDialog(
                floorTitle = target.floorTitle,
                onDismiss = { addTopicTarget = null },
                onConfirm = { title, weight ->
                    val pId = currentDetail.pyramid.id
                    addTopicTarget = null
                    viewModel.addTopicToFloor(
                        pyramidId = pId,
                        floorId = target.floorId,
                        levelNumber = target.levelNumber,
                        title = title,
                        weight = weight
                    )
                }
            )
        }
    }

    if (showEditPyramidTitleModal && currentDetail != null) {
        EditPyramidTitleDialog(
            currentTitle = currentDetail.pyramid.title,
            onDismiss = { showEditPyramidTitleModal = false },
            onConfirm = { newTitle ->
                showEditPyramidTitleModal = false
                viewModel.updatePyramidTitle(currentDetail.pyramid.id, newTitle)
            }
        )
    }

    editFloorTarget?.let { target ->
        EditFloorTitleDialog(
            levelNumber = target.levelNumber,
            currentTitle = target.currentTitle,
            onDismiss = { editFloorTarget = null },
            onConfirm = { newTitle ->
                val fId = target.floorId
                editFloorTarget = null
                viewModel.updateFloorTitle(fId, newTitle)
            }
        )
    }

    editTopicTarget?.let { topic ->
        if (currentDetail != null) {
            EditTopicDialog(
                currentTitle = topic.title,
                currentWeight = topic.weight,
                onDismiss = { editTopicTarget = null },
                onConfirm = { newTitle, newWeight ->
                    val pId = currentDetail.pyramid.id
                    val tId = topic.id
                    editTopicTarget = null
                    viewModel.updateTopic(
                        pyramidId = pId,
                        topicId = tId,
                        newTitle = newTitle,
                        newWeight = newWeight
                    )
                }
            )
        }
    }

    if (showBackupModal) {
        BackupRestoreDialog(
            onSaveBackupToFile = {
                viewModel.exportBackupJson { json ->
                    pendingExportJson = json
                    saveBackupLauncher.launch("pyraminds_backup.json")
                }
            },
            onLoadBackupFromFile = { replaceExisting ->
                pendingImportReplaceMode = replaceExisting
                openBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            onGenerateBackupText = { onTextReady ->
                viewModel.exportBackupJson(onTextReady)
            },
            onImportFromRawText = { rawJson, replaceExisting ->
                viewModel.importBackupJson(
                    rawJson = rawJson,
                    replaceExisting = replaceExisting,
                    onCompleted = { showBackupModal = false }
                )
            },
            onDismiss = { showBackupModal = false }
        )
    }
}
