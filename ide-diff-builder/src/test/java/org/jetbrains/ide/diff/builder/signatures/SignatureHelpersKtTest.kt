package org.jetbrains.ide.diff.builder.signatures

import org.junit.Assert.assertEquals
import org.junit.Test

class SignatureHelpersKtTest {
  @Test
  fun `test package name extraction`() {
    assertEquals("org.some", "org.some.Class".getPackageName())
    assertEquals("org.some", "org.some.Class.Inner".getPackageName())
    assertEquals("org.some", "org.some.Class$1".getPackageName())
    assertEquals("", "Some".getPackageName())
  }
}