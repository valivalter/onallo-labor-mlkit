package hu.bme.aut.onlab.valivalter.chessanalyzer.network

import android.app.Activity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import hu.bme.aut.onlab.valivalter.chessanalyzer.model.PositionInfo
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import kotlin.concurrent.thread

class LichessInteractor(val activity: Activity) {

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

    private fun resetPositionInfos(): List<PositionInfo> {
        val temp = positionInfos
        positionInfos = mutableListOf()
        return temp
    }

    fun getInfos(
        fens: MutableList<String>,
        onSuccess: (List<PositionInfo>) -> Unit,
        onError: (Throwable) -> Unit,
        previousPositionInfo: PositionInfo? = null) {

        if (previousPositionInfo != null) {
            positionInfos.add(previousPositionInfo)
        }

        if (fens.size > 0) {
            thread {
                val getPositionInfoRequest = lichessApi.getPositionInfo(fens.removeAt(0))
                try {
                    val response = getPositionInfoRequest.execute().body()!!
                    getInfos(fens, onSuccess, onError, response)
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (!activity.isDestroyed && !activity.isFinishing) {
                        onError(e)
                    }
                }
            }
        }
        else {
            val infos = resetPositionInfos()
            if (!activity.isDestroyed && !activity.isFinishing) {
                onSuccess(infos)
            }
        }
    }
}