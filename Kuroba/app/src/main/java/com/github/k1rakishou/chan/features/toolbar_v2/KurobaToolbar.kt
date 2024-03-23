package com.github.k1rakishou.chan.features.toolbar_v2

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.github.k1rakishou.chan.features.toolbar_v2.state.catalog.KurobaCatalogToolbarContent
import com.github.k1rakishou.chan.features.toolbar_v2.state.catalog.KurobaCatalogToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.container.KurobaContainerToolbarContent
import com.github.k1rakishou.chan.features.toolbar_v2.state.container.KurobaContainerToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.default.KurobaDefaultToolbarContent
import com.github.k1rakishou.chan.features.toolbar_v2.state.default.KurobaDefaultToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.reply.KurobaReplyToolbarContent
import com.github.k1rakishou.chan.features.toolbar_v2.state.reply.KurobaReplyToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.search.KurobaSearchToolbarContent
import com.github.k1rakishou.chan.features.toolbar_v2.state.search.KurobaSearchToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.selection.KurobaSelectionToolbarContent
import com.github.k1rakishou.chan.features.toolbar_v2.state.selection.KurobaSelectionToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.thread.KurobaThreadToolbarContent
import com.github.k1rakishou.chan.features.toolbar_v2.state.thread.KurobaThreadToolbarState

@Composable
fun KurobaToolbar(
  kurobaToolbarState: KurobaToolbarState?
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
      is KurobaContainerToolbarState -> {
        error("Must be a toolbar container")
      }
      is KurobaCatalogToolbarState -> {
        KurobaCatalogToolbarContent(
          modifier = Modifier.fillMaxSize(),
          state = childToolbarState
        )
      }
      is KurobaThreadToolbarState -> {
        KurobaThreadToolbarContent(
          modifier = Modifier.fillMaxSize(),
          state = childToolbarState
        )
      }
      is KurobaDefaultToolbarState -> {
        KurobaDefaultToolbarContent(
          modifier = Modifier.fillMaxSize(),
          state = childToolbarState
        )
      }
      is KurobaSearchToolbarState -> {
        KurobaSearchToolbarContent(
          modifier = Modifier.fillMaxSize(),
          state = childToolbarState
        )
      }
      is KurobaSelectionToolbarState -> {
        KurobaSelectionToolbarContent(
          modifier = Modifier.fillMaxSize(),
          state = childToolbarState
        )
      }
      is KurobaReplyToolbarState -> {
        KurobaReplyToolbarContent(
          modifier = Modifier.fillMaxSize(),
          state = childToolbarState
        )
      }
      null -> {
        // no-op
      }
    }
  }
}