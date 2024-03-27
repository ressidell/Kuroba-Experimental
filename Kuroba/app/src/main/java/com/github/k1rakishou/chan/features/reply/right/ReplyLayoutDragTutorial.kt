package com.github.k1rakishou.chan.features.reply.right

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.chan.ui.compose.providers.LocalChanTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

fun Modifier.replyLayoutDragTutorial(enabled: Boolean, cornerRadius: CornerRadius): Modifier {
  if (!enabled) {
    return this
  }

  return composed {
    val density = LocalDensity.current
    val chanTheme = LocalChanTheme.current

    val startOffset = with(density) { 8.dp.toPx() }
    val arrowsPadding = with(density) { 4.dp.toPx() }
    val arrowWidth = with(density) { 12.dp.toPx() }
    val arrowHeight = with(density) { 24.dp.toPx() }
    val arrowStrokeWidth = with(density) { 4.dp.toPx() }

    var currentTime by remember { mutableLongStateOf(0L) }
    var focusedArrowAnimationLastUpdateTime by remember { mutableLongStateOf(0L) }
    var currentFocusedArrow by remember { mutableIntStateOf(0) }

    LaunchedEffect(key1 = Unit) {
      while (isActive) {
        delay(16)
        currentTime = System.currentTimeMillis()
      }
    }

    val arrowPath = remember {
      Path().apply {
        moveTo(0f, 0f)
        lineTo(0f, 0f)
        lineTo(0f, 0f)
      }
    }

    return@composed Modifier.drawWithContent {
      drawContent()

      var offset = startOffset
      val arrowTotalHeight = arrowsPadding + arrowHeight
      val availableHeight = size.height - arrowHeight
      val arrowsToDrawCount = (availableHeight / arrowTotalHeight).toInt()

      if (currentTime - focusedArrowAnimationLastUpdateTime > 125L) {
        focusedArrowAnimationLastUpdateTime = currentTime

        var localCurrentFocusedArrow = currentFocusedArrow - 1
        if (localCurrentFocusedArrow < 0) {
          localCurrentFocusedArrow = arrowsToDrawCount
        }

        currentFocusedArrow = localCurrentFocusedArrow
      }

      drawRoundRect(
        color = chanTheme.accentColorCompose.copy(alpha = 0.7f),
        cornerRadius = cornerRadius
      )

      repeat(arrowsToDrawCount) { arrowIndex ->
        val alpha = if (currentFocusedArrow == arrowIndex) 1f else .5f
        val arrowSize = Size(width = arrowWidth, height = arrowHeight)

        with(arrowPath) {
          rewind()

          moveTo(0f, 0f)
          lineTo(arrowSize.width, arrowSize.height / 2f)
          lineTo(0f, arrowSize.height)
        }

        translate(top = offset) {
          val horizontalInset = (size.width - arrowWidth) / 2
          val verticalInset = (size.height - (arrowsToDrawCount * arrowTotalHeight)) / 2f

          inset(
            horizontal = horizontalInset,
            vertical = verticalInset
          ) {
            rotate(
              degrees = -90f,
              pivot = arrowPath.getBounds().center
            ) {
              drawPath(
                path = arrowPath,
                color = androidx.compose.ui.graphics.Color.White,
                style = Stroke(width = arrowStrokeWidth),
                alpha = alpha
              )
            }
          }
        }

        offset += arrowTotalHeight
      }
    }
  }
}
