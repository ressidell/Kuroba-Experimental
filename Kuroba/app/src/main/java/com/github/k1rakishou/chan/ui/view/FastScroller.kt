package com.github.k1rakishou.chan.ui.view

import android.R
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLayoutChangeListener
import androidx.annotation.VisibleForTesting
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.github.k1rakishou.ChanSettings
import com.github.k1rakishou.ChanSettings.FastScrollerType
import com.github.k1rakishou.chan.ui.globalstate.GlobalUiStateHolder
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils
import com.github.k1rakishou.core_themes.ChanTheme
import com.github.k1rakishou.core_themes.ThemeEngine
import com.github.k1rakishou.core_themes.ThemeEngine.ThemeChangesListener
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

/**
 * Class responsible to animate and provide a fast scroller.
 *
 *
 * Clover changed: the original FastScroller didn't account for the recyclerview top padding we
 * require. A minimum thumb length parameter was also added.
 */
class FastScroller(
  recyclerView: RecyclerView,
  postInfoMapItemDecoration: PostInfoMapItemDecoration?,
  defaultWidth: Int,
  scrollbarMinimumRange: Int,
  thumbMinLength: Int
) : RecyclerView.ItemDecoration(), RecyclerView.OnItemTouchListener, ThemeChangesListener, OnLayoutChangeListener {
  private var thumbDragListener: ThumbDragListener? = null

  // Final values for the vertical scroll bar
  private lateinit var mVerticalThumbDrawable: StateListDrawable
  private lateinit var realVerticalThumbDrawable: StateListDrawable
  private lateinit var mVerticalTrackDrawable: Drawable

  // Dynamic values for the vertical scroll bar
  private var verticalThumbHeight: Int = 0
  private var realVerticalThumbHeight: Int = 0
  private var mVerticalThumbCenterY: Int = 0
  private var mVerticalDragY: Float = 0f
  private var mVerticalDragThumbHeight: Int = 0

  private var mRecyclerViewWidth = 0
  private var mRecyclerViewHeight = 0

  private var mRecyclerViewLeftPadding = 0
  private var mRecyclerViewTopPadding = 0

  private var mRecyclerView: RecyclerView? = null

  /**
   * Whether the document is long/wide enough to require scrolling. If not, we don't show the
   * relevant scroller.
   */
  private var mNeedVerticalScrollbar = false
  private var mState = STATE_HIDDEN
  private var mDragState = DRAG_NONE
  private var mAnimationState = ANIMATION_STATE_OUT

  private val postInfoMapItemDecoration: PostInfoMapItemDecoration?
  private val mScrollbarMinimumRange: Int
  private val thumbMinLength: Int
  private val realThumbMinLength = AppModuleAndroidUtils.dp(1f)
  private val fastScrollerWidth: Int
  private val mVerticalRange = IntArray(2)
  private val mShowHideAnimator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f)
  private val mHideRunnable = Runnable { hide(HIDE_DURATION_MS) }

  private val mOnScrollListener: RecyclerView.OnScrollListener = object : RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
      updateScrollPosition(recyclerView.computeVerticalScrollOffset())
    }
  }

  @Inject
  lateinit var themeEngine: ThemeEngine
  @Inject
  lateinit var globalUiStateHolder: GlobalUiStateHolder

  init {
    AppModuleAndroidUtils.extractActivityComponent(recyclerView.context)
      .inject(this)

    this.mScrollbarMinimumRange = scrollbarMinimumRange
    this.fastScrollerWidth = defaultWidth
    this.thumbMinLength = thumbMinLength
    this.postInfoMapItemDecoration = postInfoMapItemDecoration

    onThemeChanged()

    mShowHideAnimator.addListener(AnimatorListener())
    mShowHideAnimator.addUpdateListener(AnimatorUpdater())

    attachToRecyclerView(recyclerView)
    themeEngine.addListener(this)
  }

  override fun onThemeChanged() {
    mVerticalThumbDrawable = getThumb(themeEngine.chanTheme)
    realVerticalThumbDrawable = getRealThumb(themeEngine.chanTheme)
    mVerticalTrackDrawable = getTrack(themeEngine.chanTheme)

    mVerticalThumbDrawable.alpha = SCROLLBAR_THUMB_ALPHA
    realVerticalThumbDrawable.alpha = SCROLLBAR_REAL_THUMB_ALPHA
    mVerticalTrackDrawable.alpha = trackAlpha

    requestRedraw()
  }

  private val trackAlpha: Int
    get() {
      if (mState == STATE_DRAGGING) {
        return SCROLLBAR_TRACK_ALPHA_DRAGGING
      }

      return SCROLLBAR_TRACK_ALPHA_VISIBLE
    }

  fun setThumbDragListener(listener: ThumbDragListener) {
    this.thumbDragListener = listener
  }

  fun attachToRecyclerView(recyclerView: RecyclerView) {
    if (mRecyclerView === recyclerView) {
      return  // nothing to do
    }

    if (mRecyclerView != null) {
      destroyCallbacks()
    }

    mRecyclerView = recyclerView
    setupCallbacks()
  }

  private fun setupCallbacks() {
    mRecyclerView?.addItemDecoration(this)
    mRecyclerView?.addOnItemTouchListener(this)
    mRecyclerView?.addOnScrollListener(mOnScrollListener)
    mRecyclerView?.addOnLayoutChangeListener(this)
  }

  fun onCleanup() {
    themeEngine.removeListener(this)
    thumbDragListener = null

    destroyCallbacks()
  }

  fun destroyCallbacks() {
    mRecyclerView!!.removeItemDecoration(this)
    mRecyclerView!!.removeOnItemTouchListener(this)
    mRecyclerView!!.removeOnScrollListener(mOnScrollListener)
    mRecyclerView!!.removeOnLayoutChangeListener(this)
    cancelHide()

    postInfoMapItemDecoration?.cancelAnimation()
  }

  private fun requestRedraw() {
    if (mRecyclerView != null) {
      mRecyclerView!!.invalidate()
    }
  }

  private fun setState(state: Int) {
    if (state == STATE_DRAGGING && mState != STATE_DRAGGING) {
      mVerticalThumbDrawable.setState(PRESSED_STATE_SET)
      mVerticalTrackDrawable.setState(PRESSED_STATE_SET)
      cancelHide()
    }

    if (state == STATE_HIDDEN) {
      requestRedraw()
    } else {
      show()
    }

    if (mState == STATE_DRAGGING && state == STATE_VISIBLE) {
      mVerticalThumbDrawable.setState(EMPTY_STATE_SET)
      mVerticalTrackDrawable.setState(HOVERED_STATE_SET)
      resetHideDelay(HIDE_DELAY_AFTER_DRAGGING_MS)
    } else if (mState == STATE_DRAGGING && state == STATE_HIDDEN) {
      mVerticalThumbDrawable.setState(EMPTY_STATE_SET)
      mVerticalTrackDrawable.setState(EMPTY_STATE_SET)
      resetHideDelay(HIDE_DELAY_AFTER_DRAGGING_MS)
    } else if (state == STATE_VISIBLE) {
      mVerticalTrackDrawable.setState(HOVERED_STATE_SET)
      resetHideDelay(HIDE_DELAY_AFTER_VISIBLE_MS)
    }

    mState = state
  }

  private val isLayoutRTL: Boolean
    get() {
      if (mRecyclerView == null) {
        return false
      }

      return ViewCompat.getLayoutDirection(mRecyclerView!!) == ViewCompat.LAYOUT_DIRECTION_RTL
    }

  val isDragging: Boolean
    get() = mState == STATE_DRAGGING

  @get:VisibleForTesting
  val isVisible: Boolean
    get() = mState == STATE_VISIBLE

  @get:VisibleForTesting
  val isHidden: Boolean
    get() = mState == STATE_HIDDEN

  fun show() {
    when (mAnimationState) {
      ANIMATION_STATE_FADING_OUT -> {
        mShowHideAnimator.cancel()

        postInfoMapItemDecoration?.cancelAnimation()
        mAnimationState = ANIMATION_STATE_FADING_IN
        mShowHideAnimator.setFloatValues(mShowHideAnimator.animatedValue as Float, 1f)
        mShowHideAnimator.setDuration(SHOW_DURATION_MS.toLong())
        mShowHideAnimator.startDelay = 0
        mShowHideAnimator.start()

        postInfoMapItemDecoration?.show()
      }

      ANIMATION_STATE_OUT -> {
        mAnimationState = ANIMATION_STATE_FADING_IN
        mShowHideAnimator.setFloatValues(mShowHideAnimator.animatedValue as Float, 1f)
        mShowHideAnimator.setDuration(SHOW_DURATION_MS.toLong())
        mShowHideAnimator.startDelay = 0
        mShowHideAnimator.start()

        postInfoMapItemDecoration?.show()
      }

      ANIMATION_STATE_FADING_IN, ANIMATION_STATE_IN -> {}
    }
  }

  fun hide() {
    hide(0)
  }

  @VisibleForTesting
  fun hide(duration: Int) {
    when (mAnimationState) {
      ANIMATION_STATE_FADING_IN -> {
        mShowHideAnimator.cancel()

        postInfoMapItemDecoration?.cancelAnimation()
        mAnimationState = ANIMATION_STATE_FADING_OUT
        mShowHideAnimator.setFloatValues(mShowHideAnimator.animatedValue as Float, 0f)
        mShowHideAnimator.setDuration(duration.toLong())
        mShowHideAnimator.start()

        postInfoMapItemDecoration?.hide(duration)
      }

      ANIMATION_STATE_IN -> {
        mAnimationState = ANIMATION_STATE_FADING_OUT
        mShowHideAnimator.setFloatValues(mShowHideAnimator.animatedValue as Float, 0f)
        mShowHideAnimator.setDuration(duration.toLong())
        mShowHideAnimator.start()

        postInfoMapItemDecoration?.hide(duration)
      }

      ANIMATION_STATE_FADING_OUT, ANIMATION_STATE_OUT -> {}
    }
  }

  private fun cancelHide() {
    mRecyclerView?.removeCallbacks(mHideRunnable)
  }

  private fun resetHideDelay(delay: Int) {
    cancelHide()
    mRecyclerView?.postDelayed(mHideRunnable, delay.toLong())
  }

  override fun onLayoutChange(
    v: View,
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
    oldLeft: Int,
    oldTop: Int,
    oldRight: Int,
    oldBottom: Int
  ) {
    if (mRecyclerView == null) {
      return
    }

    mRecyclerViewWidth = recyclerViewWidth
    mRecyclerViewHeight = recyclerViewHeight
    mRecyclerViewLeftPadding = mRecyclerView!!.paddingLeft
    mRecyclerViewTopPadding = mRecyclerView!!.paddingTop

    updateScrollPosition(mRecyclerView!!.computeVerticalScrollOffset())
    requestRedraw()
  }

  override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
    if (mRecyclerViewWidth != recyclerViewWidth || mRecyclerViewHeight != recyclerViewHeight) {
      mRecyclerViewWidth = recyclerViewWidth
      mRecyclerViewHeight = recyclerViewHeight
      mRecyclerViewLeftPadding = mRecyclerView!!.paddingLeft
      mRecyclerViewTopPadding = mRecyclerView!!.paddingTop

      // This is due to the different events ordering when keyboard is opened or
      // retracted vs rotate. Hence to avoid corner cases we just disable the
      // scroller when size changed, and wait until the scroll position is recomputed
      // before showing it back.
      setState(STATE_HIDDEN)
      return
    }

    if (mAnimationState != ANIMATION_STATE_OUT && mNeedVerticalScrollbar) {
      postInfoMapItemDecoration?.onDrawOver(canvas, mRecyclerView!!)

      val drawInnerThumb = (postInfoMapItemDecoration != null && !postInfoMapItemDecoration.isEmpty())
      drawVerticalScrollbar(canvas, drawInnerThumb)
    }
  }

  private val recyclerViewWidth: Int
    get() = mRecyclerView!!.width - mRecyclerView!!.paddingLeft - mRecyclerView!!.paddingRight

  private val recyclerViewHeight: Int
    get() = mRecyclerView!!.height - mRecyclerView!!.paddingTop - mRecyclerView!!.paddingBottom

  private fun drawVerticalScrollbar(canvas: Canvas, drawInnerThumb: Boolean) {
    val left = mRecyclerView!!.width - mRecyclerView!!.paddingLeft - fastScrollerWidth
    var top = mVerticalThumbCenterY - verticalThumbHeight / 2

    if (top < mRecyclerViewTopPadding) {
      top = mRecyclerViewTopPadding
    }

    if (top > mRecyclerViewHeight + verticalThumbHeight / 2) {
      top = mRecyclerViewHeight + verticalThumbHeight / 2
    }

    // Draw the draggable thumb. It uses may not always be of the same height as the real thumb
    // because we force it to a minimum height so that it's easy to drag it around.
    mVerticalThumbDrawable.setBounds(
      0,
      0,
      fastScrollerWidth,
      verticalThumbHeight
    )

    mVerticalTrackDrawable.setBounds(
      0,
      mRecyclerView!!.paddingTop,
      fastScrollerWidth,
      mRecyclerView!!.height - mRecyclerView!!.paddingBottom
    )

    if (isLayoutRTL) {
      mVerticalTrackDrawable.draw(canvas)
      canvas.translate(fastScrollerWidth.toFloat(), top.toFloat())
      canvas.scale(-1f, 1f)
      mVerticalThumbDrawable.draw(canvas)
      canvas.scale(1f, 1f)
      canvas.translate(-fastScrollerWidth.toFloat(), -top.toFloat())
    } else {
      canvas.translate(left.toFloat(), 0f)
      mVerticalTrackDrawable.draw(canvas)
      canvas.translate(0f, top.toFloat())
      mVerticalThumbDrawable.draw(canvas)
      canvas.translate(-left.toFloat(), -top.toFloat())
    }

    if (drawInnerThumb && (realVerticalThumbHeight * 4) < verticalThumbHeight) {
      // Draw the real thumb (with the real height). This one is useful in huge thread to see
      // where exactly a marked post is relative to it.
      val realTop = mVerticalThumbCenterY - (realVerticalThumbHeight / 2)
      realVerticalThumbDrawable.setBounds(0, 0, fastScrollerWidth, realVerticalThumbHeight)

      if (isLayoutRTL) {
        canvas.translate(fastScrollerWidth.toFloat(), realTop.toFloat())
        canvas.scale(-1f, 1f)
        realVerticalThumbDrawable.draw(canvas)
        canvas.scale(1f, 1f)
        canvas.translate(-fastScrollerWidth.toFloat(), -realTop.toFloat())
      } else {
        canvas.translate(left.toFloat(), 0f)
        canvas.translate(0f, realTop.toFloat())
        realVerticalThumbDrawable.draw(canvas)
        canvas.translate(-left.toFloat(), -realTop.toFloat())
      }
    }
  }

  /**
   * Notify the scroller of external change of the scroll, e.g. through dragging or flinging on
   * the view itself.
   *
   * @param offsetY The new scroll Y offset.
   */
  fun updateScrollPosition(offsetY: Int) {
    val verticalContentLength = mRecyclerView!!.computeVerticalScrollRange()
    val verticalVisibleLength = mRecyclerViewHeight

    mNeedVerticalScrollbar = (verticalContentLength - verticalVisibleLength > 0
      && mRecyclerViewHeight >= mScrollbarMinimumRange)

    if (mNeedVerticalScrollbar) {
      val middleScreenPos = offsetY + verticalVisibleLength / 2.0f
      mVerticalThumbCenterY =
        mRecyclerViewTopPadding + ((verticalVisibleLength * middleScreenPos) / verticalContentLength).toInt()

      val length = min(
        verticalVisibleLength.toDouble(),
        ((verticalVisibleLength * verticalVisibleLength) / verticalContentLength).toDouble()
      ).toInt()

      verticalThumbHeight = max(thumbMinLength.toDouble(), length.toDouble()).toInt()
      realVerticalThumbHeight = max(realThumbMinLength.toDouble(), length.toDouble()).toInt()
    }

    if (mState == STATE_HIDDEN || mState == STATE_VISIBLE) {
      setState(STATE_VISIBLE)
    }
  }

  override fun onInterceptTouchEvent(recyclerView: RecyclerView, ev: MotionEvent): Boolean {
    val handled: Boolean
    if (mState == STATE_VISIBLE) {
      val insideVerticalThumb = isPointInsideVerticalThumb(ev.x, ev.y)
      if (ev.action == MotionEvent.ACTION_DOWN && insideVerticalThumb) {
        mDragState = DRAG_Y
        mVerticalDragY = ev.y.toInt().toFloat()
        mVerticalDragThumbHeight = verticalThumbHeight

        setState(STATE_DRAGGING)
        handled = true
      } else {
        handled = false
      }
    } else {
      handled = mState == STATE_DRAGGING
    }
    return handled
  }

  override fun onTouchEvent(recyclerView: RecyclerView, me: MotionEvent) {
    if (mState == STATE_HIDDEN) {
      globalUiStateHolder.updateFastScrollerState {
        updateIsDraggingFastScroller(false)
      }

      return
    }

    if (me.action == MotionEvent.ACTION_DOWN) {
      val insideVerticalThumb = isPointInsideVerticalThumb(me.x, me.y)
      if (insideVerticalThumb) {
        mDragState = DRAG_Y
        mVerticalDragY = me.y.toInt().toFloat()
        mVerticalDragThumbHeight = verticalThumbHeight

        setState(STATE_DRAGGING)
        requestRedraw()

        if (thumbDragListener != null) {
          thumbDragListener!!.onDragStarted()
        }

        globalUiStateHolder.updateFastScrollerState {
          updateIsDraggingFastScroller(true)
        }

        verticalScrollTo(me.y)
      }
    } else if ((me.action == MotionEvent.ACTION_UP || me.action == MotionEvent.ACTION_CANCEL) && mState == STATE_DRAGGING) {
      mVerticalDragY = 0f
      setState(STATE_VISIBLE)
      mDragState = DRAG_NONE
      requestRedraw()

      if (thumbDragListener != null) {
        thumbDragListener!!.onDragEnded()
      }

      globalUiStateHolder.updateFastScrollerState {
        updateIsDraggingFastScroller(false)
      }
    } else if (me.action == MotionEvent.ACTION_MOVE && mState == STATE_DRAGGING) {
      show()
      if (mDragState == DRAG_Y) {
        verticalScrollTo(me.y)
      }
    }
  }

  override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
  }

  private fun verticalScrollTo(y: Float) {
    val scrollbarRange = verticalRange
    val touchFraction = calculateTouchFraction(y, scrollbarRange)

    var scrollPosition = ((mRecyclerView!!.adapter!!.itemCount - 1) * touchFraction).toInt()
    if (scrollPosition < 0) {
      scrollPosition = 0
    }

    val layoutManager = mRecyclerView!!.layoutManager

    if (layoutManager is GridLayoutManager) {
      layoutManager.scrollToPositionWithOffset(scrollPosition, 0)
    } else if (layoutManager is LinearLayoutManager) {
      layoutManager.scrollToPositionWithOffset(scrollPosition, 0)
    } else if (layoutManager is StaggeredGridLayoutManager) {
      layoutManager.scrollToPositionWithOffset(scrollPosition, 0)
    }
  }

  private fun calculateTouchFraction(y: Float, scrollbarRange: IntArray): Float {
    if (scrollbarRange[0] > scrollbarRange[1]) {
      val tmp = scrollbarRange[1]
      scrollbarRange[1] = scrollbarRange[0]
      scrollbarRange[0] = tmp
    }

    if (y < scrollbarRange[0]) {
      return 0f
    }

    if (y > scrollbarRange[1]) {
      return 1f
    }

    val scrollbarHeight = (scrollbarRange[1] - scrollbarRange[0]).toFloat()
    val convertedY = y - (scrollbarRange[0].toFloat())

    return convertedY / scrollbarHeight
  }

  @VisibleForTesting
  fun isPointInsideVerticalThumb(x: Float, y: Float): Boolean {
    val fastScrollerType = ChanSettings.draggableScrollbars.get()
    if (fastScrollerType == FastScrollerType.Disabled) {
      // Can't use fast scroller for scrolling
      return false
    }

    if (fastScrollerType != FastScrollerType.ScrollByClickingAnyPointOfTrack) {
      // Can only scroll when the touch is inside the scrollbar's thumb
      return ((if (isLayoutRTL) x <= mRecyclerViewLeftPadding + fastScrollerWidth / 2f
      else x >= mRecyclerViewLeftPadding + mRecyclerViewWidth - fastScrollerWidth)
        && (y >= mVerticalThumbCenterY - verticalThumbHeight / 2f - fastScrollerWidth
        ) && (y <= mVerticalThumbCenterY + verticalThumbHeight / 2f + fastScrollerWidth))
    }

    // Can scroll when clicking any point of the scrollbar's track
    return if (isLayoutRTL
    ) x <= mRecyclerViewLeftPadding + fastScrollerWidth / 2.0f
    else x >= mRecyclerViewLeftPadding + mRecyclerViewWidth - fastScrollerWidth
  }

  private val verticalRange: IntArray
    /**
     * Gets the (min, max) vertical positions of the vertical scroll bar.
     */
    get() {
      mVerticalRange[0] = mRecyclerViewTopPadding
      mVerticalRange[1] = mRecyclerViewTopPadding + mRecyclerViewHeight
      return mVerticalRange
    }

  private inner class AnimatorListener

    : AnimatorListenerAdapter() {
    private var mCanceled = false

    override fun onAnimationEnd(animation: Animator) {
      // Cancel is always followed by a new directive, so don't update state.
      if (mCanceled) {
        mCanceled = false
        return
      }
      if (mShowHideAnimator.animatedValue as Float == 0f) {
        mAnimationState = ANIMATION_STATE_OUT
        setState(STATE_HIDDEN)
      } else {
        mAnimationState = ANIMATION_STATE_IN
        requestRedraw()
      }
    }

    override fun onAnimationCancel(animation: Animator) {
      mCanceled = true
    }
  }

  private inner class AnimatorUpdater : AnimatorUpdateListener {
    override fun onAnimationUpdate(valueAnimator: ValueAnimator) {
      val thumbAlpha = (SCROLLBAR_THUMB_ALPHA * (valueAnimator.animatedValue as Float)).toInt()
      val realThumbAlpha = (SCROLLBAR_REAL_THUMB_ALPHA * (valueAnimator.animatedValue as Float)).toInt()
      val trackAlpha: Int = (trackAlpha * (valueAnimator.animatedValue as Float)).toInt()

      mVerticalThumbDrawable.alpha = thumbAlpha
      realVerticalThumbDrawable.alpha = realThumbAlpha
      mVerticalTrackDrawable.alpha = trackAlpha
      requestRedraw()
    }
  }

  interface ThumbDragListener {
    fun onDragStarted()
    fun onDragEnded()
  }

  companion object {
    // Scroll thumb not showing
    private const val STATE_HIDDEN = 0

    // Scroll thumb visible and moving along with the scrollbar
    private const val STATE_VISIBLE = 1

    // Scroll thumb being dragged by user
    private const val STATE_DRAGGING = 2

    private const val DRAG_NONE = 0
    private const val DRAG_X = 1
    private const val DRAG_Y = 2

    private const val ANIMATION_STATE_OUT = 0
    private const val ANIMATION_STATE_FADING_IN = 1
    private const val ANIMATION_STATE_IN = 2
    private const val ANIMATION_STATE_FADING_OUT = 3

    private const val SHOW_DURATION_MS = 300
    private const val HIDE_DELAY_AFTER_VISIBLE_MS = 1500
    private const val HIDE_DELAY_AFTER_DRAGGING_MS = 1200
    private const val HIDE_DURATION_MS = 300
    private const val SCROLLBAR_THUMB_ALPHA = 150
    private const val SCROLLBAR_REAL_THUMB_ALPHA = 200
    private const val SCROLLBAR_TRACK_ALPHA_DRAGGING = 150
    private const val SCROLLBAR_TRACK_ALPHA_VISIBLE = 80

    private val PRESSED_STATE_SET = intArrayOf(R.attr.state_pressed)
    private val HOVERED_STATE_SET = intArrayOf(R.attr.state_hovered)
    private val EMPTY_STATE_SET = intArrayOf()

    private fun getRealThumb(curTheme: ChanTheme): StateListDrawable {
      val list = StateListDrawable()
      list.addState(intArrayOf(), ColorDrawable(curTheme.textColorSecondary))
      return list
    }

    private fun getThumb(curTheme: ChanTheme): StateListDrawable {
      val list = StateListDrawable()
      list.addState(intArrayOf(R.attr.state_pressed), ColorDrawable(curTheme.accentColor))
      list.addState(intArrayOf(), ColorDrawable(curTheme.textColorSecondary))
      return list
    }

    private fun getTrack(curTheme: ChanTheme): StateListDrawable {
      val list = StateListDrawable()
      list.addState(intArrayOf(R.attr.state_pressed), ColorDrawable(curTheme.textColorHint))
      list.addState(intArrayOf(R.attr.state_hovered), ColorDrawable(curTheme.textColorHint))
      list.addState(intArrayOf(), ColorDrawable(0))
      return list
    }
  }

}
