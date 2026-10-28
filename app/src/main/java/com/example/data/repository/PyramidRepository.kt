package com.example.data.repository

import android.content.Context
import com.example.data.local.GrowthPyramidsDatabase
import com.example.data.local.PyramidDao
import com.example.data.model.FieldBiome
import com.example.data.model.FieldBoard
import com.example.data.model.Floor
import com.example.data.model.FloorStatus
import com.example.data.model.FloorWithTopics
import com.example.data.model.NewFloorDraft
import com.example.data.model.Pyramid
import com.example.data.model.PyramidDetail
import com.example.data.model.Topic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

class PyramidRepository(private val dao: PyramidDao) {

    constructor(context: Context) : this(
        GrowthPyramidsDatabase.getInstance(context.applicationContext).pyramidDao()
    )

    fun observeBoards(): Flow<List<FieldBoard>> = dao.observeAllBoards()

    fun observePyramidsForBoard(boardId: String): Flow<List<Pyramid>> =
        dao.observePyramidsForBoard(boardId)

    fun observeAllPyramids(): Flow<List<Pyramid>> = dao.observeAllPyramids()

    fun observePyramidDetail(pyramidId: String): Flow<PyramidDetail?> {
        return combine(
            dao.observePyramidById(pyramidId),
            dao.observeFloorsForPyramid(pyramidId),
            dao.observeTopicsForPyramid(pyramidId)
        ) { pyramid, floors, topics ->
            if (pyramid == null) {
                null
            } else {
                val floorsWithTopics = floors.map { floor ->
                    FloorWithTopics(
                        floor = floor,
                        topics = topics.filter { it.floorId == floor.id }
                            .sortedBy { it.orderIndex }
                    )
                }
                PyramidDetail(
                    pyramid = pyramid,
                    floorsWithTopics = floorsWithTopics
                )
            }
        }
    }

