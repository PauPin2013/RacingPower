class GameRepository(private val dao: ScoreDao) {
    suspend fun getScore(username: String) = dao.getScore(username)
    suspend fun saveScore(score: PlayerScore) = dao.insertOrUpdate(score)
}