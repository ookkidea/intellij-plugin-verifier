package com.jetbrains.plugin.structure.dotnet.beans

import com.jetbrains.plugin.structure.dotnet.DotNetDependency
import com.jetbrains.plugin.structure.dotnet.ReSharperPlugin
import org.jonnyzzz.kotlin.xml.bind.XAttribute
import org.jonnyzzz.kotlin.xml.bind.XElements
import org.jonnyzzz.kotlin.xml.bind.XSub
import org.jonnyzzz.kotlin.xml.bind.XText
import org.jonnyzzz.kotlin.xml.bind.jdom.JXML

class ReSharperPluginBean {
  val id by JXML / "metadata" / "id" / XText
  val version by JXML / "metadata" / "version" / XText
  val description by JXML / "metadata" / "description" / XText
  val url by JXML / "metadata" / "projectUrl" / XText
  val changeNotes by JXML / "metadata" / "releaseNotes" / XText
  val dependencies by JXML / "metadata" / "dependencies" / XElements("dependency") / XSub(DotNetDependencyBean::class.java)
}

class DotNetDependencyBean {
  val id by JXML / XAttribute("id")
  val version by JXML / XAttribute("version")
}

fun ReSharperPluginBean.toPlugin() = ReSharperPlugin(
    pluginId = this.id!!,
    pluginName = this.id?.split(".")?.get(1)!!,
    vendor = this.id?.split(".")?.get(0)!!,
    pluginVersion = this.version!!,
    url = this.url,
    changeNotes = this.changeNotes,
    description = this.description,
    vendorEmail = null,
    vendorUrl = null,
    dependencies = dependencies?.map { DotNetDependency(it.id!!, it.version!!) } ?: emptyList()
//    sinceBuild = this.minBuild?.toLong()?.let { TeamcityVersion(it) },
//    untilBuild = this.maxBuild?.toLong()?.let { TeamcityVersion(it) },
//    downloadUrl = this.downloadUrl,
//    useSeparateClassLoader = this.useSeparateClassLoader?.toBoolean() ?: false,
//    parameters = this.parameters?.associate { it.name!! to it.value!! }
)