package main.budgieapp.network

import main.budgieapp.models.AIRequest
import main.budgieapp.models.AIResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AIService {
    @POST("/")
    suspend fun askAI(
        @Header("Authorization") authorization: String,
        @Body request: AIRequest
    ): AIResponse
}