package hu.bme.aut.onlab.valivalter.chessanalyzer.Network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import hu.bme.aut.onlab.valivalter.chessanalyzer.PositionInfo
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import kotlin.concurrent.thread

class LichessInteractor {

    private val lichessApi: LichessApi
    private var positionInfos = mutableListOf<PositionInfo>()

    init {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(LichessApi.ENDPOINT_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        this.lichessApi = retrofit.create(LichessApi::class.java)
    }

    //fun addPositionInfo(positionInfo: PositionInfo) {
    //    positionInfos.add(positionInfo)
    //}

    fun resetPositionInfos(): List<PositionInfo> {
        val temp = positionInfos
        positionInfos = mutableListOf()
        return temp
    }

    //fun getPositionInfos(fens: MutableList<String>, onSuccess: (List<PositionInfo>) -> Unit, onError: (Throwable) -> Unit) {
    //    getInfos()
    //}

    fun getInfos(
        fens: MutableList<String>,
        onSuccess: (List<PositionInfo>) -> Unit,
        onError: (Throwable) -> Unit,
        previousPositionInfo: PositionInfo? = null) {

        if (previousPositionInfo != null) {
            positionInfos.add(previousPositionInfo)
        }

        if (fens.size > 0) {
            val getPositionInfoRequest = lichessApi.getPositionInfo(fens.removeAt(0))
            thread {
                try {
                    val response = getPositionInfoRequest.execute().body()!!
                    getInfos(fens, onSuccess, onError, response)
                } catch (e: Exception) {
                    e.printStackTrace()
                    onError(e)
                }
            }
        }
        else {
            val infos = resetPositionInfos()
            onSuccess(infos)
        }
    }

    /*fun getPositionInfo(
        fen: String,
        onSuccess: (List<PositionInfo>) -> Unit,
        onError: (Throwable) -> Unit,
        remainingFens: Int
    ) {
        if (remainingFens >= 0) {
            val getPositionInfoRequest = lichessApi.getPositionInfo(fen)
            runCallOnBackgroundThread(getPositionInfoRequest, this::addPositionInfo, onError)
        }
        else {
            val infos = resetPositionInfos()
            onSuccess(infos)
        }
    }*/

    private fun runCallOnBackgroundThread(
        call: Call<PositionInfo>,
        onSuccess: (PositionInfo) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        thread {
            try {
                val response = call.execute().body()!!
                onSuccess(response)
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e)
            }
        }
    }
}