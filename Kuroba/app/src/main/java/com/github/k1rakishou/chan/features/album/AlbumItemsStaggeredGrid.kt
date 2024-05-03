package com.github.k1rakishou.chan.features.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.chan.ui.compose.lazylist.LazyVerticalStaggeredGridWithFastScroller
import com.github.k1rakishou.chan.ui.compose.providers.LocalContentPaddings
import com.github.k1rakishou.chan.ui.controller.base.ControllerKey

@Composable
fun AlbumItemsStaggeredGrid(
  controllerKey: ControllerKey,
  albumItems: SnapshotStateList<AlbumViewControllerV2ViewModel.AlbumItemData>,
  albumSpanCount: Int
) {
  val contentPaddings = LocalContentPaddings.current

  LazyVerticalStaggeredGridWithFastScroller(
    modifier = Modifier.fillMaxSize(),
    columns = StaggeredGridCells.Fixed(albumSpanCount),
    verticalItemSpacing = 2.dp,
    horizontalArrangement = Arrangement.spacedBy(2.dp),
    contentPadding = remember(contentPaddings, controllerKey) { contentPaddings.asPaddingValues(controllerKey) }
  ) {
    items(
      count = albumItems.size,
      key = { index -> albumItems.get(index).composeKey },
      contentType = { "album_item" },
      itemContent = { index ->
        val albumItemData = albumItems[index]

        AlbumItem(
          modifier = Modifier
            .fillMaxSize()
            .let { modifier ->
              val aspectRatio = albumItemData.albumItemPostData?.aspectRatio
              if (aspectRatio == null) {
                return@let modifier
              }

              return@let modifier.aspectRatio(aspectRatio)
            }
          ,
          albumItemData = albumItemData
        )
      }
    )
  }
}