    suspend fun ensureDefaultBoardExists(): Result<Unit> {
        return try {
            val boardsCount = dao.countBoards()
            if (boardsCount == 0) {
                dao.insertBoard(
                    FieldBoard(
                        id = "board_main",
                        title = "Основное поле",
                        biomeId = FieldBiome.DESERT.id,
                        orderIndex = 0L
                    )
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createBoard(title: String, biome: FieldBiome): Result<String> {
        return try {
            val count = dao.countBoards()
            val boardId = "board_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val board = FieldBoard(
                id = boardId,
                title = title.trim().ifBlank { "Новое поле" }.take(60),
                biomeId = biome.id,
                orderIndex = count.toLong()
            )
            dao.insertBoard(board)
            Result.success(boardId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBoard(boardId: String, newTitle: String, newBiome: FieldBiome): Result<Unit> {
        return try {
            val existing = dao.getBoardById(boardId)
                ?: return Result.failure(IllegalArgumentException("Поле не найдено"))
            dao.updateBoard(
                existing.copy(
                    title = newTitle.trim().ifBlank { existing.title }.take(60),
                    biomeId = newBiome.id
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePyramidColor(pyramidId: String, newBaseColor: String): Result<Unit> {
        return try {
            val existing = dao.getPyramidById(pyramidId)
                ?: return Result.failure(IllegalArgumentException("Пирамида не найдена"))
            dao.updatePyramid(existing.copy(baseColor = newBaseColor))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPyramidWithFloors(
        boardId: String,
        title: String,
        baseColor: String,
        gridX: Float,
        gridY: Float,
        floorDrafts: List<NewFloorDraft>
    ): Result<String> {
        if (floorDrafts.size < 2) {
            return Result.failure(
                IllegalArgumentException("При создании пирамиды необходимо задать минимум 2 этажа")
            )
        }

        val pyramidId = "pyr_${UUID.randomUUID().toString().replace("-", "").take(16)}"

        return try {
            val pyramid = Pyramid(
                id = pyramidId,
                boardId = boardId,
                title = title.trim().take(120),
                baseColor = baseColor,
                gridX = gridX.coerceIn(-25f, 25f),
                gridY = gridY.coerceIn(-25f, 25f),
                baseWeight = 10L,
                totalWeight = 10L
            )

            val floorsToInsert = mutableListOf<Floor>()
            val topicsToInsert = mutableListOf<Topic>()

            floorDrafts.forEachIndexed { idx, draft ->
                val levelNum = (idx + 1).toLong()
                val floorId = "flr_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                val topicId = "top_${UUID.randomUUID().toString().replace("-", "").take(16)}"

                val rawTitle = draft.title.trim().ifBlank { "Этаж $levelNum" }
                val cleanTitle = if (rawTitle.startsWith("Этаж")) {
                    rawTitle.take(150)
                } else {
                    "Этаж $levelNum: $rawTitle".take(150)
                }

                floorsToInsert.add(
                    Floor(
                        id = floorId,
                        pyramidId = pyramidId,
                        levelNumber = levelNum,
                        title = cleanTitle,
                        status = if (levelNum == 1L) FloorStatus.IN_PROGRESS else FloorStatus.LOCKED
                    )
                )

                topicsToInsert.add(
                    Topic(
                        id = topicId,
                        pyramidId = pyramidId,
                        floorId = floorId,
                        levelNumber = levelNum,
                        title = draft.firstTopicTitle.trim().ifBlank { "Шаг $levelNum" }.take(200),
                        weight = draft.firstTopicWeight.coerceIn(1L, 1000L),
                        isCompleted = false,
                        orderIndex = 0L
                    )
                )
            }

            dao.insertPyramid(pyramid)
            dao.insertFloors(floorsToInsert)
            dao.insertTopics(topicsToInsert)

            Result.success(pyramidId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun movePyramid(
        pyramidId: String,
        targetGridX: Float,
        targetGridY: Float
    ): Result<Unit> {
        return try {
            val current = dao.getPyramidById(pyramidId)
                ?: return Result.failure(IllegalArgumentException("Пирамида не найдена"))
            val clampedX = targetGridX.coerceIn(-12f, 12f)
            val clampedY = targetGridY.coerceIn(-12f, 12f)

            dao.updatePyramid(
                current.copy(
                    gridX = clampedX,
                    gridY = clampedY
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addFloorAbove(pyramidId: String, customTitle: String? = null): Result<String> {
        return try {
            val existingFloors = dao.getFloorsForPyramid(pyramidId).sortedBy { it.levelNumber }
            val existingTopics = dao.getTopicsForPyramid(pyramidId)
            val nextLevel = (existingFloors.maxOfOrNull { it.levelNumber } ?: 0L) + 1L

            val allBelowCompleted = existingFloors.isNotEmpty() && existingFloors.all { flr ->
                val flrTopics = existingTopics.filter { it.floorId == flr.id }
                flrTopics.isNotEmpty() && flrTopics.all { it.isCompleted }
            }
            val newStatus = if (existingFloors.isEmpty() || allBelowCompleted) {
                FloorStatus.IN_PROGRESS
            } else {
                FloorStatus.LOCKED
            }

            val cleanSubTitle = customTitle?.trim()?.takeIf { it.isNotEmpty() } ?: "Новый рубеж"
            val fullTitle = if (cleanSubTitle.startsWith("Этаж")) {
                cleanSubTitle.take(150)
            } else {
                "Этаж $nextLevel: $cleanSubTitle".take(150)
            }

            val floorId = "flr_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val floor = Floor(
                id = floorId,
                pyramidId = pyramidId,
                levelNumber = nextLevel,
                title = fullTitle,
                status = newStatus
            )
            dao.insertFloor(floor)
            recalculatePyramidProgression(pyramidId)
            Result.success(floorId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addTopicToFloor(
        pyramidId: String,
        floorId: String,
        levelNumber: Long,
        title: String,
        weight: Long
    ): Result<String> {
        return try {
            val existingTopics = dao.getTopicsForPyramid(pyramidId)
            val floorTopics = existingTopics.filter { it.floorId == floorId }

            val nextOrder = (floorTopics.maxOfOrNull { it.orderIndex } ?: -1L) + 1L
            val topicId = "top_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val safeWeight = weight.coerceIn(1L, 10000L)

            val topic = Topic(
                id = topicId,
                pyramidId = pyramidId,
                floorId = floorId,
                levelNumber = levelNumber,
                title = title.trim().take(200),
                weight = safeWeight,
                isCompleted = false,
                orderIndex = nextOrder
            )

            dao.insertTopic(topic)
            recalculatePyramidProgression(pyramidId)
            Result.success(topicId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleTopic(pyramidId: String, topicId: String): Result<Unit> {
        return try {
            val currentTopic = dao.getTopicById(topicId)
                ?: return Result.failure(IllegalArgumentException("Тема не найдена"))

            val floors = dao.getFloorsForPyramid(pyramidId).sortedBy { it.levelNumber }
            val allTopics = dao.getTopicsForPyramid(pyramidId)

            val lowerFloors = floors.filter { it.levelNumber < currentTopic.levelNumber }
            for (lowerFloor in lowerFloors) {
                val lowerTopics = allTopics.filter { it.floorId == lowerFloor.id }
                val isLowerFloorComplete = lowerTopics.isNotEmpty() && lowerTopics.all { it.isCompleted }
                if (!isLowerFloorComplete) {
                    return Result.failure(
                        IllegalStateException(
                            "Нельзя выполнять задачи верхних этажей, пока не завершён «${lowerFloor.title}»!"
                        )
                    )
                }
            }

            dao.updateTopic(currentTopic.copy(isCompleted = !currentTopic.isCompleted))
            recalculatePyramidProgression(pyramidId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun recalculatePyramidProgression(pyramidId: String) {
        val pyramid = dao.getPyramidById(pyramidId) ?: return
        val floors = dao.getFloorsForPyramid(pyramidId).sortedBy { it.levelNumber }
        var topics = dao.getTopicsForPyramid(pyramidId)

        var activeFloorIndex = -1
        for (i in floors.indices) {
            val flr = floors[i]
            val flrTopics = topics.filter { it.floorId == flr.id }
            val isFloorDone = flrTopics.isNotEmpty() && flrTopics.all { it.isCompleted }
            if (!isFloorDone) {
                activeFloorIndex = i
                break
            }
        }

        if (activeFloorIndex != -1) {
            val lockedFloorIds = floors.drop(activeFloorIndex + 1).map { it.id }.toSet()
            val topicsToReset = topics.filter { it.floorId in lockedFloorIds && it.isCompleted }
                .map { it.copy(isCompleted = false) }
            if (topicsToReset.isNotEmpty()) {
                dao.updateTopics(topicsToReset)
                topics = dao.getTopicsForPyramid(pyramidId)
            }
        }

        val updatedFloors = floors.mapIndexed { i, floor ->
            val targetStatus = when {
                activeFloorIndex == -1 -> FloorStatus.COMPLETED
                i < activeFloorIndex -> FloorStatus.COMPLETED
                i == activeFloorIndex -> FloorStatus.IN_PROGRESS
                else -> FloorStatus.LOCKED
            }
            floor.copy(status = targetStatus)
        }
        dao.updateFloors(updatedFloors)

        val completedWeightSum = topics.filter { it.isCompleted }.sumOf { it.weight }
        val newTotalWeight = (pyramid.baseWeight + completedWeightSum).coerceAtLeast(0L)
        if (pyramid.totalWeight != newTotalWeight) {
            dao.updatePyramid(pyramid.copy(totalWeight = newTotalWeight))
        }
    }
}
