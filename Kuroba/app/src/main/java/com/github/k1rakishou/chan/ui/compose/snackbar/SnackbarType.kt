package com.github.k1rakishou.chan.ui.compose.snackbar

import androidx.compose.runtime.Immutable

@Immutable
sealed class SnackbarType {
  val isToast: Boolean
    get() = this is Toast || this is ErrorToast

  data object Default : SnackbarType()
  data object Toast : SnackbarType()
  data object ErrorToast : SnackbarType()
}

@Immutable
sealed interface SnackbarScope {
  val mainLayoutAnchor: MainLayoutAnchor?

  val tag: String
    get() {
      val layoutAnchor = mainLayoutAnchor
      if (layoutAnchor != null) {
        return "SnackbarScope_${this::class.java.simpleName}_${layoutAnchor.name}"
      }

      return "SnackbarScope_${this::class.java.simpleName}"
    }

  data class Global(
    override val mainLayoutAnchor: MainLayoutAnchor? = null
  ) : SnackbarScope

  data class PostList(
    override val mainLayoutAnchor: MainLayoutAnchor? = null
  ) : SnackbarScope

  data class MediaViewer(
    override val mainLayoutAnchor: MainLayoutAnchor? = null
  ) : SnackbarScope

  data class Album(
    override val mainLayoutAnchor: MainLayoutAnchor
  ) : SnackbarScope

  enum class MainLayoutAnchor {
    Catalog,
    Thread
  }

}