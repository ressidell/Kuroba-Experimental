package com.github.k1rakishou.chan.ui.globalstate.scroll

import androidx.recyclerview.widget.RecyclerView

class RecyclerViewScrollHandler(
  private val onScrollChanged: (Int) -> Unit,
  private val onScrollSettled: () -> Unit
) {
  private var _enabled = false
  private var _recyclerViewWasScrolling = false

  private val onScrollListener = object : RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
      if (_enabled) {
        onScrollChanged(dy)
      }
    }

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
      if (_enabled) {
        if (_recyclerViewWasScrolling && newState == RecyclerView.SCROLL_STATE_IDLE) {
          _recyclerViewWasScrolling = false
          onScrollSettled()
        } else {
          _recyclerViewWasScrolling = true
        }
      }
    }
  }

  fun attachToRecyclerView(recyclerView: RecyclerView) {
    detachFromRecyclerView(recyclerView)
    recyclerView.addOnScrollListener(onScrollListener)

    _enabled = true
  }

  fun detachFromRecyclerView(recyclerView: RecyclerView) {
    _enabled = false

    recyclerView.removeOnScrollListener(onScrollListener)
  }

}