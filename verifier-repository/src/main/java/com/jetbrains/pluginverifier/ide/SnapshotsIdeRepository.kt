package com.jetbrains.pluginverifier.ide

import com.google.common.base.Suppliers
import com.google.gson.annotations.SerializedName
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.createOkHttpClient
import com.jetbrains.pluginverifier.network.executeSuccessfully
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

/**
 * Provides index of snapshot IDE builds available for downloading.
 * The index is fetched from https://www.jetbrains.com/intellij-repository/snapshots/.
 */
class SnapshotsIdeRepository : IdeRepository {

  companion object {
    const val INTELLIJ_REPOSITORY_URL = "https://www.jetbrains.com/intellij-repository/"
  }

  private val repositoryIndexConnector by lazy {
    Retrofit.Builder()
        .baseUrl(INTELLIJ_REPOSITORY_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(createOkHttpClient(false, 5, TimeUnit.MINUTES))
        .build()
        .create(RepositoryIndexConnector::class.java)
  }

  private val indexCache = Suppliers.memoizeWithExpiration<List<AvailableIde>>(this::updateIndex, 1, TimeUnit.MINUTES)

  private fun updateIndex(): List<AvailableIde> {
    val artifacts = repositoryIndexConnector
        .getSnapshotsIndex()
        .executeSuccessfully().body()
        .artifacts
    return IntelliJRepositoryIndexParser().parseArtifacts(artifacts, true)
  }

  override fun fetchIndex(): List<AvailableIde> = indexCache.get()

  override fun fetchAvailableIde(ideVersion: IdeVersion): AvailableIde? {
    val fullIdeVersion = ideVersion.setProductCodeIfAbsent("IU")
    return fetchIndex().find { it.version == fullIdeVersion }
  }

  override fun toString() = "IDE Repository on $INTELLIJ_REPOSITORY_URL"

}

internal data class ArtifactsJson(
    @SerializedName("artifacts")
    val artifacts: List<ArtifactJson>
)

internal data class ArtifactJson(
    @SerializedName("groupId")
    val groupId: String,

    @SerializedName("artifactId")
    val artifactId: String,

    @SerializedName("version")
    val version: String,

    @SerializedName("packaging")
    val packaging: String,

    @SerializedName("content")
    val content: String?
)

private interface RepositoryIndexConnector {

  @GET("snapshots/index.json")
  fun getSnapshotsIndex(): Call<ArtifactsJson>
}
