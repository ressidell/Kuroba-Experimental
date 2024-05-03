package com.github.k1rakishou.chan.ui.compose.image

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.BitmapPainter

@Immutable
sealed class ImageLoaderResult {
  data object NotInitialized : ImageLoaderResult()
  data object Loading : ImageLoaderResult()
  data class Success(val painter: BitmapPainter) : ImageLoaderResult()
  data class Error(val throwable: Throwable) : ImageLoaderResult()
}