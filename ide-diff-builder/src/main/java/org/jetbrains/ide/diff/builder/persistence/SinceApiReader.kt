package org.jetbrains.ide.diff.builder.persistence

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.*
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.NameFileFilter
import org.apache.commons.io.filefilter.TrueFileFilter
import org.jetbrains.ide.diff.builder.api.ApiData
import org.jetbrains.ide.diff.builder.api.SinceApiData
import org.jetbrains.ide.diff.builder.signatures.ApiSignature
import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Utility class used to read [SinceApiData] from [annotationsRoot],
 * which may be a .zip or directory.
 *
 * This class is not thread-safe.
 */
class SinceApiReader(private val annotationsRoot: Path) : Closeable {

  /**
   * Sequence of [SinceApiXmlReader] for the given [annotationsRoot].
   * XML readers of this sequence get closed after fully processed.
   */
  private val xmlReaderSequence: XmlReaderSequence

  /**
   * XML reader used to read signature of the current "annotations.xml".
   */
  private var currentXmlReader: SinceApiXmlReader? = null

  init {
    require(annotationsRoot.isDirectory || annotationsRoot.extension == "zip") {
      "Only directory or .zip roots are supported"
    }
    xmlReaderSequence = buildXmlReaderSequence()
  }

  private fun buildXmlReaderSequence(): XmlReaderSequence =
      if (annotationsRoot.extension == "zip") {
        ZipXmlReaderSequence(ZipFile(annotationsRoot.toFile()))
      } else {
        val xmlFiles = FileUtils.listFiles(
            annotationsRoot.toFile(),
            NameFileFilter(ANNOTATIONS_XML_FILE_NAME),
            TrueFileFilter.INSTANCE
        ).map { it.toPath() }
        FilesXmlReaderSequence(xmlFiles)
      }

  /**
   * Reads external annotations from [annotationsRoot]
   * and returns corresponding [SinceApiData].
   */
  fun readSinceApiData(): SinceApiData {
    val versionToApiData = mutableMapOf<IdeVersion, ApiData>()
    for ((apiSignature, sinceVersion) in readAllSignatures()) {
      versionToApiData.getOrPut(sinceVersion) { ApiData() }
          .addSignature(apiSignature)
    }
    return SinceApiData(versionToApiData)
  }

  /**
   * Reads all [ApiSignature]s and "available since" versions
   * recorder in the specified [annotationsRoot].
   */
  fun readAllSignatures(): Sequence<Pair<ApiSignature, IdeVersion>> =
      generateSequence { readNextSignature() }

  /**
   * Reads next [ApiSignature] and "available since" version
   * specified in the [annotationsRoot].
   * Returns `null` if no more unread signatures left in `this` reader.
   */
  private fun readNextSignature(): Pair<ApiSignature, IdeVersion>? {
    while (true) {
      currentXmlReader = if (currentXmlReader == null) {
        if (xmlReaderSequence.hasNext()) {
          xmlReaderSequence.next()
        } else {
          return null
        }
      } else {
        val nextSignature = currentXmlReader!!.readNextSignature()
        if (nextSignature != null) {
          return nextSignature
        } else {
          currentXmlReader?.closeLogged()
          null
        }
      }
    }
  }

  override fun close() {
    currentXmlReader?.closeLogged()
    xmlReaderSequence.closeLogged()
  }

}

/**
 * Iterable sequence of [SinceApiXmlReader]s from a specific root.
 * - from a zip file - [ZipXmlReaderSequence]
 * - from multiple files - [FilesXmlReaderSequence]
 *
 * On [close], all allocated resources will be released. For [ZipXmlReaderSequence]
 * the ZipFile will be closed.
 */
private interface XmlReaderSequence : Iterator<SinceApiXmlReader>, Closeable

private class FilesXmlReaderSequence(files: List<Path>) : XmlReaderSequence {

  private val filesIterator = files.iterator()

  private var currentReader: SinceApiXmlReader? = null

  override fun hasNext(): Boolean {
    if (filesIterator.hasNext()) {
      val nextFile = filesIterator.next()
      currentReader = Files.newBufferedReader(nextFile).closeOnException {
        SinceApiXmlReader(it)
      }
      return true
    }
    return false
  }

  override fun next() = currentReader!!

  override fun close() {
    currentReader?.closeLogged()
  }
}

private class ZipXmlReaderSequence(val zipFile: ZipFile) : XmlReaderSequence {

  private val zipEntries = zipFile.entries()

  private var currentReader: SinceApiXmlReader? = null

  private fun getNextXmlEntry(): ZipEntry? {
    while (zipEntries.hasMoreElements()) {
      val zipEntry = zipEntries.nextElement()
      if (zipEntry.name.toSystemIndependentName().endsWith("/$ANNOTATIONS_XML_FILE_NAME")) {
        return zipEntry
      }
    }
    return null
  }

  override fun next(): SinceApiXmlReader = currentReader!!

  override fun hasNext(): Boolean {
    val xmlEntry = getNextXmlEntry() ?: return false
    currentReader = zipFile.getInputStream(xmlEntry).bufferedReader().closeOnException {
      SinceApiXmlReader(it)
    }
    return true
  }

  override fun close() {
    currentReader?.closeLogged()
    zipFile.closeLogged()
  }

}