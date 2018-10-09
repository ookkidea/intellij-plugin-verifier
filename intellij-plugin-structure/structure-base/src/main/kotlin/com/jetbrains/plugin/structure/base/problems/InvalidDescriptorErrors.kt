package com.jetbrains.plugin.structure.base.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

abstract class InvalidDescriptorProblem(private val descriptorPath: String?) : PluginProblem() {
  abstract val detailedMessage: String

  override val message
    get() = "Invalid plugin descriptor" + (if (descriptorPath.isNullOrEmpty()) ": " else " $descriptorPath: ") + detailedMessage
}

class UnexpectedDescriptorElements(
    override val detailedMessage: String,
    descriptorPath: String? = null
) : InvalidDescriptorProblem(descriptorPath) {

  override val level
    get() = PluginProblem.Level.ERROR

}

class PropertyNotSpecified(
    private val propertyName: String,
    descriptorPath: String? = null
) : InvalidDescriptorProblem(descriptorPath) {

  override val detailedMessage: String
    get() = "<$propertyName> is not specified"

  override val level
    get() = Level.ERROR
}

class NotNumber(
    private val propertyName: String,
    descriptorPath: String? = null
) : InvalidDescriptorProblem(descriptorPath) {

  override val detailedMessage: String
    get() = "<$propertyName> must be of type int"

  override val level
    get() = Level.ERROR
}