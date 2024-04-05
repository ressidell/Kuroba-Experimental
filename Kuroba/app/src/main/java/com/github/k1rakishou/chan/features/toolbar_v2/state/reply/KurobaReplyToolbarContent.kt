package com.github.k1rakishou.chan.features.toolbar_v2.state.reply

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
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.features.toolbar_v2.AbstractToolbarMenuOverflowItem
import com.github.k1rakishou.chan.features.toolbar_v2.MoreVerticalMenuItem
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarText
import com.github.k1rakishou.chan.features.toolbar_v2.state.ToolbarClickableIcon
import com.github.k1rakishou.chan.features.toolbar_v2.state.ToolbarTitleWithSubtitle
import com.github.k1rakishou.model.data.descriptor.ChanDescriptor

@Composable
fun KurobaReplyToolbarContent(
  modifier: Modifier,
  state: KurobaReplyToolbarSubState,
  showFloatingMenu: (List<AbstractToolbarMenuOverflowItem>) -> Unit
) {
  val chanDescriptorMut by state.chanDescriptor
  val chanDescriptor = chanDescriptorMut

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

      ToolbarClickableIcon(
        toolbarMenuItem = leftIcon,
        onClick = { leftIcon.onClick(leftIcon) }
      )
    }

    if (chanDescriptor != null) {
      val title = remember(key1 = chanDescriptor) {
        when (chanDescriptor) {
          is ChanDescriptor.ICatalogDescriptor -> ToolbarText.Id(R.string.reply_make_new_thread_hint)
          is ChanDescriptor.ThreadDescriptor -> ToolbarText.Id(R.string.reply_reply_in_thread_hint)
        }
      }

      ToolbarTitleWithSubtitle(
        modifier = Modifier
          .weight(1f)
          .padding(start = 12.dp),
        title = title,
        subtitle = null,
        scrollableTitle = false
      )
    } else {
      Spacer(modifier = Modifier.weight(1f))
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
            onClick = { rightIcon.onClick(rightIcon) }
          )
        }
      }

      val overflowMenuItems = toolbarMenu.overflowMenuItems
      if (overflowMenuItems.isNotEmpty()) {
        val overflowIcon = remember { MoreVerticalMenuItem(onClick = { }) }

        Spacer(modifier = Modifier.width(12.dp))

        ToolbarClickableIcon(
          toolbarMenuItem = overflowIcon,
          onClick = { showFloatingMenu(overflowMenuItems) }
        )
      }

      Spacer(modifier = Modifier.width(12.dp))
    }
  }
}