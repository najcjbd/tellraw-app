package com.tellraw.app.data.remote

import retrofit2.http.*

interface ApiService {
    
    @GET("selectors/validate")
    suspend fun validateSelector(@Query("selector") selector: String): ApiResponse<SelectorValidationResult>
    
    @GET("commands/suggest")
    suspend fun getCommandSuggestions(@Query("partial") partialCommand: String): ApiResponse<List<String>>
    
    @POST("commands/sync")
    suspend fun syncCommand(@Body command: SyncCommandRequest): ApiResponse<SyncResult>
    
    @GET("version/check")
    suspend fun checkVersion(): ApiResponse<VersionInfo>
}

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?
)

data class SelectorValidationResult(
    val isValid: Boolean,
    val type: String, // "java" or "bedrock"
    val suggestions: List<String>
)

data class SyncCommandRequest(
    val command: String,
    val version: String, // "java" or "bedrock"
    val timestamp: Long
)

data class SyncResult(
    val synced: Boolean,
    val commandId: String
)

data class VersionInfo(
    val currentVersion: String,
    val latestVersion: String,
    val updateAvailable: Boolean,
    val updateNotes: String?
)