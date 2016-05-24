package com.intellij.structure.domain;

import com.intellij.structure.errors.IncorrectPluginException;
import com.intellij.structure.impl.domain.PluginManagerImpl;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * @author Sergey Patrikeev
 */
public abstract class PluginManager {

  @NotNull
  public static PluginManager getInstance() {
    return new PluginManagerImpl();
  }

  @NotNull
  final public Plugin createPlugin(@NotNull File pluginFile) throws IOException, IncorrectPluginException {
    return createPlugin(pluginFile, true);
  }

  @NotNull
  public abstract Plugin createPlugin(@NotNull File pluginFile, boolean validatePluginXml) throws IOException, IncorrectPluginException;

}