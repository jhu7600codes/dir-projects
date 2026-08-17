package com.androdrop.app.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/** Talks to the androdrop Next.js API — see androdrop/src/app/api/**/route.ts. */
interface ApiService {

    @POST("api/pair")
    suspend fun pair(@Body body: PairRequest): PairResponse

    @GET("api/pair/{code}")
    suspend fun resolveCode(@Path("code") code: String): ResolveCodeResponse

    @Multipart
    @POST("api/transfer")
    suspend fun createTransfer(
        @Part("senderDeviceId") senderDeviceId: RequestBody,
        @Part("targetDeviceId") targetDeviceId: RequestBody,
        @Part files: List<MultipartBody.Part>,
    ): CreateTransferResponse

    @GET("api/transfer/{id}")
    suspend fun getTransfer(
        @Path("id") id: String,
        @Query("deviceId") deviceId: String? = null,
    ): TransferDetailResponse

    @POST("api/transfer/{id}/respond")
    suspend fun respond(
        @Path("id") id: String,
        @Body body: RespondRequest,
    ): RespondResponse

    @GET("api/transfer/incoming")
    suspend fun incoming(@Query("deviceId") deviceId: String): List<IncomingTransferSummary>

    // No push-subscribe call here: web push (VAPID) is a browser API. Android
    // gets incoming transfers via IncomingTransferService polling this
    // endpoint instead of FCM, matching the "plain REST for upload/poll"
    // option from the original spec.
}
