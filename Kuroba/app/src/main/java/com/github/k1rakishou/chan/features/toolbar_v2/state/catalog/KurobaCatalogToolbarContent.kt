package com.github.k1rakishou.chan.features.toolbar_v2.state.catalog

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.chan.features.toolbar_v2.MoreVerticalMenuItem
import com.github.k1rakishou.chan.features.toolbar_v2.state.ToolbarClickableIcon
import com.github.k1rakishou.chan.features.toolbar_v2.state.ToolbarTitleWithSubtitle
import com.github.k1rakishou.chan.ui.compose.components.kurobaClickable
import com.github.k1rakishou.chan.ui.compose.providers.LocalChanTheme

@Composable
fun KurobaCatalogToolbarContent(
  modifier: Modifier,
  state: KurobaCatalogToolbarState
) {
  val leftIconMut by state.leftItem
  val leftIcon = leftIconMut

  val toolbarMenuMut by state.toolbarMenu
  val toolbarMenu = toolbarMenuMut

  val titleMut by state.title
  val title = titleMut

  val subtitle by state.subtitle

  val chanTheme = LocalChanTheme.current
  val textColor = chanTheme.onToolbarBackgroundComposeColor

  val path = remember { Path() }

  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (leftIcon != null) {
      Spacer(modifier = Modifier.width(12.dp))

      val iconDrawableId by leftIcon.drawableIdState
      ToolbarClickableIcon(
        drawableId = iconDrawableId,
        onClick = {
          val iconClickInterceptor = state.iconClickInterceptor

          if (iconClickInterceptor == null || iconClickInterceptor(leftIcon)) {
            leftIcon.onClick(leftIcon)
          }
        }
      )
    }

    if (title != null) {
      ToolbarTitleWithSubtitle(
        modifier = Modifier
          .weight(1f)
          .kurobaClickable(
            bounded = true,
            enabled = true,
            onClick = { state.onMainContentClick?.invoke() }
          )
          .drawBehind { drawTriangle(path, textColor) }
          .padding(start = 12.dp, end = 28.dp),
        title = title,
        subtitle = subtitle,
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

          ToolbarClickableIcon(
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

        ToolbarClickableIcon(
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

private fun DrawScope.drawTriangle(
  path: Path,
  textColor: Color
) {
  val width = with(density) { 10.dp.toPx() }
  val height = with(density) { 6.dp.toPx() }

  val leftOffset = with(density) { 8.dp.toPx() }
  val topOffset = (size.height - height) / 2f

  translate(left = -leftOffset, top = -topOffset) {
    rotate(degrees = 180f) {
      path.rewind()

      with(path) {
        moveTo(x = width / 2, y = 0f)
        lineTo(x = width, y = height)
        lineTo(x = 0f, y = height)
        close()
      }

      drawPath(path = path, color = textColor)
    }
  }
}
