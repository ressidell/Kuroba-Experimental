package com.github.k1rakishou.chan.features.toolbar_v2.state

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.github.k1rakishou.chan.features.toolbar_v2.AbstractToolbarMenuOverflowItem
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMenu
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMenuCheckableOverflowItem
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMenuItem
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMenuOverflowItem

abstract class KurobaToolbarSubState {
  abstract val kind: ToolbarStateKind
  abstract val leftMenuItem: ToolbarMenuItem?
  abstract val rightToolbarMenu: ToolbarMenu?

  private val _toolbarBadgeState = mutableStateOf<ToolbarBadge?>(null)
  val toolbarBadgeState: State<ToolbarBadge?>
    get() = _toolbarBadgeState

  abstract fun update(params: IKurobaToolbarParams)
  abstract fun onPushed()
  abstract fun onPopped()

  fun findItem(id: Int): ToolbarMenuItem? {
    if (leftMenuItem?.id == id) {
      return leftMenuItem
    }

    val toolbarMenu = rightToolbarMenu
      ?: return null

    for (menuItem in toolbarMenu.menuItems) {
      if (menuItem.id == id) {
        return menuItem
      }
    }

    return null
  }

  fun findOverflowItem(
    id: Int,
    overflowMenuItems: List<AbstractToolbarMenuOverflowItem>? = rightToolbarMenu?.overflowMenuItems?.toList()
  ): ToolbarMenuOverflowItem? {
    if (overflowMenuItems == null) {
      return null
    }

    for (overflowMenuItem in overflowMenuItems) {
      if (overflowMenuItem is ToolbarMenuOverflowItem && overflowMenuItem.id == id) {
        return overflowMenuItem
      }

      val foundItem = findOverflowItem(id, overflowMenuItem.subItems)
      if (foundItem != null) {
        return foundItem
      }
    }

    return null
  }

  fun findCheckableOverflowItem(
    id: Int,
    overflowMenuItems: List<AbstractToolbarMenuOverflowItem>? = rightToolbarMenu?.overflowMenuItems?.toList()
  ): ToolbarMenuCheckableOverflowItem? {
    if (overflowMenuItems == null) {
      return null
    }

    for (overflowMenuItem in overflowMenuItems) {
      if (overflowMenuItem is ToolbarMenuCheckableOverflowItem && overflowMenuItem.id == id) {
        return overflowMenuItem
      }

      val foundItem = findCheckableOverflowItem(id, overflowMenuItem.subItems)
      if (foundItem != null) {
        return foundItem
      }
    }

    return null
  }

  fun checkOrUncheckItem(
    subItem: ToolbarMenuCheckableOverflowItem,
    check: Boolean,
    overflowMenuItems: List<AbstractToolbarMenuOverflowItem>? = rightToolbarMenu?.overflowMenuItems?.toList()
  ) {
    if (overflowMenuItems == null) {
      return
    }

    val groupId = subItem.groupId

    for (overflowMenuItem in overflowMenuItems) {
      if (overflowMenuItem is ToolbarMenuCheckableOverflowItem) {
        if (overflowMenuItem.id == subItem.id) {
          overflowMenuItem.updateChecked(check)
        } else if (groupId != null && overflowMenuItem.groupId == groupId) {
          overflowMenuItem.updateChecked(false)
        }
      }

      checkOrUncheckItem(subItem, check, overflowMenuItem.subItems)
    }
  }

  fun updateBadge(counter: Int, highImportance: Boolean) {
    _toolbarBadgeState.value = ToolbarBadge(
      counter = counter,
      highlight = highImportance
    )
  }

  fun hideBadge() {
    _toolbarBadgeState.value = null
  }

}

enum class ToolbarStateKind {
  Container,
  Catalog,
  Thread,
  Default,
  Search,
  Selection,
  Reply
}

data class ToolbarBadge(
  val counter: Int,
  val highlight: Boolean
)