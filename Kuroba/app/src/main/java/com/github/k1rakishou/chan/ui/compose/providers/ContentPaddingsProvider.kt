package com.github.k1rakishou.chan.ui.compose.providers

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.chan.ui.controller.base.ControllerKey
import com.github.k1rakishou.chan.utils.activityDependencies
import com.github.k1rakishou.chan.utils.appDependencies
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach

val LocalContentPaddings = staticCompositionLocalOf<ContentPaddings> { error("ContentPaddings not provided") }

@Composable
fun ProvideContentPaddings(
  content: @Composable () -> Unit
) {
  val globalUiStateHolder = appDependencies().globalUiStateHolder
  val globalWindowInsetsManager = activityDependencies().globalWindowInsetsManager

  var contentPaddings by remember { mutableStateOf(ContentPaddings()) }

  LaunchedEffect(key1 = Unit) {
    combine(
      snapshotFlow { globalWindowInsetsManager.currentWindowInsets.value },
      globalUiStateHolder.toolbar.toolbarHeight,
      globalUiStateHolder.bottomPanel.bottomPanelHeight,
      globalUiStateHolder.bottomPanel.controllersHoldingBottomPanel
    ) { currentWindowInsets, toolbarHeight, bottomPanelHeight, controllersHoldingBottomPanel ->
      return@combine ContentPaddingsInfo(
        currentWindowInsets = currentWindowInsets,
        toolbarHeight = toolbarHeight,
        bottomPanelHeight = bottomPanelHeight,
        controllersHoldingBottomPanel = controllersHoldingBottomPanel
      )
    }
      .onEach { contentPaddingsInfo ->
        contentPaddings = ContentPaddings(
          topInset = contentPaddingsInfo.currentWindowInsets.top,
          bottomInset = contentPaddingsInfo.currentWindowInsets.bottom,
          toolbarHeight = contentPaddingsInfo.toolbarHeight,
          bottomPanelHeight = contentPaddingsInfo.bottomPanelHeight,
          controllersHoldingBottomPanel = contentPaddingsInfo.controllersHoldingBottomPanel
        )
      }
      .collect()
  }

  CompositionLocalProvider(LocalContentPaddings provides contentPaddings) {
    content()
  }
}

private data class ContentPaddingsInfo(
  val currentWindowInsets: KurobaWindowInsets,
  val toolbarHeight: Dp,
  val bottomPanelHeight: Dp,
  val controllersHoldingBottomPanel: Set<ControllerKey>
)

data class ContentPaddings(
  val topInset: Dp = 0.dp,
  val toolbarHeight: Dp = 0.dp,
  val bottomInset: Dp = 0.dp,
  val bottomPanelHeight: Dp = 0.dp,
  val controllersHoldingBottomPanel: Set<ControllerKey> = emptySet()
) {

  fun calculateBottomPadding(controllerKey: ControllerKey): Dp {
    val bottomPanelHeight = if (controllerKey in controllersHoldingBottomPanel) {
      bottomPanelHeight
    } else {
      0.dp
    }

    return maxOf(bottomInset, bottomPanelHeight)
  }

  fun calculateTopPadding(): Dp {
    return maxOf(
      topInset,
      toolbarHeight
    )
  }

  fun asPaddingValues(controllerKey: ControllerKey): PaddingValues {
    return PaddingValues(
      top = calculateTopPadding(),
      bottom = calculateBottomPadding(controllerKey)
    )
  }

}