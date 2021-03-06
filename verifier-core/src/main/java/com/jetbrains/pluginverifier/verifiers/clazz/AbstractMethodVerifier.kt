package com.jetbrains.pluginverifier.verifiers.clazz

import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.problems.MethodNotImplementedProblem
import com.jetbrains.pluginverifier.verifiers.*
import com.jetbrains.pluginverifier.verifiers.logic.hierarchy.ClassParentsVisitor
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class AbstractMethodVerifier : ClassVerifier {
  override fun verify(clazz: ClassNode, ctx: VerificationContext) {
    if (clazz.isAbstract() || clazz.isInterface()) return

    val abstractMethods = hashMapOf<MethodSignature, MethodLocation>()
    val implementedMethods = hashMapOf<MethodSignature, MethodLocation>()

    ClassParentsVisitor(ctx, true).visitClass(clazz, true, onEnter = { parent ->
      @Suppress("UNCHECKED_CAST")
      (parent.methods as List<MethodNode>).forEach { method ->
        if (!method.isPrivate() && !method.isStatic()) {
          val methodLocation = createMethodLocation(parent, method)
          val methodSignature = MethodSignature(method.name, method.desc)
          if (method.isAbstract()) {
            abstractMethods.put(methodSignature, methodLocation)
          } else {
            implementedMethods.put(methodSignature, methodLocation)
          }
        }
      }
      true
    })

    val currentClass = clazz.createClassLocation()
    (abstractMethods.keys - implementedMethods.keys).forEach { method ->
      val abstractMethod = abstractMethods[method]!!
      ctx.registerProblem(MethodNotImplementedProblem(abstractMethod, currentClass))
    }
  }

  private data class MethodSignature(val name: String, val descriptor: String)

}