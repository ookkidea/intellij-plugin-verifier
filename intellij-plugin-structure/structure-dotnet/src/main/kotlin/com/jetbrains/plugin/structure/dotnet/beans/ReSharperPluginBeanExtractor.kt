package com.jetbrains.plugin.structure.dotnet.beans

import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.input.SAXBuilder
import org.jonnyzzz.kotlin.xml.bind.jdom.JDOM
import java.io.InputStream

fun extractPluginBean(inputStream: InputStream): ReSharperPluginBean {
  val rootElement = SAXBuilder().build(inputStream).rootElement
  rootElement.namespace = Namespace.NO_NAMESPACE
  rootElement.descendants.forEach {
    if(it is Element) {
      it.namespace = Namespace.NO_NAMESPACE
    }
  }
  return JDOM.load(rootElement, ReSharperPluginBean::class.java)
}