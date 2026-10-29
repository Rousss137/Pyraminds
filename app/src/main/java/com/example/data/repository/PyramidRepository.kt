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
import org.json.JSONArray
import org.json.JSONObject
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

    suspend fun updatePyramidTitle(pyramidId: String, newTitle: String): Result<Unit> {
        val clean = newTitle.trim().take(120)
        if (clean.isBlank()) {
            return Result.failure(IllegalArgumentException("Название пирамиды не может быть пустым"))
        }
        return try {
            val existing = dao.getPyramidById(pyramidId)
                ?: return Result.failure(IllegalArgumentException("Пирамида не найдена"))
            dao.updatePyramid(existing.copy(title = clean))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFloorTitle(floorId: String, newTitle: String): Result<Unit> {
        val clean = newTitle.trim().take(150)
        if (clean.isBlank()) {
            return Result.failure(IllegalArgumentException("Название этажа не может быть пустым"))
        }
        return try {
            val existing = dao.getFloorById(floorId)
                ?: return Result.failure(IllegalArgumentException("Этаж не найден"))
            val formattedTitle = if (clean.startsWith("Этаж")) {
                clean
            } else {
                "Этаж ${existing.levelNumber}: $clean".take(150)
            }
            dao.updateFloor(existing.copy(title = formattedTitle))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTopic(
        pyramidId: String,
        topicId: String,
        newTitle: String,
        newWeight: Long
    ): Result<Unit> {
        val clean = newTitle.trim().take(200)
        if (clean.isBlank()) {
            return Result.failure(IllegalArgumentException("Название цели / задачи не может быть пустым"))
        }
        return try {
            val existing = dao.getTopicById(topicId)
                ?: return Result.failure(IllegalArgumentException("Задача не найдена"))
            val safeWeight = newWeight.coerceIn(1L, 10000L)
            dao.updateTopic(
                existing.copy(
                    title = clean,
                    weight = safeWeight
                )
            )
            recalculatePyramidProgression(pyramidId)
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

    /**
     * Exports a stable, versioned JSON backup template (`schemaVersion = 1`) that preserves
     * backward and forward compatibility across app updates.
     */
    suspend fun exportBackupJson(): Result<String> {
        return try {
            ensureDefaultBoardExists()
            val boards = dao.getAllBoards()
            val pyramids = dao.getAllPyramids()
            val floors = dao.getAllFloors()
            val topics = dao.getAllTopics()

            val root = JSONObject().apply {
                put("app", "Pyraminds")
                put("title", "Пирамиды личностного роста")
                put("schemaVersion", 1)
                put("exportedAt", System.currentTimeMillis())

                val boardsArray = JSONArray()
                for (b in boards) {
                    boardsArray.put(
                        JSONObject().apply {
                            put("id", b.id)
                            put("title", b.title)
                            put("biomeId", b.biomeId)
                            put("orderIndex", b.orderIndex)
                        }
                    )
                }
                put("boards", boardsArray)

                val pyramidsArray = JSONArray()
                for (p in pyramids) {
                    pyramidsArray.put(
                        JSONObject().apply {
                            put("id", p.id)
                            put("boardId", p.boardId)
                            put("title", p.title)
                            put("baseColor", p.baseColor)
                            put("gridX", p.gridX.toDouble())
                            put("gridY", p.gridY.toDouble())
                            put("baseWeight", p.baseWeight)
                            put("totalWeight", p.totalWeight)
                        }
                    )
                }
                put("pyramids", pyramidsArray)

                val floorsArray = JSONArray()
                for (f in floors) {
                    floorsArray.put(
                        JSONObject().apply {
                            put("id", f.id)
                            put("pyramidId", f.pyramidId)
                            put("levelNumber", f.levelNumber)
                            put("title", f.title)
                            put("status", f.status)
                        }
                    )
                }
                put("floors", floorsArray)

                val topicsArray = JSONArray()
                for (t in topics) {
                    topicsArray.put(
                        JSONObject().apply {
                            put("id", t.id)
                            put("pyramidId", t.pyramidId)
                            put("floorId", t.floorId)
                            put("levelNumber", t.levelNumber)
                            put("title", t.title)
                            put("weight", t.weight)
                            put("isCompleted", t.isCompleted)
                            put("orderIndex", t.orderIndex)
                        }
                    )
                }
                put("topics", topicsArray)
            }

            Result.success(root.toString(2))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Imports a JSON backup with tolerant schema parsing so backups saved on older or newer
     * versions load cleanly without data loss.
     */
    suspend fun importBackupJson(
        rawJson: String,
        replaceExisting: Boolean
    ): Result<Int> {
        return try {
            val cleaned = rawJson.trim()
            if (cleaned.isEmpty()) {
                return Result.failure(IllegalArgumentException("Файл резервной копии пуст"))
            }

            val root = JSONObject(cleaned)

            val parsedBoards = mutableListOf<FieldBoard>()
            val boardsJson = root.optJSONArray("boards") ?: root.optJSONArray("field_boards")
            if (boardsJson != null) {
                for (i in 0 until boardsJson.length()) {
                    val obj = boardsJson.optJSONObject(i) ?: continue
                    val id = obj.optString("id").ifBlank { "board_${i + 1}" }
                    val title = obj.optString("title", "Основное поле").ifBlank { "Основное поле" }
                    val biomeId = obj.optString(
                        "biomeId",
                        obj.optString("biome_id", FieldBiome.DESERT.id)
                    ).ifBlank { FieldBiome.DESERT.id }
                    val orderIndex = obj.optLong("orderIndex", obj.optLong("order_index", i.toLong()))
                    parsedBoards.add(
                        FieldBoard(
                            id = id,
                            title = title.take(60),
                            biomeId = FieldBiome.fromId(biomeId).id,
                            orderIndex = orderIndex
                        )
                    )
                }
            }

            if (parsedBoards.none { it.id == "board_main" } && parsedBoards.isEmpty()) {
                parsedBoards.add(
                    FieldBoard(
                        id = "board_main",
                        title = "Основное поле",
                        biomeId = FieldBiome.DESERT.id,
                        orderIndex = 0L
                    )
                )
            }
            val defaultBoardId = parsedBoards.first().id

            val parsedPyramids = mutableListOf<Pyramid>()
            val parsedFloors = mutableListOf<Floor>()
            val parsedTopics = mutableListOf<Topic>()

            val pyramidsJson = root.optJSONArray("pyramids")
            if (pyramidsJson != null) {
                for (i in 0 until pyramidsJson.length()) {
                    val pObj = pyramidsJson.optJSONObject(i) ?: continue
                    val pId = pObj.optString("id").ifBlank {
                        "pyr_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                    }
                    val boardId = pObj.optString(
                        "boardId",
                        pObj.optString("board_id", defaultBoardId)
                    ).ifBlank { defaultBoardId }
                    val title = pObj.optString("title", "Пирамида ${i + 1}").ifBlank { "Пирамида ${i + 1}" }
                    val baseColor = pObj.optString(
                        "baseColor",
                        pObj.optString("base_color", "#2997FF")
                    ).ifBlank { "#2997FF" }
                    val gridX = pObj.optDouble("gridX", pObj.optDouble("grid_x", 0.0)).toFloat().coerceIn(-25f, 25f)
                    val gridY = pObj.optDouble("gridY", pObj.optDouble("grid_y", 0.0)).toFloat().coerceIn(-25f, 25f)
                    val baseWeight = pObj.optLong("baseWeight", pObj.optLong("base_weight", 10L))
                    val totalWeight = pObj.optLong("totalWeight", pObj.optLong("total_weight", baseWeight))

                    parsedPyramids.add(
                        Pyramid(
                            id = pId,
                            boardId = boardId,
                            title = title.take(120),
                            baseColor = baseColor,
                            gridX = gridX,
                            gridY = gridY,
                            baseWeight = baseWeight,
                            totalWeight = totalWeight
                        )
                    )

                    // Support nested floors inside pyramid object (if exported from older/custom format)
                    val nestedFloors = pObj.optJSONArray("floors")
                    if (nestedFloors != null) {
                        for (fIdx in 0 until nestedFloors.length()) {
                            val fObj = nestedFloors.optJSONObject(fIdx) ?: continue
                            val fId = fObj.optString("id").ifBlank {
                                "flr_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                            }
                            val levelNumber = fObj.optLong(
                                "levelNumber",
                                fObj.optLong("level_number", (fIdx + 1).toLong())
                            )
                            val fTitle = fObj.optString("title", "Этаж $levelNumber")
                            val fStatus = fObj.optString("status", FloorStatus.IN_PROGRESS)
                            parsedFloors.add(
                                Floor(
                                    id = fId,
                                    pyramidId = pId,
                                    levelNumber = levelNumber,
                                    title = fTitle.take(150),
                                    status = fStatus
                                )
                            )

                            val nestedTopics = fObj.optJSONArray("topics")
                            if (nestedTopics != null) {
                                for (tIdx in 0 until nestedTopics.length()) {
                                    val tObj = nestedTopics.optJSONObject(tIdx) ?: continue
                                    val tId = tObj.optString("id").ifBlank {
                                        "top_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                                    }
                                    val tTitle = tObj.optString("title", "Шаг ${tIdx + 1}")
                                    val tWeight = tObj.optLong("weight", 1L).coerceIn(1L, 10000L)
                                    val isCompleted = tObj.optBoolean(
                                        "isCompleted",
                                        tObj.optBoolean("is_completed", false)
                                    )
                                    val orderIndex = tObj.optLong(
                                        "orderIndex",
                                        tObj.optLong("order_index", tIdx.toLong())
                                    )
                                    parsedTopics.add(
                                        Topic(
                                            id = tId,
                                            pyramidId = pId,
                                            floorId = fId,
                                            levelNumber = levelNumber,
                                            title = tTitle.take(200),
                                            weight = tWeight,
                                            isCompleted = isCompleted,
                                            orderIndex = orderIndex
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Support top-level flat floors array
            val floorsJson = root.optJSONArray("floors")
            if (floorsJson != null) {
                val existingFloorIds = parsedFloors.map { it.id }.toMutableSet()
                for (i in 0 until floorsJson.length()) {
                    val fObj = floorsJson.optJSONObject(i) ?: continue
                    val fId = fObj.optString("id").ifBlank {
                        "flr_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                    }
                    if (fId in existingFloorIds) continue
                    val pId = fObj.optString("pyramidId", fObj.optString("pyramid_id", ""))
                    if (pId.isBlank()) continue
                    val levelNumber = fObj.optLong("levelNumber", fObj.optLong("level_number", 1L))
                    val fTitle = fObj.optString("title", "Этаж $levelNumber")
                    val fStatus = fObj.optString("status", FloorStatus.IN_PROGRESS)
                    parsedFloors.add(
                        Floor(
                            id = fId,
                            pyramidId = pId,
                            levelNumber = levelNumber,
                            title = fTitle.take(150),
                            status = fStatus
                        )
                    )
                    existingFloorIds.add(fId)
                }
            }

            // Map floorId -> Floor for resolving missing pyramidId/levelNumber on topics
            val floorMap = parsedFloors.associateBy { it.id }

            // Support top-level flat topics array
            val topicsJson = root.optJSONArray("topics")
            if (topicsJson != null) {
                val existingTopicIds = parsedTopics.map { it.id }.toMutableSet()
                for (i in 0 until topicsJson.length()) {
                    val tObj = topicsJson.optJSONObject(i) ?: continue
                    val tId = tObj.optString("id").ifBlank {
                        "top_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                    }
                    if (tId in existingTopicIds) continue
                    val floorId = tObj.optString("floorId", tObj.optString("floor_id", ""))
                    if (floorId.isBlank()) continue
                    val parentFloor = floorMap[floorId]
                    val pyramidId = tObj.optString(
                        "pyramidId",
                        tObj.optString("pyramid_id", parentFloor?.pyramidId ?: "")
                    )
                    if (pyramidId.isBlank()) continue
                    val levelNumber = tObj.optLong(
                        "levelNumber",
                        tObj.optLong("level_number", parentFloor?.levelNumber ?: 1L)
                    )
                    val tTitle = tObj.optString("title", "Задача")
                    val tWeight = tObj.optLong("weight", 1L).coerceIn(1L, 10000L)
                    val isCompleted = tObj.optBoolean(
                        "isCompleted",
                        tObj.optBoolean("is_completed", false)
                    )
                    val orderIndex = tObj.optLong("orderIndex", tObj.optLong("order_index", i.toLong()))

                    parsedTopics.add(
                        Topic(
                            id = tId,
                            pyramidId = pyramidId,
                            floorId = floorId,
                            levelNumber = levelNumber,
                            title = tTitle.take(200),
                            weight = tWeight,
                            isCompleted = isCompleted,
                            orderIndex = orderIndex
                        )
                    )
                    existingTopicIds.add(tId)
                }
            }

            // Ensure every pyramid's boardId exists in boards
            val knownBoardIds = parsedBoards.map { it.id }.toMutableSet()
            for (pyr in parsedPyramids) {
                if (pyr.boardId !in knownBoardIds) {
                    parsedBoards.add(
                        FieldBoard(
                            id = pyr.boardId,
                            title = "Основное поле",
                            biomeId = FieldBiome.DESERT.id,
                            orderIndex = parsedBoards.size.toLong()
                        )
                    )
                    knownBoardIds.add(pyr.boardId)
                }
            }

            if (replaceExisting) {
                dao.deleteAllTopics()
                dao.deleteAllFloors()
                dao.deleteAllPyramids()
                dao.deleteAllBoards()
            }

            dao.insertBoards(parsedBoards)
            dao.insertPyramids(parsedPyramids)
            dao.insertFloors(parsedFloors)
            dao.insertTopics(parsedTopics)

            for (pyr in parsedPyramids) {
                recalculatePyramidProgression(pyr.id)
            }

            Result.success(parsedPyramids.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(
                IllegalArgumentException(
                    "Не удалось прочитать резервную копию: ${e.localizedMessage ?: "некорректный формат файла"}"
                )
            )
        }
    }
}
