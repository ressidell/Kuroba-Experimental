package com.github.k1rakishou.chan.features.toolbar

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.github.k1rakishou.chan.features.toolbar.state.catalog.KurobaCatalogSearchToolbarContent
import com.github.k1rakishou.chan.features.toolbar.state.catalog.KurobaCatalogSearchToolbarSubState
import com.github.k1rakishou.chan.features.toolbar.state.catalog.KurobaCatalogToolbarContent
import com.github.k1rakishou.chan.features.toolbar.state.catalog.KurobaCatalogToolbarSubState
import com.github.k1rakishou.chan.features.toolbar.state.container.KurobaContainerToolbarContent
import com.github.k1rakishou.chan.features.toolbar.state.container.KurobaContainerToolbarSubState
import com.github.k1rakishou.chan.features.toolbar.state.default.KurobaDefaultToolbarContent
import com.github.k1rakishou.chan.features.toolbar.state.default.KurobaDefaultToolbarSubState
import com.github.k1rakishou.chan.features.toolbar.state.reply.KurobaReplyToolbarContent
import com.github.k1rakishou.chan.features.toolbar.state.reply.KurobaReplyToolbarSubState
import com.github.k1rakishou.chan.features.toolbar.state.search.KurobaSearchToolbarContent
import com.github.k1rakishou.chan.features.toolbar.state.search.KurobaSearchToolbarSubState
import com.github.k1rakishou.chan.features.toolbar.state.selection.KurobaSelectionToolbarContent
import com.github.k1rakishou.chan.features.toolbar.state.selection.KurobaSelectionToolbarSubState
import com.github.k1rakishou.chan.features.toolbar.state.thread.KurobaThreadSearchToolbarContent
import com.github.k1rakishou.chan.features.toolbar.state.thread.KurobaThreadSearchToolbarSubState
import com.github.k1rakishou.chan.features.toolbar.state.thread.KurobaThreadToolbarContent
import com.github.k1rakishou.chan.features.toolbar.state.thread.KurobaThreadToolbarSubState
import com.github.k1rakishou.chan.ui.compose.freeFocusSafe
import com.github.k1rakishou.chan.ui.compose.providers.KurobaWindowInsets
import com.github.k1rakishou.chan.ui.compose.providers.LocalChanTheme
import com.github.k1rakishou.chan.ui.compose.providers.LocalWindowInsets
import com.github.k1rakishou.chan.ui.compose.requestFocusSafe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach

@Composable
fun KurobaToolbar(
  kurobaToolbarState: KurobaToolbarState?,
  showFloatingMenu: (List<AbstractToolbarMenuOverflowItem>) -> Unit
) {
  if (kurobaToolbarState == null) {
    return
  }

  val overriddenTheme by kurobaToolbarState.overriddenTheme
  val isThemeOverridden = overriddenTheme != null

  val chanTheme = kurobaToolbarState.overriddenTheme.value
    ?: LocalChanTheme.current

  val windowInsets = if (isThemeOverridden) {
    remember { KurobaWindowInsets() }
  } else {
    LocalWindowInsets.current
  }

  val focusRequester = remember { FocusRequester() }

  if (!isThemeOverridden) {
    val toolbarVisible by kurobaToolbarState.toolbarVisibleState.collectAsState(initial = false)
    if (!toolbarVisible) {
      return
    }

    val toolbarFullyInvisible by remember(key1 = kurobaToolbarState) {
      derivedStateOf {
        kurobaToolbarState.toolbarAlpha.floatValue <= 0f
      }
    }

    if (toolbarFullyInvisible) {
      return
    }
  }

  if (!isThemeOverridden) {
    KeyboardRequester(
      kurobaToolbarState = kurobaToolbarState,
      focusRequester = focusRequester
    )
  }

  KurobaContainerToolbarContent(
    isThemeOverridden = isThemeOverridden,
    chanTheme = chanTheme,
    windowInsets = windowInsets,
    kurobaToolbarState = kurobaToolbarState
  ) { childToolbarState ->
    when (childToolbarState) {
      is KurobaContainerToolbarSubState -> {
        error("Must be a toolbar container")
      }
      is KurobaCatalogToolbarSubState -> {
        KurobaCatalogToolbarContent(
          modifier = Modifier.fillMaxSize(),
          chanTheme = chanTheme,
          state = childToolbarState,
          showFloatingMenu = showFloatingMenu
        )
      }
      is KurobaThreadToolbarSubState -> {
        KurobaThreadToolbarContent(
          modifier = Modifier.fillMaxSize(),
          chanTheme = chanTheme,
          state = childToolbarState,
          showFloatingMenu = showFloatingMenu
        )
      }
      is KurobaThreadSearchToolbarSubState -> {
        KurobaThreadSearchToolbarContent(
          modifier = Modifier.fillMaxSize(),
          chanTheme = chanTheme,
          state = childToolbarState,
          focusRequester = focusRequester,
          onCloseSearchToolbarButtonClicked = {
            if (kurobaToolbarState.isInThreadSearchMode()) {
              kurobaToolbarState.pop()
            }
          }
        )
      }
      is KurobaDefaultToolbarSubState -> {
        KurobaDefaultToolbarContent(
          modifier = Modifier.fillMaxSize(),
          chanTheme = chanTheme,
          state = childToolbarState,
          showFloatingMenu = showFloatingMenu
        )
      }
      is KurobaSearchToolbarSubState -> {
        KurobaSearchToolbarContent(
          modifier = Modifier.fillMaxSize(),
          chanTheme = chanTheme,
          state = childToolbarState,
          focusRequester = focusRequester,
          onCloseSearchToolbarButtonClicked = {
            if (kurobaToolbarState.isInSearchMode()) {
              kurobaToolbarState.pop()
            }
          }
        )
      }
      is KurobaCatalogSearchToolbarSubState -> {
        KurobaCatalogSearchToolbarContent(
          modifier = Modifier.fillMaxSize(),
          chanTheme = chanTheme,
          state = childToolbarState,
          focusRequester = focusRequester,
          onCloseSearchToolbarButtonClicked = {
            if (kurobaToolbarState.isInCatalogSearchMode()) {
              kurobaToolbarState.pop()
            }
          }
        )
      }
      is KurobaSelectionToolbarSubState -> {
        KurobaSelectionToolbarContent(
          modifier = Modifier.fillMaxSize(),
          chanTheme = chanTheme,
          state = childToolbarState,
          showFloatingMenu = showFloatingMenu
        )
      }
      is KurobaReplyToolbarSubState -> {
        KurobaReplyToolbarContent(
          modifier = Modifier.fillMaxSize(),
          chanTheme = chanTheme,
          state = childToolbarState,
          showFloatingMenu = showFloatingMenu
        )
      }
      null -> {
        // no-op
      }
    }
  }
}

@Composable
private fun KeyboardRequester(
  kurobaToolbarState: KurobaToolbarState,
  focusRequester: FocusRequester
) {
  LaunchedEffect(key1 = kurobaToolbarState) {
    kurobaToolbarState.keyboardOpenRequesters
      .onEach { delay(250) }
      .collectLatest { keyboardOpenRequesters ->
        if (keyboardOpenRequesters.isEmpty()) {
          focusRequester.freeFocusSafe()
        } else {
          focusRequester.requestFocusSafe()
        }
      }
  }
}