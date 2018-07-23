package com.jetbrains.plugin.structure.dotnet

import com.jetbrains.plugin.structure.base.plugin.*
import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.dotnet.beans.extractPluginBean
import com.jetbrains.plugin.structure.dotnet.beans.toPlugin
import org.jdom2.input.JDOMParseException
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

@Suppress("unused")
object ReSharperPluginManager : PluginManager<ReSharperPlugin> {
  private val LOG = LoggerFactory.getLogger(ReSharperPluginManager::class.java)

  override fun createPlugin(pluginFile: File): PluginCreationResult<ReSharperPlugin> {
    if (!pluginFile.exists()) {
      throw IllegalArgumentException("Plugin file $pluginFile does not exist")
    }
    return when (pluginFile.extension) {
      "nupkg" -> loadDescriptorFromZip(pluginFile)
      else -> PluginCreationFail(IncorrectPluginFile(pluginFile))
    }
  }

  private fun loadDescriptorFromZip(pluginFile: File): PluginCreationResult<ReSharperPlugin> = try {
    loadDescriptorFromZip(ZipFile(pluginFile))
  } catch (e: IOException) {
    LOG.info("Unable to extract plugin zip: $pluginFile", e)
    PluginCreationFail(UnableToExtractZip(pluginFile))
  }

  private fun loadDescriptorFromZip(pluginFile: ZipFile): PluginCreationResult<ReSharperPlugin> {
    pluginFile.use {
      var nugetDescriptor: ZipEntry? = null
      for (entry in pluginFile.entries()) {
        if (!entry.name.endsWith(".nuspec")) {
          continue
        }
        if (nugetDescriptor != null) {
          return PluginCreationFail(PluginDescriptorIsNotFound(""))
        }
        nugetDescriptor = entry
      }

      if (nugetDescriptor == null) {
        return PluginCreationFail(PluginDescriptorIsNotFound(""))
      }
      return loadDescriptorFromStream(nugetDescriptor.name, pluginFile.getInputStream(nugetDescriptor))
    }
  }

  private fun loadDescriptorFromStream(streamName: String, inputStream: InputStream): PluginCreationResult<ReSharperPlugin> {
    try {
      val bean = extractPluginBean(inputStream)
      val beanValidationResult = validateDotNetPluginBean(bean)
      if (beanValidationResult.any { it.level == PluginProblem.Level.ERROR }) {
        return PluginCreationFail(beanValidationResult)
      }
      return PluginCreationSuccess(bean.toPlugin(), beanValidationResult)
    } catch (e: JDOMParseException) {
      val lineNumber = e.lineNumber
      val message = if (lineNumber != -1) "unexpected element on line " + lineNumber else "unexpected elements"
      return PluginCreationFail(UnexpectedDescriptorElements(message))
    } catch (e: Exception) {
      LOG.info("Unable to read plugin descriptor from $streamName", e)
      return PluginCreationFail(UnableToReadDescriptor(streamName))
    }
  }
}