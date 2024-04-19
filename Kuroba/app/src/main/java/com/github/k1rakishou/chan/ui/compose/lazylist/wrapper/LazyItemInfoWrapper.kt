package com.github.k1rakishou.chan.ui.compose.lazylist.wrapper

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemInfo
import androidx.compose.runtime.Stable


@Stable
interface LazyItemInfoWrapper {
  val index: Int
  val key: Any
  val offsetY: Int

  companion object {
    fun fromLazyListItemInfo(lazyListItemInfo: LazyListItemInfo): LazyListItemInfoWrapper {
      return LazyListItemInfoWrapper(
        index = lazyListItemInfo.index,
        key = lazyListItemInfo.key,
        offsetY = lazyListItemInfo.offset,
        offset = lazyListItemInfo.offset,
        size = lazyListItemInfo.size,
      )
    }

    fun fromLazyGridItemInfo(lazyGridItemInfo: LazyGridItemInfo): LazyGridItemInfoWrapper {
      return LazyGridItemInfoWrapper(
        index = lazyGridItemInfo.index,
        key = lazyGridItemInfo.key,
        offsetY = lazyGridItemInfo.offset.y,
        offset = lazyGridItemInfo.offset,
        size = lazyGridItemInfo.size,
      )
    }

    fun fromLazyStaggeredGridItemInfo(lazyStaggeredGridItemInfo: LazyStaggeredGridItemInfo): LazyStaggeredGridItemInfoWrapper {
      return LazyStaggeredGridItemInfoWrapper(
        index = lazyStaggeredGridItemInfo.index,
        key = lazyStaggeredGridItemInfo.key,
        offsetY = lazyStaggeredGridItemInfo.offset.y,
        offset = lazyStaggeredGridItemInfo.offset,
        size = lazyStaggeredGridItemInfo.size,
      )
    }
  }

}
