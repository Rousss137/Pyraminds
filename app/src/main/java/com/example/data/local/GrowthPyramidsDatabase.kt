package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.data.model.FieldBoard
import com.example.data.model.Floor
import com.example.data.model.Pyramid
import com.example.data.model.Topic
import kotlinx.coroutines.flow.Flow

@Dao
interface PyramidDao {
    @Query("SELECT * FROM field_boards ORDER BY orderIndex ASC")
    fun observeAllBoards(): Flow<List<FieldBoard>>

    @Query("SELECT COUNT(*) FROM field_boards")
    suspend fun countBoards(): Int

    @Query("SELECT * FROM field_boards WHERE id = :boardId LIMIT 1")
    suspend fun getBoardById(boardId: String): FieldBoard?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoard(board: FieldBoard)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoards(boards: List<FieldBoard>)

    @Update
    suspend fun updateBoard(board: FieldBoard)

    @Query("DELETE FROM field_boards")
    suspend fun deleteAllBoards()

    @Query("SELECT * FROM pyramids ORDER BY totalWeight DESC")
    fun observeAllPyramids(): Flow<List<Pyramid>>

    @Query("SELECT * FROM pyramids WHERE boardId = :boardId ORDER BY totalWeight DESC")
    fun observePyramidsForBoard(boardId: String): Flow<List<Pyramid>>

    @Query("SELECT * FROM pyramids WHERE id = :pyramidId LIMIT 1")
    fun observePyramidById(pyramidId: String): Flow<Pyramid?>

    @Query("SELECT * FROM floors WHERE pyramidId = :pyramidId ORDER BY levelNumber ASC")
    fun observeFloorsForPyramid(pyramidId: String): Flow<List<Floor>>

    @Query("SELECT * FROM topics WHERE pyramidId = :pyramidId ORDER BY orderIndex ASC")
    fun observeTopicsForPyramid(pyramidId: String): Flow<List<Topic>>

    @Query("SELECT COUNT(*) FROM pyramids")
    suspend fun countPyramids(): Int

    @Query("SELECT * FROM field_boards ORDER BY orderIndex ASC")
    suspend fun getAllBoards(): List<FieldBoard>

    @Query("SELECT * FROM pyramids ORDER BY totalWeight DESC")
    suspend fun getAllPyramids(): List<Pyramid>

    @Query("SELECT * FROM floors ORDER BY levelNumber ASC")
    suspend fun getAllFloors(): List<Floor>

    @Query("SELECT * FROM topics ORDER BY orderIndex ASC")
    suspend fun getAllTopics(): List<Topic>

    @Query("SELECT * FROM pyramids WHERE id = :pyramidId LIMIT 1")
    suspend fun getPyramidById(pyramidId: String): Pyramid?

    @Query("SELECT * FROM floors WHERE id = :floorId LIMIT 1")
    suspend fun getFloorById(floorId: String): Floor?

    @Query("SELECT * FROM floors WHERE pyramidId = :pyramidId ORDER BY levelNumber ASC")
    suspend fun getFloorsForPyramid(pyramidId: String): List<Floor>

    @Query("SELECT * FROM topics WHERE pyramidId = :pyramidId ORDER BY orderIndex ASC")
    suspend fun getTopicsForPyramid(pyramidId: String): List<Topic>

    @Query("SELECT * FROM topics WHERE id = :topicId LIMIT 1")
    suspend fun getTopicById(topicId: String): Topic?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPyramid(pyramid: Pyramid)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPyramids(pyramids: List<Pyramid>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFloor(floor: Floor)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFloors(floors: List<Floor>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: Topic)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<Topic>)

    @Update
    suspend fun updatePyramid(pyramid: Pyramid)

    @Update
    suspend fun updateFloor(floor: Floor)

    @Update
    suspend fun updateFloors(floors: List<Floor>)

    @Update
    suspend fun updateTopic(topic: Topic)

    @Update
    suspend fun updateTopics(topics: List<Topic>)

    @Query("DELETE FROM topics")
    suspend fun deleteAllTopics()

    @Query("DELETE FROM floors")
    suspend fun deleteAllFloors()

    @Query("DELETE FROM pyramids")
    suspend fun deleteAllPyramids()
}

@Database(
    entities = [Pyramid::class, Floor::class, Topic::class, FieldBoard::class],
    version = 4,
    exportSchema = false
)
abstract class GrowthPyramidsDatabase : RoomDatabase() {
    abstract fun pyramidDao(): PyramidDao

    companion object {
        @Volatile
        private var INSTANCE: GrowthPyramidsDatabase? = null

        fun getInstance(context: Context): GrowthPyramidsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GrowthPyramidsDatabase::class.java,
                    "growth_pyramids_room.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
