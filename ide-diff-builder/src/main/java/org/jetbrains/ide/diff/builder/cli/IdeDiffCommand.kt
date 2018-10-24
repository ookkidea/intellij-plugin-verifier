package org.jetbrains.ide.diff.builder.cli

import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.ide.diff.builder.api.SinceApiBuilder
import org.jetbrains.ide.diff.builder.persistence.SinceApiWriter
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Builds API diff between two IDE versions,
 * and saves the result as external "available since"
 * annotations root with since-build equal to the second IDE.
 * @see [help]
 */
class IdeDiffCommand : Command {
  companion object {
    private val LOG = LoggerFactory.getLogger("ide-diff")
  }

  override val commandName
    get() = "ide-diff"

  override val help
    get() = """
      Builds API diff between two IDE versions,
      and saves the result as external "available since"
      annotations root with since-build equal to the second IDE.

      ide-diff <old IDE path> <new IDE path> <result path>

      For example:
      java -jar diff-builder.jar ide-diff path/to/IU-183.1 path/to/IU-183.567 path/to/result

      will build and save "available since" IU-183.567 external annotations
      to path/to/result, which can be a directory or a zip file.
    """.trimIndent()

  override fun execute(freeArgs: List<String>) {
    val oldIdePath = Paths.get(freeArgs[0])
    val newIdePath = Paths.get(freeArgs[1])
    val resultRoot = Paths.get(freeArgs[2])
    val (newVersion, oldVersion) = buildIdeDiff(oldIdePath, newIdePath, resultRoot)
    println("New API in $newVersion compared to $oldVersion is saved to external annotations root ${resultRoot.toAbsolutePath()}")
  }

  fun buildIdeDiff(oldIdePath: Path, newIdePath: Path, resultRoot: Path): Pair<IdeVersion, IdeVersion> {
    val oldIde = IdeManager.createManager().createIde(oldIdePath.toFile())
    val newIde = IdeManager.createManager().createIde(newIdePath.toFile())
    LOG.info("Building API diff between ${oldIde.version} and ${newIde.version}")

    val sinceApiData = SinceApiBuilder().build(oldIde, newIde)
    SinceApiWriter(resultRoot).use {
      it.appendSinceApiData(sinceApiData)
    }
    return newIde.version to oldIde.version
  }

}