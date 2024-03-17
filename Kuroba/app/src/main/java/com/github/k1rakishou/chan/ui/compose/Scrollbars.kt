package com.github.k1rakishou.chan.ui.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.core_themes.ChanTheme

private val DefaultPaddingValues = PaddingValues(0.dp)
val SCROLLBAR_WIDTH = 8.dp

fun Modifier.simpleVerticalScrollbar(
  state: LazyGridState,
  chanTheme: ChanTheme,
  contentPadding: PaddingValues = DefaultPaddingValues,
  width: Dp = SCROLLBAR_WIDTH
): Modifier {
  return composed {
    val targetAlpha = if (state.isScrollInProgress) 0.8f else 0f
    val duration = if (state.isScrollInProgress) 10 else 1500

    val alpha by animateFloatAsState(
      targetValue = targetAlpha,
      animationSpec = tween(durationMillis = duration)
    )

    this.then(
      Modifier.drawWithContent {
        drawContent()

        val layoutInfo = state.layoutInfo
        val firstVisibleElementIndex = layoutInfo.visibleItemsInfo.firstOrNull()?.index
        val needDrawScrollbar = layoutInfo.totalItemsCount > layoutInfo.visibleItemsInfo.size
          && (state.isScrollInProgress || alpha > 0.0f)

        // Draw scrollbar if total item count is greater than visible item count and either
        // currently scrolling or if the animation is still running and lazy column has content
        if (!needDrawScrollbar || firstVisibleElementIndex == null) {
          return@drawWithContent
        }

        val topPaddingPx = contentPadding.calculateTopPadding().toPx()
        val bottomPaddingPx = contentPadding.calculateBottomPadding().toPx()
        val totalHeightWithoutPaddings = this.size.height - topPaddingPx - bottomPaddingPx

        val elementHeight = totalHeightWithoutPaddings / layoutInfo.totalItemsCount
        val scrollbarOffsetY = firstVisibleElementIndex * elementHeight
        val scrollbarHeight = layoutInfo.visibleItemsInfo.size * elementHeight

        drawRect(
          color = chanTheme.textColorHintCompose,
          topLeft = Offset(this.size.width - width.toPx(), topPaddingPx + scrollbarOffsetY),
          size = Size(width.toPx(), scrollbarHeight),
          alpha = alpha
        )
      }
    )
  }
}

fun Modifier.simpleVerticalScrollbar(
  state: LazyListState,
  chanTheme: ChanTheme,
  contentPadding: PaddingValues = DefaultPaddingValues,
  width: Dp = SCROLLBAR_WIDTH
): Modifier {
  return composed {
    val targetAlpha = if (state.isScrollInProgress) 0.8f else 0f
    val duration = if (state.isScrollInProgress) 10 else 1500

    val alpha by animateFloatAsState(
      targetValue = targetAlpha,
      animationSpec = tween(durationMillis = duration)
    )

    this.then(
      Modifier.drawWithContent {
        drawContent()

        val layoutInfo = state.layoutInfo
        val firstVisibleElementIndex = layoutInfo.visibleItemsInfo.firstOrNull()?.index
        val needDrawScrollbar = layoutInfo.totalItemsCount > layoutInfo.visibleItemsInfo.size
          && (state.isScrollInProgress || alpha > 0.0f)

        // Draw scrollbar if total item count is greater than visible item count and either
        // currently scrolling or if the animation is still running and lazy column has content
        if (!needDrawScrollbar || firstVisibleElementIndex == null) {
          return@drawWithContent
        }

        val topPaddingPx = contentPadding.calculateTopPadding().toPx()
        val bottomPaddingPx = contentPadding.calculateBottomPadding().toPx()
        val totalHeightWithoutPaddings = this.size.height - topPaddingPx - bottomPaddingPx

        val elementHeight = totalHeightWithoutPaddings / layoutInfo.totalItemsCount
        val scrollbarOffsetY = firstVisibleElementIndex * elementHeight
        val scrollbarHeight = layoutInfo.visibleItemsInfo.size * elementHeight

        drawRect(
          color = chanTheme.textColorHintCompose,
          topLeft = Offset(this.size.width - width.toPx(), topPaddingPx + scrollbarOffsetY),
          size = Size(width.toPx(), scrollbarHeight),
          alpha = alpha
        )
      }
    )
  }
}

/**
 * Vertical scrollbar for Composables that use ScrollState (like verticalScroll())
 * */
fun Modifier.verticalScrollbar(
  contentPadding: PaddingValues,
  scrollState: ScrollState,
  thumbColor: Color,
  enabled: Boolean = true
): Modifier {
  if (!enabled) {
    return this
  }

  return composed {
    val density = LocalDensity.current

    val scrollbarWidth = with(density) { 4.dp.toPx() }
    val scrollbarHeight = with(density) { 16.dp.toPx() }

    val scrollStateUpdated by rememberUpdatedState(newValue = scrollState)
    val currentPositionPx by remember { derivedStateOf { scrollStateUpdated.value } }
    val maxScrollPositionPx by remember { derivedStateOf { scrollStateUpdated.maxValue } }

    val topPaddingPx = with(density) {
      remember(key1 = contentPadding) { contentPadding.calculateTopPadding().toPx() }
    }
    val bottomPaddingPx = with(density) {
      remember(key1 = contentPadding) { contentPadding.calculateBottomPadding().toPx() }
    }

    val duration = if (scrollStateUpdated.isScrollInProgress) 150 else 1000
    val delay = if (scrollStateUpdated.isScrollInProgress) 0 else 1000
    val targetThumbAlpha = if (scrollStateUpdated.isScrollInProgress) 0.8f else 0f

    val thumbAlphaAnimated by animateFloatAsState(
      targetValue = targetThumbAlpha,
      animationSpec = tween(
        durationMillis = duration,
        delayMillis = delay
      )
    )

    return@composed Modifier.drawWithContent {
      drawContent()

      if (maxScrollPositionPx == Int.MAX_VALUE || maxScrollPositionPx == 0) {
        return@drawWithContent
      }

      val availableHeight = this.size.height - scrollbarHeight - topPaddingPx - bottomPaddingPx
      val unit = availableHeight / maxScrollPositionPx.toFloat()
      val scrollPosition = currentPositionPx * unit

      val offsetX = this.size.width - scrollbarWidth
      val offsetY = topPaddingPx + scrollPosition

      drawRect(
        color = thumbColor,
        topLeft = Offset(offsetX, offsetY),
        size = Size(scrollbarWidth, scrollbarHeight),
        alpha = thumbAlphaAnimated
      )
    }
  }
}
