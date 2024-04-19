package com.github.k1rakishou.chan.ui.compose.snackbar

import android.os.SystemClock
import androidx.compose.runtime.Stable
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration


@Stable
data class SnackbarInfo(
  val snackbarId: SnackbarId,
  val createdAt: Long = SystemClock.elapsedRealtime(),
  val aliveUntil: Long?,
  val content: List<SnackbarContentItem>,
  val snackbarType: SnackbarType = SnackbarType.Default,
  val snackbarScope: SnackbarScope
) {
  val snackbarIdForCompose: Long = nextSnackbarIdForCompose()

  val hasClickableItems: Boolean
    get() = content.any { contentItem -> contentItem is SnackbarClickable }

  fun contentsEqual(other: SnackbarInfo): Boolean {
    return content == other.content
  }

  companion object {
    private val snackbarIdsForCompose = AtomicLong(0)

    fun nextSnackbarIdForCompose(): Long = snackbarIdsForCompose.incrementAndGet()

    fun snackbarDuration(duration: Duration): Long {
      return SystemClock.elapsedRealtime() + duration.inWholeMilliseconds
    }
  }

}