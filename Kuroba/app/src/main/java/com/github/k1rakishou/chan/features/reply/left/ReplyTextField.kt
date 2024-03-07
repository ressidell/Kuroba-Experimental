package com.github.k1rakishou.chan.features.reply.left

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material.ContentAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.k1rakishou.chan.features.reply.ReplyLayoutViewModel
import com.github.k1rakishou.chan.features.reply.data.ReplyLayoutState
import com.github.k1rakishou.chan.features.reply.data.ReplyLayoutVisibility
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeTextFieldV2
import com.github.k1rakishou.chan.ui.compose.components.KurobaLabelText
import com.github.k1rakishou.chan.ui.compose.freeFocusSafe
import com.github.k1rakishou.chan.ui.compose.providers.LocalChanTheme
import com.github.k1rakishou.chan.ui.compose.requestFocusSafe

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ReplyTextField(
  replyLayoutState: ReplyLayoutState,
  replyLayoutViewModel: ReplyLayoutViewModel,
  replyLayoutEnabled: Boolean
) {
  val chanTheme = LocalChanTheme.current
  val localSoftwareKeyboardController = LocalSoftwareKeyboardController.current
  val focusRequester = remember { FocusRequester() }

  var prevReplyLayoutVisibility by remember { mutableStateOf<ReplyLayoutVisibility>(ReplyLayoutVisibility.Collapsed) }
  val replyTextState = replyLayoutState.replyTextState
  val replyLayoutVisibility by replyLayoutState.replyLayoutVisibility
  val replyFieldHintText by replyLayoutState.replyFieldHintText

  val disabledAlpha = ContentAlpha.disabled

  val replyInputVisualTransformation = remember(key1 = chanTheme, key2 = replyLayoutEnabled, key3 = disabledAlpha) {
    return@remember VisualTransformation { text ->
      val spannedText = ReplyTextFieldHelpers.colorizeReplyInputText(
        disabledAlpha = disabledAlpha,
        text = text,
        replyLayoutEnabled = replyLayoutEnabled,
        chanTheme = chanTheme
      )

      return@VisualTransformation TransformedText(spannedText, OffsetMapping.Identity)
    }
  }

  LaunchedEffect(
    key1 = replyLayoutVisibility,
    key2 = prevReplyLayoutVisibility,
    block = {
      if (prevReplyLayoutVisibility == replyLayoutVisibility) {
        return@LaunchedEffect
      }

      fun isOpenedNow(): Boolean {
        return prevReplyLayoutVisibility == ReplyLayoutVisibility.Collapsed &&
          replyLayoutVisibility == ReplyLayoutVisibility.Opened
      }

      fun isCollapsedNow(): Boolean {
        return prevReplyLayoutVisibility != ReplyLayoutVisibility.Collapsed &&
          replyLayoutVisibility == ReplyLayoutVisibility.Collapsed
      }

      if (isOpenedNow()) {
        focusRequester.requestFocusSafe()
        localSoftwareKeyboardController?.show()
      } else if (isCollapsedNow() && !replyLayoutViewModel.isAnyReplyLayoutOpened()) {
        focusRequester.freeFocusSafe()
        localSoftwareKeyboardController?.hide()
      }

      prevReplyLayoutVisibility = replyLayoutVisibility
    }
  )

  DisposableEffect(
    key1 = Unit,
    effect = {
      onDispose {
        focusRequester.freeFocusSafe()

        if (!replyLayoutViewModel.isAnyReplyLayoutOpened()) {
          localSoftwareKeyboardController?.hide()
        }
      }
    }
  )

  val minHeightModifier = if (replyLayoutVisibility == ReplyLayoutVisibility.Expanded) {
    Modifier.heightIn(min = 200.dp)
  } else {
    Modifier
  }

  val mutableInteractionSource = remember { MutableInteractionSource() }

  KurobaComposeTextFieldV2(
    modifier = Modifier
      .fillMaxSize()
      .padding(vertical = 4.dp)
      .focusable(interactionSource = mutableInteractionSource)
      .focusRequester(focusRequester)
      .then(minHeightModifier),
    enabled = replyLayoutEnabled,
    state = replyTextState,
    lineLimits = TextFieldLineLimits.MultiLine(),
    // TODO: New reply layout
//    visualTransformation = replyInputVisualTransformation,
    keyboardOptions = KeyboardOptions(
      capitalization = KeyboardCapitalization.Sentences
    ),
    label = {
      KurobaLabelText(
        enabled = replyLayoutEnabled,
        labelText = replyFieldHintText,
        fontSize = 12.sp,
        color = chanTheme.textColorHintCompose
      )
    },
    interactionSource = mutableInteractionSource
  )
}