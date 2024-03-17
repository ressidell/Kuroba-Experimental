package com.github.k1rakishou.chan.features.toolbar_v2

import androidx.annotation.DrawableRes
import androidx.compose.runtime.toMutableStateList
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils.getString
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList


@DslMarker
annotation class ToolbarDsl

@ToolbarDsl
class ToolbarMenuBuilder {
  private val items = mutableListOf<ToolbarMenuItem>()
  private val overflowItems = mutableListOf<AbstractToolbarMenuOverflowItem>()

  fun withMenuItem(
    id: Int? = null,
    @DrawableRes drawableId: Int,
    onClick: (ToolbarMenuItem) -> Unit
  ): ToolbarMenuBuilder {
    val toolbarMenuItem = ToolbarMenuItem(
      id = id,
      drawableId = drawableId,
      onClick = onClick
    )

    if (id != null) {
      val existingToolbarIcon = items.firstOrNull { toolbarIcon -> toolbarIcon.id == id }
      if (existingToolbarIcon != null) {
        error("ToolbarIcon with id: ${id} already registered! existingToolbarIcon: ${existingToolbarIcon}")
      }
    }

    items += toolbarMenuItem
    return this
  }

  fun withOverflowMenu(
    builder: ToolbarOverflowMenuBuilder.() -> Unit,
  ): ToolbarMenuBuilder {
    val toolbarOverflowMenuBuilder = ToolbarOverflowMenuBuilder()
    with(toolbarOverflowMenuBuilder) { builder() }
    overflowItems += toolbarOverflowMenuBuilder.build()

    return this
  }

  fun build(): ToolbarMenu {
    return ToolbarMenu(
      menuItems = items.toMutableStateList(),
      overflowMenuItems = overflowItems.toMutableStateList()
    )
  }

}

@ToolbarDsl
class ToolbarOverflowMenuBuilder {
  private val overflowItems = mutableListOf<AbstractToolbarMenuOverflowItem>()

  fun withOverflowMenuItem(
    id: Int,
    stringId: Int,
    visible: Boolean = true,
    groupId: String? = null,
    value: Any? = null,
    onClick: ((ToolbarMenuOverflowItem) -> Unit)? = null,
    builder: ToolbarOverflowMenuBuilder.() -> Unit = { }
  ): ToolbarOverflowMenuBuilder {
    val toolbarOverflowMenuBuilder = ToolbarOverflowMenuBuilder()
    with(toolbarOverflowMenuBuilder) { builder() }
    val subItems = toolbarOverflowMenuBuilder.build()

    overflowItems += ToolbarMenuOverflowItem(
      id = id,
      // TODO: New toolbar
      text = getString(stringId),
      visible = visible,
      groupId = groupId,
      value = value,
      subItems = subItems,
      onClick = onClick
    )

    return this
  }

  fun withCheckableOverflowMenuItem(
    id: Int,
    stringId: Int,
    visible: Boolean = true,
    checked: Boolean = false,
    value: Any? = null,
    onClick: (ToolbarMenuCheckableOverflowItem) -> Unit,
    builder: ToolbarOverflowMenuBuilder.() -> Unit = { }
  ): ToolbarOverflowMenuBuilder {
    return withCheckableOverflowMenuItem(
      id = id,
      text = getString(stringId),
      visible = visible,
      checked = checked,
      value = value,
      onClick = onClick,
      builder = builder
    )
  }

  fun withCheckableOverflowMenuItem(
    id: Int,
    text: String,
    visible: Boolean = true,
    checked: Boolean = false,
    groupId: String? = null,
    value: Any? = null,
    onClick: (ToolbarMenuCheckableOverflowItem) -> Unit,
    builder: ToolbarOverflowMenuBuilder.() -> Unit = { }
  ): ToolbarOverflowMenuBuilder {
    val toolbarOverflowMenuBuilder = ToolbarOverflowMenuBuilder()
    with(toolbarOverflowMenuBuilder) { builder() }
    val subItems = toolbarOverflowMenuBuilder.build()

    overflowItems += ToolbarMenuCheckableOverflowItem(
      id = id,
      // TODO: New toolbar
      text = text,
      visible = visible,
      checked = checked,
      groupId = groupId,
      value = value,
      subItems = subItems,
      onClick = onClick
    )

    return this
  }

  fun build(): ImmutableList<AbstractToolbarMenuOverflowItem> {
    return overflowItems.toImmutableList()
  }

}