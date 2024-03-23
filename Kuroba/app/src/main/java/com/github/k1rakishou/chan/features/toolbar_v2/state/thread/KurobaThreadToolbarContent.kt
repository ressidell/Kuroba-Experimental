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
import com.github.k1rakishou.chan.features.toolbar_v2.MoreVerticalMenuItem
import com.github.k1rakishou.chan.features.toolbar_v2.state.ToolbarTitleWithSubtitle
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeClickableIcon

@Composable
fun KurobaThreadToolbarContent(
  modifier: Modifier,
  state: KurobaThreadToolbarState
) {
  val leftIconMut by state.leftItem
  val leftIcon = leftIconMut

  val toolbarMenuMut by state.toolbarMenu
  val toolbarMenu = toolbarMenuMut

  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (leftIcon != null) {
      Spacer(modifier = Modifier.width(12.dp))

      val iconDrawableId by leftIcon.drawableIdState
      KurobaComposeClickableIcon(
        drawableId = iconDrawableId,
        onClick = {
          val iconClickInterceptor = state.iconClickInterceptor

          if (iconClickInterceptor == null || iconClickInterceptor(leftIcon)) {
            leftIcon.onClick(leftIcon)
          }
        }
      )
    }

    val titleMut by state.title
    val title = titleMut

    if (title != null) {
      ToolbarTitleWithSubtitle(
        modifier = Modifier
          .weight(1f)
          .padding(start = 12.dp),
        title = title,
        subtitle = null,
        scrollableTitle = false
      )
    }

    if (toolbarMenu != null) {
      val menuItems = toolbarMenu.menuItems
      if (menuItems.isNotEmpty()) {
        Spacer(modifier = Modifier.width(8.dp))

        for (rightIcon in menuItems) {
          val iconDrawableId by rightIcon.drawableIdState
          val visible by rightIcon.visibleState

          if (!visible) {
            continue
          }

          Spacer(modifier = Modifier.width(12.dp))

          KurobaComposeClickableIcon(
            drawableId = iconDrawableId,
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
        val drawableId by overflowIcon.drawableIdState

        Spacer(modifier = Modifier.width(12.dp))

        KurobaComposeClickableIcon(
          drawableId = drawableId,
          onClick = {
            val iconClickInterceptor = state.iconClickInterceptor

            if (iconClickInterceptor == null || iconClickInterceptor(overflowIcon)) {
              // TODO: New toolbar. Show overflow menu.
            }
          }
        )
      }

      Spacer(modifier = Modifier.width(12.dp))
    }
  }
}