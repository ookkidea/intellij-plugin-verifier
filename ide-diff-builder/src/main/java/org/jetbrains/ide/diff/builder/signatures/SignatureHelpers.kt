package org.jetbrains.ide.diff.builder.signatures

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.results.presentation.JvmDescriptorsPresentation
import com.jetbrains.pluginverifier.results.presentation.toFullJavaClassName
import com.jetbrains.pluginverifier.results.signatures.FormatOptions
import com.jetbrains.pluginverifier.results.signatures.SigVisitor
import org.apache.commons.text.StringEscapeUtils
import org.objectweb.asm.signature.SignatureReader

/**
 * Returns package name of a fully qualified class name.
 *
 * It attempts to recognise inner/nested classes,
 * by checking whether the first letter is capital or not,
 * but may fail to do so, if the package is named with
 * capital letter for some reason.
 */
fun String.getPackageName(): String {
  if ('.' !in this) {
    return ""
  }
  for (index in indices) {
    if (get(index) == '.' && index + 1 < length && get(index + 1).isUpperCase()) {
      return substring(0, index)
    }
  }
  return substringBeforeLast(".", "")
}

/**
 * Escapes HTML characters in `this` String.
 */
fun String.escapeHtml(): String = StringEscapeUtils.escapeHtml4(this)

/**
 * Unescapes HTML characters in `this` String.
 */
fun String.unescapeHtml(): String = StringEscapeUtils.unescapeHtml4(this)


fun ClassLocation.toSignature(): ClassSignature =
    ClassSignature(toFullJavaClassName(className))

fun MethodLocation.toSignature(): MethodSignature {
  val hostName = toFullJavaClassName(hostClass.className)
  var methodName: String = methodName
  var retType: String? = null

  var dropFirstParameter = false

  val (paramTypes, returnType) = convertMethodSignature()

  if (methodName == "<init>") {
    /**
     * Constructor name is equal to simple class name.
     */
    methodName = hostClass.className.substringAfterLast('/').substringAfterLast('$')
    /**
     * Constructors of static inner classes (aka nested classes)
     * don't contain the enclosing class in the signature,
     * so we can simply print all the parameters.
     *
     * For inner non-static classes there is implicit
     * first parameter of enclosing class,
     * but in the external name it must be ignored.
     */
    if (!modifiers.contains(Modifiers.Modifier.STATIC)) {
      dropFirstParameter = true
    }
  } else {
    retType = returnType
  }

  val paramsSignatures = paramTypes.drop(if (dropFirstParameter) 1 else 0).joinToString()
  return MethodSignature(
      hostName,
      methodName,
      retType,
      paramsSignatures
  )
}

fun FieldLocation.toSignature() =
    FieldSignature(toFullJavaClassName(hostClass.className), fieldName)

private fun MethodLocation.convertMethodSignature(): Pair<List<String>, String> {
  return if (signature.isNotEmpty()) {
    val methodSignature = SigVisitor().also { SignatureReader(signature).accept(it) }.getMethodSignature()
    val formatOptions = FormatOptions(
        isInterface = false,
        formalTypeParameters = false,
        formalTypeParametersBounds = false,
        methodThrows = false,
        internalNameConverter = toFullJavaClassName,

        typeArguments = true,
        superClass = true,
        superInterfaces = true,

        /**
         * Type arguments in method signature parameters and return type
         * are not separated with ", " but with ",".
         * This is due to "canonical" presentation of those parameters
         * used in [com.intellij.psi.util.PsiFormatUtil#formatType].
         */
        typeArgumentsSeparator = ","
    )
    val resultType = methodSignature.result.format(formatOptions)
    val parametersTypes = methodSignature.parameterSignatures.map { it.format(formatOptions) }
    parametersTypes to resultType
  } else {
    val (paramsT, retT) = JvmDescriptorsPresentation.splitMethodDescriptorOnRawParametersAndReturnTypes(methodDescriptor)
    paramsT.map { JvmDescriptorsPresentation.convertJvmDescriptorToNormalPresentation(it, toFullJavaClassName) } to JvmDescriptorsPresentation.convertJvmDescriptorToNormalPresentation(retT, toFullJavaClassName)
  }
}

/**
 * Converts the external name, which might be obtained
 * in [ApiSignature.externalPresentation],
 * to the original API signature.
 */
fun parseApiSignature(externalName: String): ApiSignature {
  if ('(' in externalName) {
    /**
     * This is a method signature, like
     * com.some.Class void foo(int)
     *
     * or a constructor signature, like
     * com.some.Class Class()
     */
    val className = externalName.substringBefore(' ')
    val methodName = externalName.substringBefore('(').substringAfterLast(' ')
    val returnType = externalName.substringAfter("$className ", "").substringBefore(" $methodName(", "").takeIf { it.isNotEmpty() }
    val paramSignatures = externalName.substringAfter('(').substringBefore(')')
    return MethodSignature(className, methodName, returnType, paramSignatures)
  }
  if (' ' in externalName) {
    /**
     * This is a field signature, like
     * com.some.Class field
     */
    val className = externalName.substringBefore(' ')
    val fieldName = externalName.substringAfter(' ')
    return FieldSignature(className, fieldName)
  }
  return ClassSignature(externalName)
}

