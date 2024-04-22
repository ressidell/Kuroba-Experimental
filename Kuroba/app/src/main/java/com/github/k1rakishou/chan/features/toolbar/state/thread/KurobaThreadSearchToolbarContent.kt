package com.github.k1rakishou.chan.features.toolbar.state.thread

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.chan.features.toolbar.state.SearchIcon
import com.github.k1rakishou.chan.features.toolbar.state.SearchToolbarInfoText
import com.github.k1rakishou.chan.ui.compose.components.KurobaSearchInput
import com.github.k1rakishou.core_themes.ChanTheme
import com.github.k1rakishou.core_themes.ThemeEngine

@Composable
fun KurobaThreadSearchToolbarContent(
  modifier: Modifier,
  chanTheme: ChanTheme,
  state: KurobaThreadSearchToolbarSubState,
  focusRequester: FocusRequester,
  onCloseSearchToolbarButtonClicked: () -> Unit
) {
  val searchVisibleState by state.searchVisibleState
  if (!searchVisibleState) {
    return
  }

  val searchQueryState = state.searchQueryState

  val currentSearchItemIndex by state.currentSearchItemIndex
  val totalFoundItems by state.totalFoundItems

  val color = ThemeEngine.resolveTextColor(chanTheme.toolbarBackgroundComposeColor)

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
        color = color,
        searchQueryState = searchQueryState,
      )
    }

    Spacer(modifier = Modifier.width(12.dp))

    SearchToolbarInfoText(
      totalFoundItems = totalFoundItems,
      currentSearchItemIndex = currentSearchItemIndex,
      onShowFoundItemsAsPopupClicked = { state.onShowFoundItemsAsPopupClicked() }
    )

    Spacer(modifier = Modifier.width(12.dp))
  }
}
