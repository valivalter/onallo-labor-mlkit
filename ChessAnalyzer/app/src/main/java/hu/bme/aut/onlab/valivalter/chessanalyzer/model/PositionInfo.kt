package hu.bme.aut.onlab.valivalter.chessanalyzer.model

data class PositionInfo(
    val white: Int,
    val draws: Int,
    val black: Int,
    val topGames: List<TopGame>,
    val opening: Opening?
)

data class TopGame(
    val winner: String?,
    val black: TopPlayer,
    val white: TopPlayer,
    val month: String?
)

class TopPlayer(
    val name: String,
    val rating: Int
)

class Opening(
    val name: String
)