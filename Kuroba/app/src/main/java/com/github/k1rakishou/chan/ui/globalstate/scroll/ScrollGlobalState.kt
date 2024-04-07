package com.github.k1rakishou.chan.ui.globalstate.scroll

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.FloatState
import androidx.recyclerview.widget.RecyclerView
import com.github.k1rakishou.chan.ui.helper.AppResources
import com.github.k1rakishou.core_logger.Logger

interface IScrollGlobalState {
  interface Readable {
    val scrollTransitionProgress: FloatState
    val currentScrollTransitionProgress: Float
  }

  interface Writeable {
    fun attachToRecyclerView(recyclerView: RecyclerView)
    fun detachFromRecyclerView(recyclerView: RecyclerView)

    fun attachToLazyList(lazyListState: LazyListState)
    fun attachToLazyGrid(lazyGridState: LazyGridState)
    fun attachToLazyStaggeredGrid(lazyStaggeredGridState: LazyStaggeredGridState)
    fun detachFromLazyList()

    fun resetScrollState()
  }
}

class ScrollGlobalState(
  private val appResources: AppResources
) : IScrollGlobalState.Readable, IScrollGlobalState.Writeable {
  private val _scrollTransitionProgress = IndividualScreenScrollState(appResources)

  private val _recyclerViewScrollHandler by lazy(LazyThreadSafetyMode.NONE) {
    RecyclerViewScrollHandler(
      onScrollChanged = { delta -> _scrollTransitionProgress.onScrollChanged(delta) },
      onScrollSettled = { _scrollTransitionProgress.onScrollSettled() }
    )
  }

  override val scrollTransitionProgress: FloatState
    get() = _scrollTransitionProgress.progress
  override val currentScrollTransitionProgress: Float
    get() = _scrollTransitionProgress.progress.floatValue

  override fun attachToRecyclerView(recyclerView: RecyclerView) {
    Logger.verbose(TAG) { "attachToRecyclerView()" }
    _recyclerViewScrollHandler.attachToRecyclerView(recyclerView)
  }

  override fun detachFromRecyclerView(recyclerView: RecyclerView) {
    Logger.verbose(TAG) { "detachFromRecyclerView()" }
    _recyclerViewScrollHandler.detachFromRecyclerView(recyclerView)
  }

  override fun attachToLazyList(lazyListState: LazyListState) {
    // TODO: New toolbar.
    Logger.verbose(TAG) { "attachToLazyList()" }
  }

  override fun attachToLazyGrid(lazyGridState: LazyGridState) {
    // TODO: New toolbar.
    Logger.verbose(TAG) { "lazyGridState()" }
  }

  override fun attachToLazyStaggeredGrid(lazyStaggeredGridState: LazyStaggeredGridState) {
    // TODO: New toolbar.
    Logger.verbose(TAG) { "attachToLazyStaggeredGrid()" }
  }

  override fun detachFromLazyList() {
    // TODO: New toolbar.
    Logger.verbose(TAG) { "detachFromLazyList()" }
  }

  override fun resetScrollState() {
    Logger.verbose(TAG) { "resetScrollState()" }
    _scrollTransitionProgress.reset()
  }

  companion object {
    private const val TAG = "ScrollGlobalState"
  }

}