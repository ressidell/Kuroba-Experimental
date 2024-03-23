package com.github.k1rakishou.chan.features.toolbar_v2.state

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarText
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeText
import com.github.k1rakishou.chan.ui.compose.ktu
import com.github.k1rakishou.chan.ui.compose.providers.LocalChanTheme

@Composable
internal fun ToolbarTitleWithSubtitle(
  modifier: Modifier,
  title: ToolbarText,
  subtitle: ToolbarText?,
  scrollableTitle: Boolean
) {
  val chanTheme = LocalChanTheme.current
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

      KurobaComposeText(
        modifier = Modifier
          .wrapContentWidth()
          .wrapContentHeight(),
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