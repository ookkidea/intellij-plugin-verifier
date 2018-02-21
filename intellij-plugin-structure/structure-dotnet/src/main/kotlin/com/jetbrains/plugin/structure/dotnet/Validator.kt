package com.jetbrains.plugin.structure.dotnet

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.dotnet.beans.ReSharperPluginBean
import com.jetbrains.plugin.structure.dotnet.problems.InvalidIdError

internal fun validateDotNetPluginBean(bean: ReSharperPluginBean): List<PluginProblem> {
  val problems = mutableListOf<PluginProblem>()

  val dependencies = bean.dependencies
  val id = bean.id

  if(id.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("id"))
  }

  if (dependencies != null && dependencies.any { it.id == "Wave" }) {
    if (id != null && id.count { it == '.' } != 1) {
      problems.add(InvalidIdError)
    }
  }
  return problems
}
