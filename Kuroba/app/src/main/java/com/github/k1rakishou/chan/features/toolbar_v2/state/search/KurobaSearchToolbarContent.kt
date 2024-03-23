package com.github.k1rakishou.chan.features.toolbar_v2.state.search

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.features.toolbar_v2.state.ToolbarClickableIcon
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeTextFieldV2
import com.github.k1rakishou.chan.ui.compose.components.KurobaLabelText
import com.github.k1rakishou.chan.ui.compose.freeFocusSafe
import com.github.k1rakishou.chan.ui.compose.ktu
import com.github.k1rakishou.chan.ui.compose.providers.LocalChanTheme
import com.github.k1rakishou.chan.ui.compose.requestFocusSafe
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun KurobaSearchToolbarContent(
  modifier: Modifier,
  state: KurobaSearchToolbarState
) {
  val chanTheme = LocalChanTheme.current
  val focusRequester = remember { FocusRequester() }
  val coroutineScope = rememberCoroutineScope()

  val searchQueryState = state.searchQueryState

  DisposableEffect(
    key1 = Unit,
    effect = {
      val job = coroutineScope.launch {
        delay(100)
        focusRequester.requestFocusSafe()
      }

      onDispose {
        job.cancel()
        focusRequester.freeFocusSafe()
      }
    }
  )

  Row(
    modifier = modifier
  ) {
    KurobaComposeTextFieldV2(
      modifier = Modifier
        .weight(1f)
        .wrapContentHeight()
        .padding(vertical = 4.dp, horizontal = 8.dp)
        .focusable()
        .focusRequester(focusRequester),
      state = searchQueryState,
      label = { interactionSource ->
        KurobaLabelText(
          enabled = true,
          labelText = stringResource(id = R.string.type_to_search_hint),
          fontSize = 12.ktu,
          interactionSource = interactionSource
        )
      }
    )

    Spacer(modifier = Modifier.width(8.dp))

    ToolbarClickableIcon(
      drawableId = R.drawable.ic_baseline_clear_24,
      onClick = { state.clearSearchQuery() }
    )
  }
}