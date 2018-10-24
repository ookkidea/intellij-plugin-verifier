package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.ide.diff.builder.signatures.ApiSignature

/**
 * Container of APIs and versions when those APIs were first introduced.
 */
data class SinceApiData(val versionToApiData: Map<IdeVersion, ApiData>) {
  /**
   * Returns this data object as a sequence of signatures
   * and "available since" versions.
   */
  fun asSequence(): Sequence<Pair<ApiSignature, IdeVersion>> =
      versionToApiData
          .asSequence()
          .flatMap { (ideVersion, signatures) ->
            signatures.apiSignatures.asSequence().map { it to ideVersion }
          }

  /**
   * Returns `true` if `this` contains [apiSignature] associated
   * with some [IdeVersion].
   */
  operator fun contains(apiSignature: ApiSignature) =
      versionToApiData.values.any { apiSignature in it }

}

/**
 * Builds a new [SinceApiData] that contains all APIs
 * from `this` and `other` data.
 */
infix operator fun SinceApiData.plus(other: SinceApiData): SinceApiData {
  val merged = mutableMapOf<IdeVersion, ApiData>()
  merged.putAll(versionToApiData)
  for ((version, apiData) in other.versionToApiData) {
    val data = merged[version]
    if (data != null) {
      data.mergeWith(apiData)
    } else {
      merged[version] = apiData
    }
  }
  return SinceApiData(merged)
}