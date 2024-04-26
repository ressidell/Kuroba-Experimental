package com.github.k1rakishou.chan.ui.compose.lazylist.wrapper

import androidx.compose.runtime.Stable

@Stable
interface LazyStateWrapper<T : LazyItemInfoWrapper, V : LazyLayoutInfoWrapper<T>> {
  val isScrollInProgress: Boolean
  val canScrollBackward: Boolean
  val canScrollForward: Boolean
  val firstVisibleItemIndex: Int
  val firstVisibleItemScrollOffset: Int
  val visibleItemsCount: Int
  val fullyVisibleItemsCount: Int
  val totalItemsCount: Int
  val viewportHeight: Int
  val layoutInfo: V

  suspend fun scrollToItem(index: Int, scrollOffset: Int = 0)
}