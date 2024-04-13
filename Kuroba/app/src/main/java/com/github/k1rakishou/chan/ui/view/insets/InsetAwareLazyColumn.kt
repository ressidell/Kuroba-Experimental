package com.github.k1rakishou.chan.ui.view.insets

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.chan.ui.compose.providers.LocalWindowInsets
import com.github.k1rakishou.chan.utils.appDependencies

@Composable
fun InsetAwareLazyColumn(
  modifier: Modifier = Modifier,
  state: LazyListState = rememberLazyListState(),
  contentPadding: PaddingValues = PaddingValues(0.dp),
  reverseLayout: Boolean = false,
  verticalArrangement: Arrangement.Vertical =
    if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
  horizontalAlignment: Alignment.Horizontal = Alignment.Start,
  flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
  userScrollEnabled: Boolean = true,
  content: LazyListScope.() -> Unit
) {
  val windowInsets = LocalWindowInsets.current
  val layoutDirection = LocalLayoutDirection.current

  val globalUiStateHolder = appDependencies().globalUiStateHolder
  val toolbarHeight by globalUiStateHolder.toolbar.toolbarHeight.collectAsState()
  val bottomPanelHeight by globalUiStateHolder.bottomPanel.bottomPanelHeight.collectAsState()

  val finalContentPadding = remember(contentPadding, bottomPanelHeight, toolbarHeight, windowInsets, layoutDirection) {
    val topPadding = maxOf(
      windowInsets.top,
      toolbarHeight
    )

    val bottomPadding = maxOf(
      windowInsets.bottom,
      bottomPanelHeight
    )

    return@remember PaddingValues(
      top = contentPadding.calculateTopPadding() + topPadding,
      bottom = contentPadding.calculateBottomPadding() + bottomPadding,
      start = contentPadding.calculateStartPadding(layoutDirection),
      end = contentPadding.calculateEndPadding(layoutDirection)
    )
  }

  LazyColumn(
    modifier = modifier,
    state = state,
    contentPadding = finalContentPadding,
    reverseLayout = reverseLayout,
    verticalArrangement = verticalArrangement,
    horizontalAlignment = horizontalAlignment,
    flingBehavior = flingBehavior,
    userScrollEnabled = userScrollEnabled,
    content = content
  )
}