package com.github.k1rakishou.chan.features.toolbar_v2.state

import com.github.k1rakishou.chan.features.toolbar_v2.AbstractToolbarMenuOverflowItem
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

}

enum class ToolbarStateKind {
  Container,
  Catalog,
  Thread,
  Default,
  Search,
  Selection,
  Reply;

  fun isSearch(): Boolean = this == Search
}