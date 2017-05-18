package com.jetbrains.pluginverifier.verifiers.clazz

import com.jetbrains.pluginverifier.problems.InheritFromFinalClassProblem
import com.jetbrains.pluginverifier.utils.VerificationContext
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode

class InheritFromFinalClassVerifier : ClassVerifier {
  override fun verify(clazz: ClassNode, ctx: VerificationContext) {
    val superClassName = clazz.superName ?: "java/lang/Object"
    val supClass = VerifierUtil.resolveClassOrProblem(superClassName, clazz, ctx, { ctx.fromClass(clazz) }) ?: return
    if (VerifierUtil.isFinal(supClass)) {
      val child = ctx.fromClass(clazz)
      val finalClass = ctx.fromClass(supClass)
      ctx.registerProblem(InheritFromFinalClassProblem(child, finalClass))
    }
  }
}
