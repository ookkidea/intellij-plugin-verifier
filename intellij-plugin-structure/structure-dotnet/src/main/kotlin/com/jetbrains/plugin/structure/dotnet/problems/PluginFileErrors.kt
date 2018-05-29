package com.jetbrains.plugin.structure.dotnet.problems

import com.jetbrains.plugin.structure.base.problems.IncorrectPluginFile
import java.io.File

data class IncorrectDotNetPluginFile(val file: File) :
    IncorrectPluginFile(file, ".nupkg archive.")
