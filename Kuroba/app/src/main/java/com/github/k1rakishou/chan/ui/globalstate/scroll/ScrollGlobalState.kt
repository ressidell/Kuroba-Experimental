package com.github.k1rakishou.chan.ui.globalstate.scroll

import androidx.compose.runtime.FloatState
import androidx.compose.runtime.mutableFloatStateOf
import com.github.k1rakishou.common.quantize
import kotlin.math.absoluteValue

interface IScrollGlobalState {
  interface Readable {
    val scrollTransitionProgress: FloatState
    val currentScrollTransitionProgress: Float
  }

  interface Writable {
    fun onScrollChanged(scrollDelta: Int)
  }
}

class ScrollGlobalState : IScrollGlobalState.Readable, IScrollGlobalState.Writable {
  private val _scrollTransitionProgress = ScreenScrollState()

  override val scrollTransitionProgress: FloatState
    get() = _scrollTransitionProgress.progress
  override val currentScrollTransitionProgress: Float
    get() = _scrollTransitionProgress.progress.floatValue

  override fun onScrollChanged(scrollDelta: Int) {
    _scrollTransitionProgress.onScrollChanged(scrollDelta)
  }

  class ScreenScrollState {
    private var scrollDeltaAccumulator = 0

    private val _progress = mutableFloatStateOf(1f)
    val progress: FloatState
      get() = _progress

    fun onScrollChanged(scrollDelta: Int) {
      scrollDeltaAccumulator += scrollDelta
      scrollDeltaAccumulator = scrollDeltaAccumulator.coerceIn(-MaxScrollDeltaAccumulated, MaxScrollDeltaAccumulated)

      val progress = ((scrollDeltaAccumulator.absoluteValue * 2).toFloat() / (MaxScrollDeltaAccumulated * 2f))
        .quantize(0.033f)

      _progress.floatValue = progress
    }

    companion object {
      private const val MaxScrollDeltaAccumulated = 256
    }

  }

}