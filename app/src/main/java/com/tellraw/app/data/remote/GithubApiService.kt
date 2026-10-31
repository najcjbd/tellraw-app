package com.tellraw.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface GithubApiService {
    @GET("repos/najcjbd/tellraw-app/releases/latest")
    suspend fun getLatestRelease(): GithubRelease
}

data class GithubRelease(
    val tag_name: String,
    /** GitHub 允许发布没有标题/说明的 release，这两个字段可能为 null。 */
    val name: String? = null,
    val html_url: String,
    val published_at: String,
    val body: String? = null
)