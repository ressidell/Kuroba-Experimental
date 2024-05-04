package com.github.k1rakishou.chan.features.album

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.github.k1rakishou.chan.ui.compose.image.ImageLoaderRequest
import com.github.k1rakishou.chan.ui.compose.image.KurobaComposePostImageThumbnail
import com.github.k1rakishou.chan.utils.appDependencies

@Composable
fun AlbumItem(
  modifier: Modifier,
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

  KurobaComposePostImageThumbnail(
    modifier = modifier,
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