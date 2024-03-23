package com.github.k1rakishou.chan.features.toolbar_v2.state.default

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.chan.features.toolbar_v2.MoreVerticalMenuItem
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMiddleContent
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarText
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeClickableIcon
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeText
import com.github.k1rakishou.chan.ui.compose.ktu
import com.github.k1rakishou.chan.ui.compose.providers.LocalChanTheme

@Composable
fun KurobaDefaultToolbarContent(
  modifier: Modifier,
  state: KurobaDefaultToolbarState
) {
  val leftIconMut by state.leftItem
  val leftIcon = leftIconMut

  val middleContentMut by state.middleContent
  val middleContent = middleContentMut

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

    when (middleContent) {
      is ToolbarMiddleContent.Title -> {
        if (middleContent.title != null) {
          Spacer(modifier = Modifier.width(12.dp))

          ToolbarTitleWithSubtitle(
            modifier = Modifier.weight(1f),
            title = middleContent.title,
            subtitle = middleContent.subtitle
          )
        }
      }
      null -> {
        // no-op
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    if (toolbarMenu != null) {
      val menuItems = toolbarMenu.menuItems
      if (menuItems.isNotEmpty()) {
        Spacer(modifier = Modifier.width(12.dp))

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
        val overflowIcon = remember {
          MoreVerticalMenuItem(onClick = { /**no-op*/ })
        }

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

@Composable
private fun ToolbarTitleWithSubtitle(modifier: Modifier, title: ToolbarText, subtitle: ToolbarText?) {
  val chanTheme = LocalChanTheme.current
  val textColor = chanTheme.textColorBasedOnToolbarBackgroundColor

  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.Center
  ) {
    KurobaComposeText(
      text = title.resolve(),
      fontSize = 18.ktu,
      color = textColor,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )

    if (subtitle != null) {
      Spacer(modifier = Modifier.height(2.dp))

      KurobaComposeText(
        modifier = Modifier.wrapContentHeight(),
        text = subtitle.resolve(),
        fontSize = 14.ktu,
        color = textColor,
        fontWeight = FontWeight.Light,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}