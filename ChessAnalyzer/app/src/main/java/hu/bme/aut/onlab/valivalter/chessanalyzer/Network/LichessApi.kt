package hu.bme.aut.onlab.valivalter.chessanalyzer.Network

import hu.bme.aut.onlab.valivalter.chessanalyzer.PositionInfo
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface LichessApi {

    companion object {
        const val ENDPOINT_URL = "https://explorer.lichess.ovh/"
    }

    @GET("masters")
    fun getPositionInfo(@Query("fen") fen: String): Call<PositionInfo>
}