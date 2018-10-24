package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.classes.IdeResolverCreator
import com.jetbrains.pluginverifier.verifiers.*
import org.jetbrains.ide.diff.builder.signatures.toSignature
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

/**
 * Builder of [SinceApiData] by APIs difference of two IDEs.
 */
class SinceApiBuilder(private val interestingPackages: List<String> = INTELLIJ_PACKAGES) {

  companion object {
    val INTELLIJ_PACKAGES = listOf("org.jetbrains", "com.jetbrains", "org.intellij", "com.intellij")
  }

  fun build(oldIde: Ide, newIde: Ide): SinceApiData {

    val apiData = ApiData()

    IdeResolverCreator.createIdeResolver(oldIde).use { oldResolver ->
      IdeResolverCreator.createIdeResolver(newIde).use { newResolver ->
        newResolver.processAllClasses { newClass ->
          if (newClass.ignoreClass()) {
            return@processAllClasses true
          }

          val oldClass = try {
            oldResolver.findClass(newClass.name)
          } catch (e: Exception) {
            null
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
            for (methodNode in newClass.getMethods().orEmpty().filterNot { it.ignoreMethod() }) {
              apiData.addSignature(createMethodLocation(newClass, methodNode).toSignature())
            }
            for (fieldNode in newClass.getFields().orEmpty().filterNot { it.ignoreField() }) {
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
      if (newMethod.ignoreMethod()) {
        continue
      }

      val oldMethod = oldClass.getMethods()?.find { it.name == newMethod.name && it.desc == newMethod.desc }
      if (oldMethod == null) {
        apiDiff.addSignature(createMethodLocation(newClass, newMethod).toSignature())
      }
    }

    for (newField in newClass.getFields().orEmpty()) {
      if (newField.ignoreField()) {
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
    val packageName = substringBeforeLast("/", "").replace('/', '.')
    return interestingPackages.none { packageName == it || packageName.startsWith("$it.") }
  }

  private fun ClassNode.ignoreClass() = isPrivate() || isSynthetic() || name.isSyntheticLikeName() || name.hasIgnoredPackage()

  private fun MethodNode.ignoreMethod() = isPrivate() || isSynthetic() || name.isSyntheticLikeName()

  private fun FieldNode.ignoreField() = isPrivate() || isSynthetic() || name.isSyntheticLikeName()

}