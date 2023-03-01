package com.example.climbit.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "grade",
    foreignKeys = [
        ForeignKey(
            entity = Difficulty::class,
            childColumns = ["difficulty_id"],
            parentColumns = ["id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class Grade(
    @PrimaryKey
    val id: Long,

    @ColumnInfo(name = "difficulty_id")
    val difficultyID: Long,

    @ColumnInfo(name = "font_scale")
    val fontScale: String,
) {
    override fun toString(): String {
        return fontScale
    }
}
