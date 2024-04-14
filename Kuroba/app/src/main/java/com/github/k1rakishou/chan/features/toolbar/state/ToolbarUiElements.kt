package com.github.k1rakishou.chan.features.toolbar.state

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.ContentAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.chan.features.toolbar.ToolbarMenuItem
import com.github.k1rakishou.chan.features.toolbar.ToolbarText
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeText
import com.github.k1rakishou.chan.ui.compose.components.kurobaClickable
import com.github.k1rakishou.chan.ui.compose.ktu
import com.github.k1rakishou.chan.ui.helper.PinHelper
import com.github.k1rakishou.core_themes.ChanTheme
import com.github.k1rakishou.core_themes.ThemeEngine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlin.coroutines.cancellation.CancellationException

@Composable
internal fun ToolbarTitleWithSubtitle(
  modifier: Modifier,
  title: ToolbarText,
  subtitle: ToolbarText?,
  chanTheme: ChanTheme,
  scrollableTitle: Boolean
) {
  val textColor = chanTheme.onToolbarBackgroundComposeColor

  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.Center
  ) {
    val marqueeModifier = if (scrollableTitle) {
      Modifier.basicMarquee(
        iterations = Int.MAX_VALUE,
        delayMillis = 3_000
      )
    } else {
      Modifier
    }

    KurobaComposeText(
      modifier = Modifier
        .wrapContentWidth()
        .wrapContentHeight()
        .then(marqueeModifier),
      text = title.resolve(),
      fontSize = 18.ktu,
      color = textColor,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = if (scrollableTitle) {
        TextOverflow.Clip
      } else {
        TextOverflow.Ellipsis
      }
    )

    if (subtitle != null) {
      Spacer(modifier = Modifier.height(2.dp))

      var inlineContent by remember { mutableStateOf<Map<String, InlineTextContent>>(emptyMap()) }

      val subtitleText = subtitle.resolve()

      ResolveInlinedContent(
        fontSize = 14.ktu,
        subtitleText = subtitleText,
        onInlineContentReady = { newInlineContent -> inlineContent = newInlineContent }
      )

      KurobaComposeText(
        modifier = Modifier
          .wrapContentWidth()
          .wrapContentHeight()
          .then(marqueeModifier),
        text = subtitleText,
        fontSize = 14.ktu,
        color = textColor,
        fontWeight = FontWeight.Light,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        inlineContent = inlineContent
      )
    }
  }
}

@Composable
internal fun ToolbarClickableIcon(
  toolbarMenuItem: ToolbarMenuItem,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  chanTheme: ChanTheme,
  onClick: () -> Unit
) {
  val visible by toolbarMenuItem.visibleState
  if (!visible) {
    return
  }

  val drawableId by toolbarMenuItem.drawableIdState

  val alpha = if (enabled) DefaultAlpha else ContentAlpha.disabled

  val rotationAnimatable = remember { Animatable(initialValue = 0f) }

  LaunchedEffect(key1 = toolbarMenuItem) {
    toolbarMenuItem.spinEventsFlow
      .onEach {
        try {
          try {
            rotationAnimatable.animateTo(1f, animationSpec = tween(durationMillis = 750))
          } finally {
            rotationAnimatable.snapTo(0f)
          }
        } catch (_: CancellationException) {

        }
      }
      .collect()
  }

  val clickModifier = if (enabled) {
    Modifier.kurobaClickable(
      bounded = false,
      onClick = { onClick() }
    )
  } else {
    Modifier
  }

  Image(
    modifier = clickModifier
      .then(modifier)
      .graphicsLayer {
        rotationZ = rotationAnimatable.value * 360f
      },
    painter = painterResource(id = drawableId),
    colorFilter = ColorFilter.tint(chanTheme.onToolbarBackgroundComposeColor),
    alpha = alpha,
    contentDescription = null
  )
}

@Composable
internal fun BoxScope.ToolbarBadgeContent(
  chanTheme: ChanTheme,
  toolbarBadge: ToolbarBadge
) {
  val counter by animateIntAsState(
    targetValue = toolbarBadge.counter,
    label = "Menu item badge counter animation"
  )
  val highlight = toolbarBadge.highlight

  val backgroundColor = if (highlight) {
    chanTheme.accentColorCompose
  } else {
    if (ThemeEngine.isDarkColor(chanTheme.primaryColor)) {
      Color.LightGray
    } else {
      Color.DarkGray
    }
  }

  val textColor = if (ThemeEngine.isDarkColor(backgroundColor)) {
    Color.White
  } else {
    Color.Black
  }

  if (counter > 0) {
    val counterText = remember(key1 = counter) { PinHelper.getShortUnreadCount(counter) }

    KurobaComposeText(
      modifier = Modifier
        .align(Alignment.Center)
        .offset(x = 10.dp, y = (-10).dp)
        .drawWithContent {
          drawRoundRect(
            color = backgroundColor,
            alpha = 0.85f,
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
          )

          drawContent()
        }
        .padding(horizontal = 2.dp),
      text = counterText,
      color = textColor,
      fontSize = 10.ktu.fixedSize()
    )
  }
}