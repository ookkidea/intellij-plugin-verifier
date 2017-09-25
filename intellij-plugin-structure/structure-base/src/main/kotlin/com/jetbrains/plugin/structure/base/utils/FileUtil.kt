package com.jetbrains.plugin.structure.base.utils

import com.google.common.io.Files
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException

/**
 * @author Sergey Patrikeev
 */
object FileUtil {

  private val TEMP_DIR_ATTEMPTS = 10000

  //it's synchronized because otherwise there is a possibility of two threads creating the same directory
  @Synchronized
  fun createTempDir(parent: File, prefix: String): File {
    val baseName = prefix + "_" + System.currentTimeMillis()
    var lastException: IOException? = null
    for (counter in 0..TEMP_DIR_ATTEMPTS - 1) {
      val tempDir = File(parent, baseName + "_" + counter)
      if (!tempDir.exists()) {
        try {
          FileUtils.forceMkdir(tempDir)
          return tempDir
        } catch (ioe: IOException) {
          lastException = ioe
        }

      }
    }
    throw IllegalStateException("Failed to create directory under " + parent.absolutePath + " within "
        + TEMP_DIR_ATTEMPTS + " attempts (tried "
        + baseName + "_0 to " + baseName + "_" + (TEMP_DIR_ATTEMPTS - 1) + ')', lastException)
  }

  fun createDir(dir: File): File {
    if (!dir.isDirectory) {
      FileUtils.forceMkdir(dir)
      if (!dir.isDirectory) {
        throw IOException("Failed to create directory $dir")
      }
    }
    return dir
  }

  private fun hasExtension(file: File, extension: String): Boolean =
      file.isFile && extension == Files.getFileExtension(file.name)

  fun isZip(file: File): Boolean = hasExtension(file, "zip")

  fun isJar(file: File): Boolean = hasExtension(file, "jar")
}
