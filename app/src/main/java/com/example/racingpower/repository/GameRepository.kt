package com.example.racingpower.repository

import com.example.racingpower.data.local.ScoreDao
import com.example.racingpower.data.model.PlayerScore

class GameRepository(private val dao: ScoreDao) {
    suspend fun getScore(username: String) = dao.getScore(username)
    suspend fun saveScore(score: PlayerScore) = dao.insertOrUpdate(score)
}