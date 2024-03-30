package com.github.k1rakishou.chan.controller

@Deprecated("Remove me!")
data class DeprecatedNavigationFlags(
  val hasBack: Boolean = true,
  val hasDrawer: Boolean = false,
  val replyOpened: Boolean = false,
  val swipeable: Boolean = true
)