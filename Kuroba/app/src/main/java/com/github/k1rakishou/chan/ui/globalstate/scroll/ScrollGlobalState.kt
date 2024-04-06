package com.github.k1rakishou.chan.ui.globalstate.scroll

import android.animation.ObjectAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.unit.dp
import androidx.core.animation.addListener
import androidx.recyclerview.widget.RecyclerView
import com.github.k1rakishou.chan.core.base.KurobaCoroutineScope
import com.github.k1rakishou.chan.ui.helper.AppResources
import com.github.k1rakishou.common.quantize
import com.github.k1rakishou.common.resumeValueSafe
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

interface IScrollGlobalState {
  interface Readable {
    val scrollTransitionProgress: FloatState
    val currentScrollTransitionProgress: Float
  }

  interface Writable {
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
) : IScrollGlobalState.Readable, IScrollGlobalState.Writable {
  private val _scrollTransitionProgress = ScreenScrollState(appResources)

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
    _recyclerViewScrollHandler.attachToRecyclerView(recyclerView)
  }

  override fun detachFromRecyclerView(recyclerView: RecyclerView) {
    _recyclerViewScrollHandler.detachFromRecyclerView(recyclerView)
  }

  override fun attachToLazyList(lazyListState: LazyListState) {
    // TODO: New toolbar.
  }

  override fun attachToLazyGrid(lazyGridState: LazyGridState) {
    // TODO: New toolbar.
  }

  override fun attachToLazyStaggeredGrid(lazyStaggeredGridState: LazyStaggeredGridState) {
    // TODO: New toolbar.
  }

  override fun detachFromLazyList() {
    // TODO: New toolbar.
  }

  override fun resetScrollState() {
    _scrollTransitionProgress.reset()
  }

  class ScreenScrollState(
    private val appResources: AppResources
  ) {
    private val fps = 1f / 30f
    private val coroutineScope = KurobaCoroutineScope()
    private val interpolator = AccelerateDecelerateInterpolator()
    private val maxScrollDeltaAccumulated = with(appResources.composeDensity) { 48.dp.roundToPx() }
    private val toolbarShownAccumulatedDelta = -maxScrollDeltaAccumulated
    private val toolbarHiddenAccumulatedDelta = maxScrollDeltaAccumulated

    private var _scrollDeltaAccumulator = toolbarShownAccumulatedDelta
    private var _settleAnimationJob: Job? = null

    private val _progress = mutableFloatStateOf(1f)
    val progress: FloatState
      get() = _progress

    fun onScrollChanged(scrollDelta: Int) {
      _settleAnimationJob?.cancel()
      _settleAnimationJob = null

      _scrollDeltaAccumulator += scrollDelta
      _scrollDeltaAccumulator = _scrollDeltaAccumulator.coerceIn(-maxScrollDeltaAccumulated, maxScrollDeltaAccumulated)

      val progress = 1f - ((_scrollDeltaAccumulator + maxScrollDeltaAccumulated).toFloat() / (maxScrollDeltaAccumulated * 2f))
        .quantize(fps)

      _progress.floatValue = progress
    }

    fun onScrollSettled() {
      _settleAnimationJob?.cancel()
      _settleAnimationJob = coroutineScope.launch {
        val initialValue = _progress.floatValue
        val animateHide = _progress.floatValue < 0.5f
        val targetValue = if (animateHide) 0f else 1f

        try {
          animateFloat(
            from = initialValue,
            to = targetValue,
            block = { animatedValue -> _progress.floatValue = animatedValue }
          )

        } catch (error: Throwable) {
          throw error
        } finally {
          if (animateHide) {
            _scrollDeltaAccumulator = toolbarHiddenAccumulatedDelta
          } else {
            _scrollDeltaAccumulator = toolbarShownAccumulatedDelta
          }

          _progress.floatValue = targetValue
          _settleAnimationJob = null
        }
      }
    }

    fun reset() {
      _scrollDeltaAccumulator = toolbarShownAccumulatedDelta
      _progress.floatValue = 1f

      _settleAnimationJob?.cancel()
      _settleAnimationJob = null
    }

    private suspend fun animateFloat(
      from: Float,
      to: Float,
      block: (Float) -> Unit
    ) {
      if (from == to) {
        block(to)
        return
      }

      suspendCancellableCoroutine<Unit> { continuation ->
        val animator = ObjectAnimator.ofFloat(from, to)
        animator.duration = 250
        animator.interpolator = interpolator
        animator.addUpdateListener { valueAnimator ->
          block((valueAnimator.animatedValue as Float).quantize(fps))
        }
        animator.addListener(
          onCancel = { continuation.resumeValueSafe(Unit) },
          onEnd = { continuation.resumeValueSafe(Unit) }
        )

        continuation.invokeOnCancellation {
          if (animator.isRunning) {
            animator.end()
          }
        }

        animator.start()
      }
    }

  }

}