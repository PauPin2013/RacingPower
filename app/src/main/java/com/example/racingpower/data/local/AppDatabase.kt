package com.example.racingpower.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.racingpower.data.model.PlayerScore

@Database(entities = [PlayerScore::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scoreDao(): ScoreDao
}