package babashnik.mors

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part


interface Api {
    @Multipart
    @FormUrlEncoded
    @POST("tg_kitgame_bot")
    fun shareMoment(@Part image: MultipartBody.Part): Call<Item>

}