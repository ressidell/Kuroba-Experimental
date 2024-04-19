package com.github.k1rakishou.chan.ui.compose.lazylist.wrapper

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

fun newLazyStaggeredGridStateWrapper(
  initialFirstVisibleItemIndex: Int = 0,
  initialFirstVisibleItemScrollOffset: Int = 0
): LazyStaggeredGridStateWrapper {
  val lazyStaggeredGridState = LazyStaggeredGridState(initialFirstVisibleItemIndex, initialFirstVisibleItemScrollOffset)
  return LazyStaggeredGridStateWrapper(lazyStaggeredGridState)
}

@Stable
class LazyStaggeredGridStateWrapper(
  val lazyStaggeredGridState: LazyStaggeredGridState
) : LazyStateWrapper<LazyStaggeredGridItemInfoWrapper, LazyStaggeredGridLayoutInfoWrapper> {

  override val isScrollInProgress: Boolean
    get() = lazyStaggeredGridState.isScrollInProgress
  override val canScrollBackward: Boolean
    get() = lazyStaggeredGridState.canScrollBackward
  override val canScrollForward: Boolean
    get() = lazyStaggeredGridState.canScrollForward
  override val firstVisibleItemIndex: Int
    get() = lazyStaggeredGridState.firstVisibleItemIndex
  override val firstVisibleItemScrollOffset: Int
    get() = lazyStaggeredGridState.firstVisibleItemScrollOffset
  override val visibleItemsCount: Int
    get() = lazyStaggeredGridState.layoutInfo.visibleItemsInfo.size
  override val fullyVisibleItemsCount: Int
    get() = lazyStaggeredGridState.layoutInfo.visibleItemsInfo.count { lazyListItemInfo -> lazyListItemInfo.offset.y >= 0 }
  override val totalItemsCount: Int
    get() = lazyStaggeredGridState.layoutInfo.totalItemsCount
  override val viewportHeight: Int
    get() = lazyStaggeredGridState.layoutInfo.viewportEndOffset - lazyStaggeredGridState.layoutInfo.viewportStartOffset

  override val layoutInfo: LazyStaggeredGridLayoutInfoWrapper = LazyStaggeredGridLayoutInfoWrapper(lazyStaggeredGridState)

  override suspend fun scrollToItem(index: Int, scrollOffset: Int) {
    lazyStaggeredGridState.scrollToItem(index, scrollOffset)
  }

}

@Stable
class LazyStaggeredGridLayoutInfoWrapper(
  val lazyStaggeredGridState: LazyStaggeredGridState
) : LazyLayoutInfoWrapper<LazyStaggeredGridItemInfoWrapper> {

  override val visibleItemsInfo: List<LazyStaggeredGridItemInfoWrapper>
    get() {
      return lazyStaggeredGridState.layoutInfo.visibleItemsInfo
        .map { lazyGridItemInfo -> LazyItemInfoWrapper.fromLazyStaggeredGridItemInfo(lazyGridItemInfo) }
    }

  override val viewportStartOffset: Int
    get() = lazyStaggeredGridState.layoutInfo.viewportStartOffset
  override val viewportEndOffset: Int
    get() = lazyStaggeredGridState.layoutInfo.viewportEndOffset
  override val totalItemsCount: Int
    get() = lazyStaggeredGridState.layoutInfo.totalItemsCount
  override val viewportSize: IntSize
    get() = lazyStaggeredGridState.layoutInfo.viewportSize
  override val orientation: Orientation
    get() = lazyStaggeredGridState.layoutInfo.orientation
  // TODO: LazyStaggeredGridState doesn't provide reverseLayout field
  override val reverseLayout: Boolean
    get() = false
  override val beforeContentPadding: Int
    get() = lazyStaggeredGridState.layoutInfo.beforeContentPadding
  override val afterContentPadding: Int
    get() = lazyStaggeredGridState.layoutInfo.afterContentPadding
}

@Stable
class LazyStaggeredGridItemInfoWrapper(
  override val index: Int,
  override val key: Any,
  override val offsetY: Int,
  val offset: IntOffset,
  val size: IntSize
) : LazyItemInfoWrapper

@Stable
interface LazyLayoutInfoWrapper<T : LazyItemInfoWrapper> {
  val visibleItemsInfo: List<T>
  val viewportStartOffset: Int
  val viewportEndOffset: Int
  val totalItemsCount: Int
  val viewportSize: IntSize get() = IntSize.Zero
  val orientation: Orientation get() = Orientation.Vertical
  val reverseLayout: Boolean get() = false
  val beforeContentPadding: Int get() = 0
  val afterContentPadding: Int get() = 0
}