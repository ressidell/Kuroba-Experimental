package com.github.k1rakishou.chan.features.album

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.core.cache.CacheFileType
import com.github.k1rakishou.chan.ui.compose.image.ImageLoaderRequest
import com.github.k1rakishou.chan.ui.compose.image.ImageLoaderRequestData
import com.github.k1rakishou.chan.ui.compose.image.KurobaComposePostImageThumbnail
import com.github.k1rakishou.chan.ui.compose.providers.LocalChanTheme
import com.github.k1rakishou.chan.utils.appDependencies
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun AlbumItem(
  modifier: Modifier,
  isInSelectionMode: Boolean,
  isSelected: Boolean,
  albumItemData: AlbumViewControllerV2ViewModel.AlbumItemData,
  downloadingAlbumItem: AlbumViewControllerV2ViewModel.DownloadingAlbumItem?,
  onClick: (AlbumViewControllerV2ViewModel.AlbumItemData) -> Unit,
  onLongClick: (AlbumViewControllerV2ViewModel.AlbumItemData) -> Unit,
  clearDownloadingAlbumItemState: (AlbumViewControllerV2ViewModel.DownloadingAlbumItem) -> Unit
) {
  val onDemandContentLoaderManager = appDependencies().onDemandContentLoaderManager

  DisposableEffect(key1 = Unit) {
    onDemandContentLoaderManager.onPostBind(albumItemData.postDescriptor, albumItemData.isCatalogMode)
    onDispose { onDemandContentLoaderManager.onPostUnbind(albumItemData.postDescriptor, albumItemData.isCatalogMode) }
  }

  val request = remember(key1 = albumItemData.thumbnailImageUrl) {
    ImageLoaderRequest(
      data = ImageLoaderRequestData.Url(
        httpUrl = albumItemData.thumbnailImageUrl,
        cacheFileType = CacheFileType.PostMediaThumbnail
      ),
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

    DownloadingAlbumItemOverlay(
      modifier = Modifier.fillMaxSize(),
      downloadingAlbumItem = downloadingAlbumItem,
      clearDownloadingAlbumItemState = clearDownloadingAlbumItemState
    )
  }
}

@Composable
fun DownloadingAlbumItemOverlay(
  modifier: Modifier,
  downloadingAlbumItem: AlbumViewControllerV2ViewModel.DownloadingAlbumItem?,
  clearDownloadingAlbumItemState: (AlbumViewControllerV2ViewModel.DownloadingAlbumItem) -> Unit
) {
  if (downloadingAlbumItem == null) {
    return
  }

  LaunchedEffect(key1 = downloadingAlbumItem.state) {
    when (downloadingAlbumItem.state) {
      AlbumViewControllerV2ViewModel.DownloadingAlbumItem.State.Enqueued,
      AlbumViewControllerV2ViewModel.DownloadingAlbumItem.State.Downloading -> {
        // no-op
      }
      AlbumViewControllerV2ViewModel.DownloadingAlbumItem.State.Downloaded,
      AlbumViewControllerV2ViewModel.DownloadingAlbumItem.State.FailedToDownload,
      AlbumViewControllerV2ViewModel.DownloadingAlbumItem.State.Canceled -> {
        delay(2000)
        clearDownloadingAlbumItemState(downloadingAlbumItem)
      }
      AlbumViewControllerV2ViewModel.DownloadingAlbumItem.State.Deleted -> {
        clearDownloadingAlbumItemState(downloadingAlbumItem)
      }
    }
  }

  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center
  ) {
    Box {
      val painter = when (downloadingAlbumItem.state) {
        AlbumViewControllerV2ViewModel.DownloadingAlbumItem.State.Enqueued -> {
          painterResource(id = R.drawable.ic_download_anim0)
        }
        AlbumViewControllerV2ViewModel.DownloadingAlbumItem.State.Downloading -> {
          var animationAtEnd by remember { mutableStateOf(false) }

          LaunchedEffect(downloadingAlbumItem.state) {
            while (isActive) {
              animationAtEnd = !animationAtEnd
              delay(1500)
            }
          }

          rememberAnimatedVectorPainter(
            animatedImageVector = AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_download_anim),
            atEnd = animationAtEnd
          )
        }
        AlbumViewControllerV2ViewModel.DownloadingAlbumItem.State.Downloaded -> {
          painterResource(id = R.drawable.ic_download_anim1)
        }
        AlbumViewControllerV2ViewModel.DownloadingAlbumItem.State.FailedToDownload,
        AlbumViewControllerV2ViewModel.DownloadingAlbumItem.State.Canceled -> {
          painterResource(id = R.drawable.ic_baseline_warning_24)
        }
        AlbumViewControllerV2ViewModel.DownloadingAlbumItem.State.Deleted -> {
          null
        }
      }

      if (painter != null) {
        Image(
          modifier = Modifier
            .size(40.dp)
            .drawBehind { drawCircle(color = Color.Black.copy(alpha = 0.5f)) }
            .padding(4.dp),
          painter = painter,
          contentDescription = null,
        )
      }
    }
  }
}

private fun Modifier.albumItemSelection(
  isInSelectionMode: Boolean,
  isSelected: Boolean,
): Modifier {
  return composed {
    val chanTheme = LocalChanTheme.current

    val inSelectionModeAndSelected = isInSelectionMode && isSelected
    val inSelectionModeAndNotSelected = isInSelectionMode && !isSelected

    val scaleAnimated = animateFloatAsState(
      targetValue = if (inSelectionModeAndSelected) 0.9f else 1f
    )
    val borderColorAnimated = animateColorAsState(
      targetValue = if (inSelectionModeAndSelected) chanTheme.accentColorCompose else Color.Transparent
    )
    val unselectedImageTintAlphaAnimated = animateFloatAsState(
      targetValue = if (inSelectionModeAndNotSelected) 0.6f else 0f
    )

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
        style = Stroke(width = 2.dp.toPx())
      )
    }
  }
}