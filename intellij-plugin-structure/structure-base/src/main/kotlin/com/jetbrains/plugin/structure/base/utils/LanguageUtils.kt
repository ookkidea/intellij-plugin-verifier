package com.jetbrains.plugin.structure.base.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable

private val logger: Logger = LoggerFactory.getLogger("LanguageUtils")

/**
 * Checks whether the current thread has been interrupted.
 * Clears the *interrupted status* and throws [InterruptedException]
 * if it is the case.
 */
@Throws(InterruptedException::class)
fun checkIfInterrupted() {
  if (Thread.interrupted()) {
    throw InterruptedException()
  }
}

fun <T : Closeable?> T.closeLogged() {
  try {
    this?.close()
  } catch (e: Exception) {
    logger.error("Unable to close $this", e)
  }
}

inline fun <T : Closeable?, R> T.closeOnException(block: (T) -> R): R {
  try {
    return block(this)
  } catch (e: Throwable) {
    this?.closeLogged()
    throw e
  }
}

fun <T> Iterator<T>.toList() = asSequence().toList()

fun <T> Iterator<T>.toSet() = asSequence().toSet()