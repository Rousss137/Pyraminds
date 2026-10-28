package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

val PYRAMID_COLOR_PALETTE: List<Pair<String, String>> = listOf(
    "#2997FF" to "Голубой",
    "#2ECC71" to "Изумрудный",
    "#9B59B6" to "Фиолетовый",
    "#F39C12" to "Янтарный",
    "#E74C3C" to "Рубиновый",
    "#1ABC9C" to "Бирюзовый",
    "#EC4899" to "Розовый",
    "#6366F1" to "Индиго",
    "#84CC16" to "Лаймовый",
    "#F97316" to "Оранжевый"
)

enum class FieldBiome(
    val id: String,
    val title: String,
    val subtitle: String
) {
    DESERT(
        id = "desert",
        title = "Пустыня",
        subtitle = "Золотые песчаные дюны и тёплый солнечный свет"
    ),
    SNOW(
        id = "snow",
        title = "Снежная локация",
        subtitle = "Арктические сугробы, кристаллы льда и снегопад"
    ),
    GRASS(
        id = "grass",
        title = "Травяная локация",
        subtitle = "Изумрудные холмы, мягкая трава и живая природа"
    ),
    UNDERWATER(
        id = "underwater",
        title = "Подводная локация",
        subtitle = "Пирамиды под водой в лучах каустики и пузырьках"
    );

    companion object {
        fun fromId(id: String?): FieldBiome {
            return entries.firstOrNull { it.id == id } ?: DESERT
        }
    }
}

@Entity(tableName = "field_boards")
data class FieldBoard(
    @PrimaryKey val id: String,
    val title: String,
    val biomeId: String = FieldBiome.DESERT.id,
    val orderIndex: Long = 0L
) {
    val biome: FieldBiome
        get() = FieldBiome.fromId(biomeId)
}

@Entity(tableName = "pyramids")
data class Pyramid(
    @PrimaryKey val id: String,
    val boardId: String = "board_main",
    val title: String,
    val baseColor: String = "#2997FF",
    val gridX: Float = 0f,
    val gridY: Float = 0f,
    val baseWeight: Long = 0L,
    val totalWeight: Long = 0L
)

@Entity(tableName = "floors")
data class Floor(
    @PrimaryKey val id: String,
    val pyramidId: String,
    val levelNumber: Long = 1L,
    val title: String,
    val status: String = FloorStatus.IN_PROGRESS
)

object FloorStatus {
    const val COMPLETED = "Завершено"
    const val IN_PROGRESS = "В процессе"
    const val LOCKED = "Заблокирован"
    const val WAITING = "Ожидает"
}

@Entity(tableName = "topics")
data class Topic(
    @PrimaryKey val id: String,
    val pyramidId: String,
    val floorId: String,
    val levelNumber: Long = 1L,
    val title: String,
    val weight: Long = 1L,
    val isCompleted: Boolean = false,
    val orderIndex: Long = 0L
)

data class FloorWithTopics(
    val floor: Floor,
    val topics: List<Topic>
)

data class PyramidDetail(
    val pyramid: Pyramid,
    val floorsWithTopics: List<FloorWithTopics>
)

data class NewFloorDraft(
    val title: String,
    val firstTopicTitle: String,
    val firstTopicWeight: Long
)
