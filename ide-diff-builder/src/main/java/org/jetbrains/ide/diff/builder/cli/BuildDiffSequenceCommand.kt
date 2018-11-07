package org.jetbrains.ide.diff.builder.cli

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.misc.simpleName
import com.jetbrains.pluginverifier.misc.tryInvokeSeveralTimes
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

/**
 * Builds a cumulative sequence of API diffs of multiple IDE builds.
 *
 * @see [help]
 */
class BuildDiffSequenceCommand : Command {

  companion object {
    private val LOG = LoggerFactory.getLogger("build-diff-sequence")
  }

  override val help
    get() = """
      Builds a cumulative sequence of API diffs of multiple IDE builds.
      API diffs are saved in .zip files under the /result directory.

      build-diff-sequence [-ides-dir <path>] path/to/result path/to/IDE-1 [path/to/IDE-2 ... path/to/IDE-n]

      -ides-dir option is used to specify a path where downloaded IDE builds will be kept.
      If not specified, temp directory will be used.

      For example:
      java -jar diff-builder.jar build-diff-sequence -ides-dir ./ides-cache result/ IU-181.1 IU-181.9 IU-182.1 IU-183.1

      will build API diffs between
       IU-181.1 <-> IU-181.9  ---> result/since-IU-181.9.zip
       IU-181.9 <-> IU-182.1  ---> result/since-IU-182.1.zip (contains since IU-181.9)
       IU-182.1 <-> IU-183.1  ---> result/since-IU-183.1.zip (contains since IU-182.1)

      Downloaded IDE builds will be cached in ./ides-cache, which is limited in size to 10 GB.

      The resulting diffs will be saved to result/ directory in files <version>.zip,
      which contains cumulative "available since" external annotations for all IDEs
      prior to that version in the versions sequence.
    """.trimIndent()

  override val commandName
    get() = "build-diff-sequence"

  open class CliOptions {
    @set:Argument("ides-dir", description = "Path where downloaded IDE builds are cached")
    var idesDirPath: String? = null
  }

  private data class Options(
      val idesDir: Path,
      val resultPath: Path,
      val ideVersions: List<IdeVersion>
  )

  /**
   * Parses command line options of the `build-diff-sequence` command.
   */
  private fun parseOptions(freeArgs: List<String>): Options {
    val cliOptions = CliOptions()
    var args = Args.parse(cliOptions, freeArgs.toTypedArray(), true)

    val idesDir = if (cliOptions.idesDirPath != null) {
      Paths.get(cliOptions.idesDirPath)
    } else {
      Files.createTempDirectory("ides-dir").also {
        it.toFile().deleteOnExit()
      }
    }

    val resultPath = Paths.get(args.first())
    resultPath.deleteLogged()

    args = args.drop(1)

    if (args.size < 2) {
      System.err.println("At least 2 IDEs must be specified")
      exit(help)
    }

    val ideVersions = args.map { IdeVersion.createIdeVersion(it) }
    checkIdeVersionsAvailable(ideVersions)
    return Options(idesDir, resultPath, ideVersions)
  }

  private fun checkIdeVersionsAvailable(ideVersions: List<IdeVersion>) {
    for (ideVersion in ideVersions) {
      if (allIdeRepository.fetchAvailableIde(ideVersion) == null) {
        throw IllegalArgumentException("IDE $ideVersion is not available in the IntelliJ artifacts repositories\n" +
            "Only the following IDEs are available: " + allIdeRepository.fetchIndex().joinToString())
      }
    }
  }

  private fun IdeVersion.getSinceFileName() =
      "since-" + asString() + ".zip"

  override fun execute(freeArgs: List<String>) {
    val (idesDir, resultPath, ideVersions) = parseOptions(freeArgs)
    val ideFilesBank = IdeFilesBank(
        idesDir,
        allIdeRepository,
        DiskSpaceSetting(SpaceAmount.ONE_GIGO_BYTE * 10)
    )

    LOG.info("Building API diffs for a list of IDE builds: " + ideVersions.joinToString())
    buildDiffs(ideVersions, ideFilesBank, resultPath)
  }

  /**
   * Builds diffs between adjacent IDE builds listed in [ideVersions]
   * and saves cumulative "available since" external annotations under [resultPath].
   */
  private fun buildDiffs(
      ideVersions: List<IdeVersion>,
      ideFilesBank: IdeFilesBank,
      resultPath: Path
  ) {
    var previousResult: Path? = null
    for (i in 0 until ideVersions.size - 1) {
      val oldIdeVersion = ideVersions[i]
      val newIdeVersion = ideVersions[i + 1]

      val newResultPath = resultPath.resolve(newIdeVersion.getSinceFileName())
      newResultPath.deleteLogged()

      val tempResultPath = newResultPath.resolveSibling("temp-" + newResultPath.simpleName)
      try {
        buildDiffBetweenIdes(oldIdeVersion, newIdeVersion, ideFilesBank, tempResultPath)
        if (previousResult == null) {
          Files.move(tempResultPath, newResultPath)
        } else {
          MergeSinceDataCommand().mergeSinceData(previousResult, tempResultPath, newResultPath)
        }
        previousResult = newResultPath
      } finally {
        tempResultPath.deleteLogged()
      }
      LOG.info("Cumulative API diff between $newIdeVersion and previous IDE builds has been saved to $newResultPath")
    }
  }

  /**
   * Downloads IDEs [oldIdeVersion] and [newIdeVersion] from [ideFilesBank],
   * builds API diff between them and saves it to [resultPath].
   */
  private fun buildDiffBetweenIdes(
      oldIdeVersion: IdeVersion,
      newIdeVersion: IdeVersion,
      ideFilesBank: IdeFilesBank,
      resultPath: Path
  ) {
    val oldIdeResult = ideFilesBank.downloadIde(oldIdeVersion)
    return oldIdeResult.ideFileLock.use { oldIdeFileLock ->
      val newIdeResult = ideFilesBank.downloadIde(newIdeVersion)
      newIdeResult.ideFileLock.use { newIdeFileLock ->
        IdeDiffCommand().buildIdeDiff(
            oldIdeFileLock.file,
            newIdeFileLock.file,
            resultPath
        )
      }
    }
  }

  private fun IdeFilesBank.downloadIde(ideVersion: IdeVersion): IdeFilesBank.Result.Found {
    return tryInvokeSeveralTimes(3, 3, TimeUnit.SECONDS, "Download $ideVersion") {
      LOG.info("Downloading $ideVersion")
      val ideFile = getIdeFile(ideVersion)
      when (ideFile) {
        is IdeFilesBank.Result.Found -> ideFile
        is IdeFilesBank.Result.NotFound -> throw IllegalArgumentException("$ideVersion is not found: ${ideFile.reason}")
        is IdeFilesBank.Result.Failed -> throw IllegalArgumentException("$ideVersion couldn't be downloaded: ${ideFile.reason}", ideFile.exception)
      }
    }
  }

}
