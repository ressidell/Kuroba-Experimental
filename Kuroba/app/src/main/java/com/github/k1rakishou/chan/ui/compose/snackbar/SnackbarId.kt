package com.github.k1rakishou.chan.ui.compose.snackbar

@JvmInline
value class SnackbarId(
  val id: String
)

fun String.asSnackbarId(): SnackbarId {
  return SnackbarId(this)
}