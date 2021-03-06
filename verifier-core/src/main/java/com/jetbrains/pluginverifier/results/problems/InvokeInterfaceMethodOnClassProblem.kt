package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import java.util.*

class InvokeInterfaceMethodOnClassProblem(
    val methodReference: MethodReference,
    val caller: MethodLocation,
    val instruction: Instruction
) : CompatibilityProblem() {

  override val problemType
    get() = "Incompatible change of interface to class"

  val changedInterface: ClassReference
    get() = methodReference.hostClass

  override val shortDescription
    get() = "Incompatible change of interface {0} to class".formatMessage(changedInterface)

  override val fullDescription
    get() = "Method {0} has invocation *{1}* instruction referencing an *interface* method {2}, but the method''s host {3} is a *class*. This can lead to **IncompatibleClassChangeError** at runtime.".formatMessage(caller, instruction, methodReference, methodReference.hostClass)

  override fun equals(other: Any?) = other is InvokeInterfaceMethodOnClassProblem
      && methodReference == other.methodReference
      && caller == other.caller
      && instruction == other.instruction

  override fun hashCode() = Objects.hash(methodReference, caller, instruction)

}