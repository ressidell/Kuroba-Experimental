package com.github.k1rakishou.chan.ui.compose.snackbar

import androidx.compose.runtime.Immutable

@Immutable
sealed class SnackbarAnimation {
  abstract val snackbarId: Long

  data class Push(
    override val snackbarId: Long
  ) : SnackbarAnimation()

  data class Pop(
    override val snackbarId: Long
  ) : SnackbarAnimation()

  data class Remove(
    override val snackbarId: Long
  ) : SnackbarAnimation()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as SnackbarAnimation

    if (snackbarId != other.snackbarId) return false

    return true
  }

  override fun hashCode(): Int {
    return snackbarId.hashCode()
  }

}