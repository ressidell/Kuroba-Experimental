package com.github.k1rakishou.chan.features.toolbar_v2.state

import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMenu
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMenuCheckableOverflowItem
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMenuItem
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMenuOverflowItem

interface IKurobaToolbarState {
  val kind: ToolbarStateKind
  val leftMenuItem: ToolbarMenuItem?
  val rightToolbarMenu: ToolbarMenu?

  fun update(params: IKurobaToolbarParams)
  fun onPushed()
  fun onPopped()

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

  fun findOverflowItem(id: Int): ToolbarMenuOverflowItem? {
    val overflowMenuItems = rightToolbarMenu?.overflowMenuItems
      ?: return null

    val currentOverflowMenuItems = mutableListOf<ToolbarMenuOverflowItem>()
    currentOverflowMenuItems += overflowMenuItems
      .filterIsInstance<ToolbarMenuOverflowItem>()

    for (overflowMenuItem in currentOverflowMenuItems) {
      if (overflowMenuItem.id == id) {
        return overflowMenuItem
      }

      overflowMenuItems += overflowMenuItem.subItems
        .filterIsInstance<ToolbarMenuOverflowItem>()
    }

    return null
  }

  fun findCheckableOverflowItem(id: Int): ToolbarMenuCheckableOverflowItem? {
    val overflowMenuItems = rightToolbarMenu?.overflowMenuItems
      ?: return null

    val currentOverflowMenuItems = mutableListOf<ToolbarMenuCheckableOverflowItem>()
    currentOverflowMenuItems += overflowMenuItems
      .filterIsInstance<ToolbarMenuCheckableOverflowItem>()

    for (overflowMenuItem in currentOverflowMenuItems) {
      if (overflowMenuItem.id == id) {
        return overflowMenuItem
      }

      overflowMenuItems += overflowMenuItem.subItems
        .filterIsInstance<ToolbarMenuCheckableOverflowItem>()
    }

    return null
  }

  fun checkOrUncheckItem(subItem: ToolbarMenuCheckableOverflowItem, check: Boolean) {
    val overflowMenuItems = rightToolbarMenu?.overflowMenuItems
      ?: return

    val currentOverflowMenuItems = mutableListOf<ToolbarMenuCheckableOverflowItem>()
    currentOverflowMenuItems += overflowMenuItems
      .filterIsInstance<ToolbarMenuCheckableOverflowItem>()

    val groupId = subItem.groupId

    for (overflowMenuItem in currentOverflowMenuItems) {
      if (overflowMenuItem.id == subItem.id) {
        overflowMenuItem.updateChecked(check)
      } else if (groupId != null && overflowMenuItem.groupId == groupId) {
        overflowMenuItem.updateChecked(false)
      }

      overflowMenuItems += overflowMenuItem.subItems
        .filterIsInstance<ToolbarMenuCheckableOverflowItem>()
    }
  }

}

enum class ToolbarStateKind {
  Default,
  Search,
  Selection,
  Reply;

  fun isSearch(): Boolean = this == Search
}