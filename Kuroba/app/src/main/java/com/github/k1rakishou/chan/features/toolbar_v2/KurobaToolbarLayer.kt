package com.github.k1rakishou.chan.features.toolbar_v2

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.github.k1rakishou.chan.features.toolbar_v2.state.IKurobaToolbarState

data class KurobaToolbarLayer(
  val toolbarLayerId: KurobaToolbarState.ToolbarLayerId,
  val kurobaToolbarState: IKurobaToolbarState,
  val leftIcon: ToolbarMenuItem? = null,
  val middleContent: ToolbarMiddleContent? = null,
  val toolbarMenu: ToolbarMenu = ToolbarMenu(),
  val iconClickInterceptor: (ToolbarMenuItem) -> Boolean
)

data class ToolbarMenu(
  val menuItems: SnapshotStateList<ToolbarMenuItem> = mutableStateListOf(),
  val overflowMenuItems: SnapshotStateList<AbstractToolbarMenuOverflowItem> = mutableStateListOf()
)

@Deprecated("Remove me!")
data class DeprecatedNavigationFlags(
  val hasBack: Boolean = true,
  val hasDrawer: Boolean = false,
  val replyOpened: Boolean = false,
  val swipeable: Boolean = true,
  val scrollableTitle: Boolean = false
)