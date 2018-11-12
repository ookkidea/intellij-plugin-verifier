package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.classes.IdeResolverCreator
import com.jetbrains.pluginverifier.verifiers.*
import org.jetbrains.ide.diff.builder.signatures.getJavaPackageNameByJvmClassName
import org.jetbrains.ide.diff.builder.signatures.toSignature
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Builder of [SinceApiData] by APIs difference of two IDEs.
 */
class SinceApiBuilder(private val interestingPackages: List<String> = INTELLIJ_PACKAGES) {

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(SinceApiBuilder::class.java)

    val INTELLIJ_PACKAGES = listOf("org.jetbrains", "com.jetbrains", "org.intellij", "com.intellij")
  }

  fun build(oldIde: Ide, newIde: Ide): SinceApiData {

    val apiData = ApiData()

    IdeResolverCreator.createIdeResolver(oldIde).use { oldResolver ->
      IdeResolverCreator.createIdeResolver(newIde).use { newResolver ->
        newResolver.processAllClasses { newClass ->
          if (newClass.isIgnored()) {
            return@processAllClasses true
          }

          val oldClass = try {
            oldResolver.findClass(newClass.name)
          } catch (e: Exception) {
            LOG.warn("Class file ${newClass.name} couldn't be read from $oldIde distribution", e)
            return@processAllClasses true
          }

          if (oldClass == null) {
            /**
             * Register the new class and all its stuff.
             *
             * Inner and nested classes will be processed separately
             * in this `processAllClasses`, so here is no need
             * to handle them.
             */
            apiData.addSignature(newClass.createClassLocation().toSignature())
            for (methodNode in newClass.getMethods().orEmpty().filterNot { it.isIgnored() }) {
              apiData.addSignature(createMethodLocation(newClass, methodNode).toSignature())
            }
            for (fieldNode in newClass.getFields().orEmpty().filterNot { it.isIgnored() }) {
              apiData.addSignature(createFieldLocation(newClass, fieldNode).toSignature())
            }
            return@processAllClasses true
          }

          compareClasses(oldClass, newClass, apiData)
          true
        }
      }
    }

    return SinceApiData(mapOf(newIde.version to apiData))
  }

  private fun compareClasses(oldClass: ClassNode, newClass: ClassNode, apiDiff: ApiData) {
    for (newMethod in newClass.getMethods().orEmpty()) {
      if (newMethod.isIgnored()) {
        continue
      }

      val oldMethod = oldClass.getMethods()?.find { it.name == newMethod.name && it.desc == newMethod.desc }
      if (oldMethod == null) {
        apiDiff.addSignature(createMethodLocation(newClass, newMethod).toSignature())
      }
    }

    for (newField in newClass.getFields().orEmpty()) {
      if (newField.isIgnored()) {
        continue
      }

      val oldField = oldClass.getFields()?.find { it.name == newField.name && it.desc == newField.desc }
      if (oldField == null) {
        apiDiff.addSignature(createFieldLocation(newClass, newField).toSignature())
      }
    }
  }

  private fun String.isSyntheticLikeName() = contains('$') && substringAfterLast('$', "").toIntOrNull() != null

  private fun String.hasIgnoredPackage(): Boolean {
    val packageName = getJavaPackageNameByJvmClassName()
    return interestingPackages.none { packageName == it || packageName.startsWith("$it.") }
  }

  /**
   * Returns `true` if this class is likely an implementation of something.
   * `org.some.ServiceImpl` -> true
   */
  private fun String.hasImplementationLikeName() = endsWith("Impl")

  /**
   * Returns `true` if this package is likely a package containing implementation of some APIs.
   * `org.some.impl.services` -> true
   */
  private fun String.hasImplementationLikePackage(): Boolean {
    val packageName = getJavaPackageNameByJvmClassName()
    return ".impl." in packageName
  }

  private fun ClassNode.isIgnored() = isPrivate()
      || isSynthetic()
      || name.isSyntheticLikeName()
      || name.hasIgnoredPackage()
      || name.hasImplementationLikeName()
      || name.hasImplementationLikePackage()

  private fun MethodNode.isIgnored() = isPrivate() || isSynthetic() || name.isSyntheticLikeName()

  private fun FieldNode.isIgnored() = isPrivate() || isSynthetic() || name.isSyntheticLikeName()

}