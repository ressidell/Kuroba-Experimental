package com.github.k1rakishou.chan.features.album

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.chan.ui.compose.image.ImageLoaderRequest
import com.github.k1rakishou.chan.ui.compose.image.KurobaComposePostImageThumbnail
import com.github.k1rakishou.chan.ui.compose.providers.LocalChanTheme
import com.github.k1rakishou.chan.utils.appDependencies
import com.github.k1rakishou.core_themes.ThemeEngine

@Composable
fun AlbumItem(
  modifier: Modifier,
  isInSelectionMode: Boolean,
  isSelected: Boolean,
  albumItemData: AlbumViewControllerV2ViewModel.AlbumItemData,
  onClick: (AlbumViewControllerV2ViewModel.AlbumItemData) -> Unit,
  onLongClick: (AlbumViewControllerV2ViewModel.AlbumItemData) -> Unit
) {
  val onDemandContentLoaderManager = appDependencies().onDemandContentLoaderManager

  DisposableEffect(key1 = Unit) {
    onDemandContentLoaderManager.onPostBind(albumItemData.postDescriptor, albumItemData.isCatalogMode)
    onDispose { onDemandContentLoaderManager.onPostUnbind(albumItemData.postDescriptor, albumItemData.isCatalogMode) }
  }

  val request = remember(key1 = albumItemData.thumbnailImage) {
    ImageLoaderRequest(
      data = albumItemData.thumbnailImage,
      transformations = emptyList()
    )
  }

  Box(
    modifier = modifier
      .albumItemSelection(isInSelectionMode, isSelected)
  ) {
    KurobaComposePostImageThumbnail(
      modifier = Modifier.fillMaxSize(),
      key = albumItemData.albumItemDataKey,
      request = request,
      mediaType = albumItemData.mediaType,
      displayErrorMessage = true,
      showShimmerEffectWhenLoading = true,
      contentScale = ContentScale.Crop,
      onClick = { onClick(albumItemData) },
      onLongClick = { onLongClick(albumItemData) }
    )
  }
}

private fun Modifier.albumItemSelection(
  isInSelectionMode: Boolean,
  isSelected: Boolean,
): Modifier {
  return composed {
    val chanTheme = LocalChanTheme.current

    val borderColor = when {
      isInSelectionMode -> {
        if (isSelected) {
          chanTheme.accentColorCompose
        } else {
          if (ThemeEngine.isDarkColor(chanTheme.backColorCompose)) {
            Color.DarkGray
          } else {
            Color.LightGray
          }
        }
      }
      else -> Color.Transparent
    }

    val scaleAnimated = animateFloatAsState(targetValue = if (isInSelectionMode && isSelected) 0.9f else 1f)
    val borderAlphaAnimated = animateFloatAsState(targetValue = if (isInSelectionMode) 1f else 0f)
    val borderColorAnimated = animateColorAsState(targetValue = borderColor)
    val unselectedImageTintAlphaAnimated = animateFloatAsState(targetValue = if (isInSelectionMode && !isSelected) 0.6f else 0f)

    return@composed graphicsLayer {
      scaleX = scaleAnimated.value
      scaleY = scaleAnimated.value
    }.drawWithContent {
      drawContent()

      drawRect(
        color = chanTheme.backColorCompose,
        alpha = unselectedImageTintAlphaAnimated.value
      )

      drawRect(
        color = borderColorAnimated.value,
        alpha = borderAlphaAnimated.value,
        style = Stroke(width = 2.dp.toPx())
      )
    }
  }
}