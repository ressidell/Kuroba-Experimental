package com.github.k1rakishou.chan.ui.helper

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.isActive

suspend fun CoroutineScope.awaitWhile(
  maxWaitTimeMs: Long = 1000L,
  waitWhile: () -> Boolean
): Boolean {
  val waitUntil = SystemClock.elapsedRealtime() + maxWaitTimeMs

  while (isActive && waitWhile()) {
    awaitFrame()

    val currentTime = SystemClock.elapsedRealtime()
    if (currentTime >= waitUntil) {
      // Timed out
      return false
    }
  }

  return true
}