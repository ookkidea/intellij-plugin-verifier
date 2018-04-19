package com.jetbrains.pluginverifier.results.deprecated

import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.FULL_HOST_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodParameterNameOption.WITH_PARAM_NAMES_IF_AVAILABLE
import com.jetbrains.pluginverifier.results.presentation.MethodParameterTypeOption.SIMPLE_PARAM_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodReturnTypeOption.SIMPLE_RETURN_TYPE_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.formatMethodLocation
import com.jetbrains.pluginverifier.results.presentation.methodOrConstructorWord

data class DeprecatedMethodUsage(override val deprecatedElement: MethodLocation,
                                 override val usageLocation: Location) : DeprecatedApiUsage() {
  override val shortDescription = "Deprecated " + deprecatedElement.methodOrConstructorWord + " usage ${deprecatedElement.formatMethodLocation(FULL_HOST_NAME, SIMPLE_PARAM_CLASS_NAME, SIMPLE_RETURN_TYPE_CLASS_NAME, WITH_PARAM_NAMES_IF_AVAILABLE)}"

  override val fullDescription = "Deprecated " + deprecatedElement.methodOrConstructorWord + " ${deprecatedElement.formatMethodLocation(FULL_HOST_NAME, SIMPLE_PARAM_CLASS_NAME, SIMPLE_RETURN_TYPE_CLASS_NAME, WITH_PARAM_NAMES_IF_AVAILABLE)} is used in " + usageLocation.formatDeprecatedUsageLocation()
}