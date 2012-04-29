package com.jetbrains.pluginverifier.domain;

import com.jetbrains.pluginverifier.pool.ClassPool;
import com.jetbrains.pluginverifier.resolvers.ClassPoolResolver;
import com.jetbrains.pluginverifier.resolvers.Resolver;
import com.jetbrains.pluginverifier.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

public class JDK {
  private final File myJdkDir;
  private final List<JarFile> myJars;
  private final ClassPool myPool;

  public JDK(final File jdkDir) throws IOException {
    myJdkDir = jdkDir;
    myJars = new ArrayList<JarFile>();

    collectJars(jdkDir);
    myPool = Util.makeClassPool(myJdkDir.getPath(), myJars);
  }

  private void collectJars(File dir) throws IOException {
    myJars.addAll(Util.getJars(dir));

    final File[] files = dir.listFiles();
    if (files == null)
      return;

    for (File file : files) {
      if (file.isDirectory())
        collectJars(file);
    }
  }

  public Resolver getResolver() {
    return new ClassPoolResolver(myPool);
  }
}
