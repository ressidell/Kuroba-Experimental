package com.github.k1rakishou.chan.features.toolbar_v2

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.github.k1rakishou.chan.features.toolbar_v2.state.catalog.KurobaCatalogToolbarContent
import com.github.k1rakishou.chan.features.toolbar_v2.state.catalog.KurobaCatalogToolbarSubState
import com.github.k1rakishou.chan.features.toolbar_v2.state.container.KurobaContainerToolbarContent
import com.github.k1rakishou.chan.features.toolbar_v2.state.container.KurobaContainerToolbarSubState
import com.github.k1rakishou.chan.features.toolbar_v2.state.default.KurobaDefaultToolbarContent
import com.github.k1rakishou.chan.features.toolbar_v2.state.default.KurobaDefaultToolbarSubState
import com.github.k1rakishou.chan.features.toolbar_v2.state.reply.KurobaReplyToolbarContent
import com.github.k1rakishou.chan.features.toolbar_v2.state.reply.KurobaReplyToolbarSubState
import com.github.k1rakishou.chan.features.toolbar_v2.state.search.KurobaSearchToolbarContent
import com.github.k1rakishou.chan.features.toolbar_v2.state.search.KurobaSearchToolbarSubState
import com.github.k1rakishou.chan.features.toolbar_v2.state.selection.KurobaSelectionToolbarContent
import com.github.k1rakishou.chan.features.toolbar_v2.state.selection.KurobaSelectionToolbarSubState
import com.github.k1rakishou.chan.features.toolbar_v2.state.thread.KurobaThreadToolbarContent
import com.github.k1rakishou.chan.features.toolbar_v2.state.thread.KurobaThreadToolbarSubState

@Composable
fun KurobaToolbar(
  kurobaToolbarState: KurobaToolbarState?,
  showFloatingMenu: (List<AbstractToolbarMenuOverflowItem>) -> Unit
) {
  if (kurobaToolbarState == null) {
    return
  }

  val toolbarVisible by kurobaToolbarState.toolbarVisibleState.collectAsState(initial = false)
  if (!toolbarVisible) {
    return
  }

  KurobaContainerToolbarContent(kurobaToolbarState) { childToolbarState ->
    when (childToolbarState) {
      is KurobaContainerToolbarSubState -> {
        error("Must be a toolbar container")
      }
      is KurobaCatalogToolbarSubState -> {
        KurobaCatalogToolbarContent(
          modifier = Modifier.fillMaxSize(),
          state = childToolbarState,
          showFloatingMenu = showFloatingMenu
        )
      }
      is KurobaThreadToolbarSubState -> {
        KurobaThreadToolbarContent(
          modifier = Modifier.fillMaxSize(),
          state = childToolbarState,
          showFloatingMenu = showFloatingMenu
        )
      }
      is KurobaDefaultToolbarSubState -> {
        KurobaDefaultToolbarContent(
          modifier = Modifier.fillMaxSize(),
          state = childToolbarState,
          showFloatingMenu = showFloatingMenu
        )
      }
      is KurobaSearchToolbarSubState -> {
        KurobaSearchToolbarContent(
          modifier = Modifier.fillMaxSize(),
          state = childToolbarState,
          onCloseSearchToolbarButtonClicked = {
            if (kurobaToolbarState.isInSearchMode()) {
              kurobaToolbarState.pop()
            }
          }
        )
      }
      is KurobaSelectionToolbarSubState -> {
        KurobaSelectionToolbarContent(
          modifier = Modifier.fillMaxSize(),
          state = childToolbarState
        )
      }
      is KurobaReplyToolbarSubState -> {
        KurobaReplyToolbarContent(
          modifier = Modifier.fillMaxSize(),
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