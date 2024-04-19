package com.github.k1rakishou.chan.ui.compose.lazylist.wrapper

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

fun newLazyGridStateWrapper(
  initialFirstVisibleItemIndex: Int = 0,
  initialFirstVisibleItemScrollOffset: Int = 0
): LazyGridStateWrapper {
  val lazyGridState = LazyGridState(initialFirstVisibleItemIndex, initialFirstVisibleItemScrollOffset)
  return LazyGridStateWrapper(lazyGridState)
}

@Stable
class LazyGridStateWrapper(
  val lazyGridState: LazyGridState
) : LazyStateWrapper<LazyGridItemInfoWrapper, LazyGridLayoutInfoWrapper> {

  override val isScrollInProgress: Boolean
    get() = lazyGridState.isScrollInProgress
  override val canScrollBackward: Boolean
    get() = lazyGridState.canScrollBackward
  override val canScrollForward: Boolean
    get() = lazyGridState.canScrollForward
  override val firstVisibleItemIndex: Int
    get() = lazyGridState.firstVisibleItemIndex
  override val firstVisibleItemScrollOffset: Int
    get() = lazyGridState.firstVisibleItemScrollOffset
  override val visibleItemsCount: Int
    get() = lazyGridState.layoutInfo.visibleItemsInfo.size
  override val fullyVisibleItemsCount: Int
    get() = lazyGridState.layoutInfo.visibleItemsInfo.count { lazyListItemInfo -> lazyListItemInfo.offset.y >= 0 }
  override val totalItemsCount: Int
    get() = lazyGridState.layoutInfo.totalItemsCount
  override val viewportHeight: Int
    get() = lazyGridState.layoutInfo.viewportEndOffset - lazyGridState.layoutInfo.viewportStartOffset

  override val layoutInfo: LazyGridLayoutInfoWrapper = LazyGridLayoutInfoWrapper(lazyGridState)

  override suspend fun scrollToItem(index: Int, scrollOffset: Int) {
    lazyGridState.scrollToItem(index, scrollOffset)
  }

}

@Stable
class LazyGridLayoutInfoWrapper(
  val lazyGridState: LazyGridState
) : LazyLayoutInfoWrapper<LazyGridItemInfoWrapper> {

  override val visibleItemsInfo: List<LazyGridItemInfoWrapper>
    get() {
      return lazyGridState.layoutInfo.visibleItemsInfo
        .map { lazyGridItemInfo -> LazyItemInfoWrapper.fromLazyGridItemInfo(lazyGridItemInfo) }
    }
  override val viewportStartOffset: Int
    get() = lazyGridState.layoutInfo.viewportStartOffset
  override val viewportEndOffset: Int
    get() = lazyGridState.layoutInfo.viewportEndOffset
  override val totalItemsCount: Int
    get() = lazyGridState.layoutInfo.totalItemsCount
  override val viewportSize: IntSize
    get() = lazyGridState.layoutInfo.viewportSize
  override val orientation: Orientation
    get() = lazyGridState.layoutInfo.orientation
  override val reverseLayout: Boolean
    get() = lazyGridState.layoutInfo.reverseLayout
  override val beforeContentPadding: Int
    get() = lazyGridState.layoutInfo.beforeContentPadding
  override val afterContentPadding: Int
    get() = lazyGridState.layoutInfo.afterContentPadding
}

@Stable
class LazyGridItemInfoWrapper(
  override val index: Int,
  override val key: Any,
  override val offsetY: Int,
  val offset: IntOffset,
  val size: IntSize
) : LazyItemInfoWrapper