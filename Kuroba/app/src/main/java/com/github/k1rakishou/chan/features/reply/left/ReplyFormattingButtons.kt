package com.github.k1rakishou.chan.features.reply.left

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.github.k1rakishou.chan.features.reply.data.ReplyLayoutState
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeTextBarButton

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReplyFormattingButtons(replyLayoutState: ReplyLayoutState) {
  val postFormatterButtons by replyLayoutState.postFormatterButtons
  if (postFormatterButtons.isEmpty()) {
    return
  }

  FlowRow(
    modifier = Modifier
      .fillMaxWidth()
      .wrapContentHeight()
  ) {
    postFormatterButtons.forEach { postFormatterButton ->
      key(postFormatterButton.openTag) {
        KurobaComposeTextBarButton(
          modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth(),
          text = postFormatterButton.title,
          fontSize = 14.sp,
          onClick = { replyLayoutState.insertTags(postFormatterButton) }
        )
      }
    }
  }
}