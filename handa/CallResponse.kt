package com.labactivity.handa

data class CallResponse(
    val status: String,
    val call_sid: String? = null,
    val message: String? = null
)
