package com.not_example.network_1ch

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.*

interface ServerAPI {
    @GET("1ch")
    suspend fun getMessages(
        @Query("limit") limit: Int,
        @Query("lastKnownId") lastKnownId: Long,
        @Query("reverse") reverse: Boolean = false
    ): List<MessageDbEntry>

    @Multipart
    @POST("1ch")
    suspend fun sendImage(
        @Part("msg") msg: RequestBody,
        @Part image: MultipartBody.Part
    ): ResponseBody

    @POST("1ch")
    suspend fun sendMessage(
        @Body msg: RequestBody
    ): ResponseBody
}