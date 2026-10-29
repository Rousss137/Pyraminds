package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.FieldBiome
import com.example.data.model.FieldBoard
import com.example.data.model.NewFloorDraft
import com.example.data.model.Pyramid
import com.example.data.model.PyramidDetail
import com.example.data.repository.PyramidRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class GrowthPyramidsViewModel(
    private val repository: PyramidRepository
) : ViewModel() {

    private val _activeBoardId = MutableStateFlow("board_main")
    val activeBoardId: StateFlow<String> = _activeBoardId.asStateFlow()

    private val _selectedPyramidId = MutableStateFlow<String?>(null)
    val selectedPyramidId: StateFlow<String?> = _selectedPyramidId.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isMoveMode = MutableStateFlow(false)
    val isMoveMode: StateFlow<Boolean> = _isMoveMode.asStateFlow()

    val boardsState: StateFlow<List<FieldBoard>> = repository.observeBoards()
        .catch { error ->
            Log.w(TAG, "Error observing boards", error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = listOf(
                FieldBoard(
                    id = "board_main",
                    title = "Основное поле",
                    biomeId = FieldBiome.DESERT.id,
                    orderIndex = 0L
                )
            )
        )

    val pyramidsState: StateFlow<UiState<List<Pyramid>>> = _activeBoardId
        .flatMapLatest { boardId ->
            repository.observePyramidsForBoard(boardId)
                .map<List<Pyramid>, UiState<List<Pyramid>>> { list ->
                    UiState.Success(list)
                }
                .catch { error ->
                    Log.w(TAG, "Error observing pyramids for board $boardId", error)
                    emit(UiState.Error(error.localizedMessage ?: "Не удалось загрузить пирамиды"))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val selectedPyramidDetailState: StateFlow<UiState<PyramidDetail?>> = _selectedPyramidId
        .flatMapLatest { pyramidId ->
            if (pyramidId == null) {
                flowOf(UiState.Success(null))
            } else {
                repository.observePyramidDetail(pyramidId)
                    .map<PyramidDetail?, UiState<PyramidDetail?>> { detail ->
                        UiState.Success(detail)
                    }
                    .catch { error ->
                        Log.w(TAG, "Error observing pyramid detail", error)
                        emit(UiState.Error(error.localizedMessage ?: "Не удалось загрузить этажи пирамиды"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Success(null)
        )

    init {
        viewModelScope.launch {
            repository.ensureDefaultBoardExists()
        }
    }

    fun selectBoard(boardId: String) {
        _activeBoardId.value = boardId
    }

    fun createBoard(title: String, biome: FieldBiome) {
        viewModelScope.launch {
            val res = repository.createBoard(title, biome)
            res.onSuccess { newBoardId ->
                _activeBoardId.value = newBoardId
                _actionMessage.value = "Поле «${title.trim().ifBlank { "Новое поле" }}» (${biome.title}) создано"
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Не удалось создать поле"
            }
        }
    }

    fun updateBoard(boardId: String, newTitle: String, newBiome: FieldBiome) {
        viewModelScope.launch {
            val res = repository.updateBoard(boardId, newTitle, newBiome)
            res.onSuccess {
                _actionMessage.value = "Настройки поля обновлены: ${newBiome.title}"
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Не удалось обновить поле"
            }
        }
    }

    fun selectPyramid(pyramidId: String?) {
        _selectedPyramidId.value = pyramidId
    }

    fun toggleMoveMode() {
        _isMoveMode.value = !_isMoveMode.value
    }

    fun setMoveMode(enabled: Boolean) {
        _isMoveMode.value = enabled
    }

    fun movePyramid(pyramidId: String, targetGridX: Float, targetGridY: Float) {
        viewModelScope.launch {
            val res = repository.movePyramid(pyramidId, targetGridX, targetGridY)
            res.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Не удалось переместить пирамиду"
            }
        }
    }

    fun updatePyramidColor(pyramidId: String, newBaseColor: String) {
        viewModelScope.launch {
            val res = repository.updatePyramidColor(pyramidId, newBaseColor)
            res.onSuccess {
                _actionMessage.value = "Цвет пирамиды обновлён"
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Не удалось изменить цвет"
            }
        }
    }

    fun updatePyramidTitle(pyramidId: String, newTitle: String) {
        viewModelScope.launch {
            val res = repository.updatePyramidTitle(pyramidId, newTitle)
            res.onSuccess {
                _actionMessage.value = "Название пирамиды обновлено"
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Не удалось переименовать пирамиду"
            }
        }
    }

    fun updateFloorTitle(floorId: String, newTitle: String) {
        viewModelScope.launch {
            val res = repository.updateFloorTitle(floorId, newTitle)
            res.onSuccess {
                _actionMessage.value = "Название этажа обновлено"
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Не удалось переименовать этаж"
            }
        }
    }

    fun updateTopic(
        pyramidId: String,
        topicId: String,
        newTitle: String,
        newWeight: Long
    ) {
        viewModelScope.launch {
            val res = repository.updateTopic(pyramidId, topicId, newTitle, newWeight)
            res.onSuccess {
                _actionMessage.value = "Цель / задача обновлена"
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Не удалось обновить задачу"
            }
        }
    }

    fun exportBackupJson(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val res = repository.exportBackupJson()
            res.onSuccess { json ->
                onReady(json)
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Ошибка создания резервной копии"
            }
        }
    }

    fun importBackupJson(rawJson: String, replaceExisting: Boolean, onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.importBackupJson(rawJson, replaceExisting)
            res.onSuccess { count ->
                _actionMessage.value = "Резервная копия успешно загружена (пирамид: $count)"
                onCompleted()
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Ошибка загрузки резервной копии"
            }
        }
    }

    fun showMessage(message: String) {
        _actionMessage.value = message
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun createPyramidWithFloors(
        title: String,
        baseColor: String,
        gridX: Float,
        gridY: Float,
        floors: List<NewFloorDraft>
    ) {
        if (title.isBlank()) return
        val currentBoardId = _activeBoardId.value
        viewModelScope.launch {
            val result = repository.createPyramidWithFloors(
                boardId = currentBoardId,
                title = title,
                baseColor = baseColor,
                gridX = gridX,
                gridY = gridY,
                floorDrafts = floors
            )
            result.onSuccess {
                _actionMessage.value = "Пирамида «${title.trim()}» (${floors.size} эт.) создана"
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Ошибка создания пирамиды"
            }
        }
    }

    fun addFloorAbove(pyramidId: String, customTitle: String?) {
        viewModelScope.launch {
            val result = repository.addFloorAbove(pyramidId, customTitle)
            result.onSuccess {
                _actionMessage.value = "Новый этаж добавлен выше"
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Не удалось добавить этаж"
            }
        }
    }

    fun addTopicToFloor(
        pyramidId: String,
        floorId: String,
        levelNumber: Long,
        title: String,
        weight: Long
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val result = repository.addTopicToFloor(pyramidId, floorId, levelNumber, title, weight)
            result.onSuccess {
                _actionMessage.value = "Тема «${title.trim()}» [Вес: $weight] добавлена"
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Не удалось добавить тему"
            }
        }
    }

    fun toggleTopic(pyramidId: String, topicId: String) {
        viewModelScope.launch {
            val result = repository.toggleTopic(pyramidId, topicId)
            result.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Ошибка обновления темы"
            }
        }
    }

    private companion object {
        const val TAG = "GrowthPyramidsVM"
    }
}
