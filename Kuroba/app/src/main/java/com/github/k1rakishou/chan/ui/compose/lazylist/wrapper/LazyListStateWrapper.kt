package com.github.k1rakishou.chan.ui.compose.lazylist.wrapper

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntSize

fun newLazyListStateWrapper(
  initialFirstVisibleItemIndex: Int = 0,
  initialFirstVisibleItemScrollOffset: Int = 0
): LazyListStateWrapper {
  val lazyListState = LazyListState(initialFirstVisibleItemIndex, initialFirstVisibleItemScrollOffset)
  return LazyListStateWrapper(lazyListState)
}

@Stable
class LazyListStateWrapper(
  val lazyListState: LazyListState
) : LazyStateWrapper<LazyListItemInfoWrapper, LazyListLayoutInfoWrapper> {

  override val isScrollInProgress: Boolean
    get() = lazyListState.isScrollInProgress
  override val canScrollBackward: Boolean
    get() = lazyListState.canScrollBackward
  override val canScrollForward: Boolean
    get() = lazyListState.canScrollForward
  override val firstVisibleItemIndex: Int
    get() = lazyListState.firstVisibleItemIndex
  override val firstVisibleItemScrollOffset: Int
    get() = lazyListState.firstVisibleItemScrollOffset
  override val visibleItemsCount: Int
    get() = lazyListState.layoutInfo.visibleItemsInfo.size
  override val fullyVisibleItemsCount: Int
    get() = lazyListState.layoutInfo.visibleItemsInfo.count { lazyListItemInfo -> lazyListItemInfo.offset >= 0 }
  override val totalItemsCount: Int
    get() = lazyListState.layoutInfo.totalItemsCount
  override val viewportHeight: Int
    get() = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset

  override val layoutInfo: LazyListLayoutInfoWrapper = LazyListLayoutInfoWrapper(lazyListState)

  override suspend fun scrollToItem(index: Int, scrollOffset: Int) {
    lazyListState.scrollToItem(index, scrollOffset)
  }

}

@Stable
class LazyListLayoutInfoWrapper(
  val lazyListState: LazyListState
) : LazyLayoutInfoWrapper<LazyListItemInfoWrapper> {

  override val visibleItemsInfo: List<LazyListItemInfoWrapper>
    get() {
      return lazyListState.layoutInfo.visibleItemsInfo
        .map { lazyListItemInfo -> LazyItemInfoWrapper.fromLazyListItemInfo(lazyListItemInfo) }
    }
  override val viewportStartOffset: Int
    get() = lazyListState.layoutInfo.viewportStartOffset
  override val viewportEndOffset: Int
    get() = lazyListState.layoutInfo.viewportEndOffset
  override val totalItemsCount: Int
    get() = lazyListState.layoutInfo.totalItemsCount
  override val viewportSize: IntSize
    get() = lazyListState.layoutInfo.viewportSize
  override val orientation: Orientation
    get() = lazyListState.layoutInfo.orientation
  override val reverseLayout: Boolean
    get() = lazyListState.layoutInfo.reverseLayout
  override val beforeContentPadding: Int
    get() = lazyListState.layoutInfo.beforeContentPadding
  override val afterContentPadding: Int
    get() = lazyListState.layoutInfo.afterContentPadding
}

@Stable
class LazyListItemInfoWrapper(
  override val index: Int,
  override val key: Any,
  override val offsetY: Int,
  val offset: Int,
  val size: Int
) : LazyItemInfoWrapper