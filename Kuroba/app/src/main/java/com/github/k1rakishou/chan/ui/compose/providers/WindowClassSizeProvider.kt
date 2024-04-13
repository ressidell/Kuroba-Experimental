package com.github.k1rakishou.chan.ui.compose.providers

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.window.layout.WindowMetricsCalculator
import com.github.k1rakishou.chan.ui.compose.window.WindowSizeClass
import com.github.k1rakishou.chan.utils.appDependencies

val LocalWindowSizeClass = staticCompositionLocalOf<WindowSizeClass> { error("LocalWindowSizeClass not initialized") }

@Composable
fun ProvideWindowClassSize(activity: ComponentActivity, content: @Composable () -> Unit) {
  val globalUiStateHolder = appDependencies().globalUiStateHolder

  @Suppress("UNUSED_VARIABLE") val configuration = LocalConfiguration.current

  val density = LocalDensity.current
  val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity)
  val size = with(density) { metrics.bounds.toComposeRect().size.toDpSize() }
  val windowClassSize = WindowSizeClass.calculateFromSize(size)

  LaunchedEffect(key1 = windowClassSize) {
    globalUiStateHolder.updateMainUiState { updateWindowSizeClass(windowClassSize) }
  }

  CompositionLocalProvider(LocalWindowSizeClass provides windowClassSize) {
    content()
  }

}