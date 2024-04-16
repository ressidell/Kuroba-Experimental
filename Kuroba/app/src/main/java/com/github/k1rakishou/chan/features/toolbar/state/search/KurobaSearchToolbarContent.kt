package com.github.k1rakishou.chan.features.toolbar.state.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.textAsFlow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.chan.ui.compose.clearText
import com.github.k1rakishou.chan.ui.compose.components.IconTint
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeClickableIcon
import com.github.k1rakishou.chan.ui.compose.components.KurobaSearchInput
import com.github.k1rakishou.chan.ui.compose.freeFocusSafe
import com.github.k1rakishou.chan.ui.compose.requestFocusSafe
import com.github.k1rakishou.core_themes.ChanTheme
import com.github.k1rakishou.core_themes.ThemeEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun KurobaSearchToolbarContent(
  modifier: Modifier,
  chanTheme: ChanTheme,
  state: KurobaSearchToolbarSubState,
  onCloseSearchToolbarButtonClicked: () -> Unit
) {
  val focusRequester = remember { FocusRequester() }
  val coroutineScope = rememberCoroutineScope()

  val searchVisibleState by state.searchVisibleState
  if (!searchVisibleState) {
    return
  }

  val searchQueryState = state.searchQueryState

  val kurobaSearchInputColor = if (ThemeEngine.isDarkColor(chanTheme.toolbarBackgroundComposeColor)) {
    Color.White
  } else {
    Color.Black
  }

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
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Spacer(modifier = Modifier.width(12.dp))

    SearchIcon(
      searchQueryState = searchQueryState,
      onCloseSearchToolbarButtonClicked = onCloseSearchToolbarButtonClicked
    )

    Spacer(modifier = Modifier.width(12.dp))

    Box(
      modifier = Modifier
        .weight(1f)
        .wrapContentHeight()
        .padding(horizontal = 8.dp)
    ) {
      KurobaSearchInput(
        modifier = Modifier
          .fillMaxSize()
          .focusable()
          .focusRequester(focusRequester),
        displayClearButton = false,
        color = kurobaSearchInputColor,
        searchQueryState = searchQueryState,
      )
    }

    Spacer(modifier = Modifier.width(12.dp))
  }
}

@Composable
private fun SearchIcon(
  searchQueryState: TextFieldState,
  onCloseSearchToolbarButtonClicked: () -> Unit
) {
  val searchQuery by searchQueryState.textAsFlow().collectAsState(initial = "")

  AnimatedContent(targetState = searchQuery.isEmpty()) { searchQueryEmpty ->
    if (searchQueryEmpty) {
      KurobaComposeClickableIcon(
        drawableId = com.github.k1rakishou.chan.R.drawable.ic_arrow_back_white_24dp,
        iconTint = IconTint.Tint,
        onClick = onCloseSearchToolbarButtonClicked
      )
    } else {
      KurobaComposeClickableIcon(
        drawableId = com.github.k1rakishou.chan.R.drawable.ic_baseline_clear_24,
        iconTint = IconTint.Tint,
        onClick = { searchQueryState.edit { clearText() } }
      )
    }
  }
}
