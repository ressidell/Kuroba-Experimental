package com.github.k1rakishou.chan.features.toolbar.state

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.github.k1rakishou.chan.features.toolbar.AbstractToolbarMenuOverflowItem
import com.github.k1rakishou.chan.features.toolbar.ToolbarMenu
import com.github.k1rakishou.chan.features.toolbar.ToolbarMenuCheckableOverflowItem
import com.github.k1rakishou.chan.features.toolbar.ToolbarMenuItem
import com.github.k1rakishou.chan.features.toolbar.ToolbarMenuOverflowItem
import com.github.k1rakishou.chan.ui.compose.badge.ToolbarBadgeData

abstract class KurobaToolbarSubState {
  abstract val kind: ToolbarStateKind
  abstract val leftMenuItem: ToolbarMenuItem?
  abstract val rightToolbarMenu: ToolbarMenu?

  private val _toolbarBadgeState = mutableStateOf<ToolbarBadgeData?>(null)
  val toolbarBadgeState: State<ToolbarBadgeData?>
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
    _toolbarBadgeState.value = ToolbarBadgeData(
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