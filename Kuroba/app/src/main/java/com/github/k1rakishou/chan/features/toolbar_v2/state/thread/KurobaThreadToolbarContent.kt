package com.github.k1rakishou.chan.features.toolbar_v2.state.thread

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.chan.features.toolbar_v2.AbstractToolbarMenuOverflowItem
import com.github.k1rakishou.chan.features.toolbar_v2.MoreVerticalMenuItem
import com.github.k1rakishou.chan.features.toolbar_v2.state.ToolbarClickableIcon
import com.github.k1rakishou.chan.features.toolbar_v2.state.ToolbarTitleWithSubtitle

@Composable
fun KurobaThreadToolbarContent(
  modifier: Modifier,
  state: KurobaThreadToolbarState,
  showFloatingMenu: (List<AbstractToolbarMenuOverflowItem>) -> Unit
) {
  val leftIconMut by state.leftItem
  val leftIcon = leftIconMut

  val toolbarMenuMut by state.toolbarMenu
  val toolbarMenu = toolbarMenuMut

  val titleMut by state.title
  val title = titleMut

  val scrollableTitle by state.scrollableTitle

  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (leftIcon != null) {
      Spacer(modifier = Modifier.width(12.dp))

      ToolbarClickableIcon(
        toolbarMenuItem = leftIcon,
        onClick = {
          val iconClickInterceptor = state.iconClickInterceptor

          if (iconClickInterceptor == null || !iconClickInterceptor(leftIcon)) {
            leftIcon.onClick(leftIcon)
          }
        }
      )
    }

    if (title != null) {
      ToolbarTitleWithSubtitle(
        modifier = Modifier
          .weight(1f)
          .padding(start = 12.dp),
        title = title,
        subtitle = null,
        scrollableTitle = scrollableTitle
      )
    }

    if (toolbarMenu != null) {
      val menuItems = toolbarMenu.menuItems
      if (menuItems.isNotEmpty()) {
        Spacer(modifier = Modifier.width(8.dp))

        for (rightIcon in menuItems) {
          val visible by rightIcon.visibleState
          if (!visible) {
            continue
          }

          Spacer(modifier = Modifier.width(12.dp))

          ToolbarClickableIcon(
            toolbarMenuItem = rightIcon,
            onClick = {
              val iconClickInterceptor = state.iconClickInterceptor

              if (iconClickInterceptor == null || !iconClickInterceptor(rightIcon)) {
                rightIcon.onClick(rightIcon)
              }
            }
          )
        }
      }

      val overflowMenuItems = toolbarMenu.overflowMenuItems
      if (overflowMenuItems.isNotEmpty()) {
        val overflowIcon = remember { MoreVerticalMenuItem(onClick = { }) }

        Spacer(modifier = Modifier.width(12.dp))

        ToolbarClickableIcon(
          toolbarMenuItem = overflowIcon,
          onClick = {
            val iconClickInterceptor = state.iconClickInterceptor

            if (iconClickInterceptor == null || !iconClickInterceptor(overflowIcon)) {
              showFloatingMenu(overflowMenuItems)
            }
          }
        )
      }

      Spacer(modifier = Modifier.width(12.dp))
    }
  }
}