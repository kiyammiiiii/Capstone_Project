package com.labactivity.handa

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface TwilioApi {
    @POST("/make_call")
    suspend fun makeCall(
        @Body callData: CallData
    ): Response<CallResponse>
}
