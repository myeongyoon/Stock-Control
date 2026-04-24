package com.mychoi.linencontrol.data.remote.api

import com.mychoi.linencontrol.data.remote.model.ClaudeRequest
import com.mychoi.linencontrol.data.remote.model.ClaudeResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ClaudeApiService {
    @POST("v1/messages")
    suspend fun sendMessage(@Body request: ClaudeRequest): ClaudeResponse
}