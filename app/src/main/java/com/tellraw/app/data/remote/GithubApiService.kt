package com.tellraw.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface GithubApiService {
    @GET("repos/najcjbd/tellraw-app/releases/latest")
    suspend fun getLatestRelease(): GithubRelease
}

data class GithubRelease(
    val tag_name: String,
    val name: String,
    val html_url: String,
    val published_at: String,
    val body: String
)