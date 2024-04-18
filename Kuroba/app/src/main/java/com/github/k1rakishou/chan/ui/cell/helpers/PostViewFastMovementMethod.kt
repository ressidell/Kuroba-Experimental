package com.github.k1rakishou.chan.ui.cell.helpers

import android.text.Layout
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.MotionEvent
import android.widget.TextView
import com.github.k1rakishou.core_spannable.PostLinkable

/**
 * A MovementMethod that searches for PostLinkables.<br></br>
 * See [PostLinkable] for more information.
 */
class PostViewFastMovementMethod(
  private val postCommentLongtapDetector: PostCommentLongtapDetector
) : LinkMovementMethod() {
  private var intercept = false

  override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
    val action = event.actionMasked

    var x = event.x.toInt()
    var y = event.y.toInt()

    x -= widget.paddingLeft
    y -= widget.paddingTop
    x += widget.scrollX
    y += widget.scrollY

    val layout: Layout = widget.layout
    val line = layout.getLineForVertical(y)
    val off = layout.getOffsetForHorizontal(line, x.toFloat())
    val link = buffer.getSpans(off, off, ClickableSpan::class.java)
    val clickIsExactlyWithinBounds = (x >= layout.getLineLeft(line)) && (x < layout.getLineRight(line))
    val clickingSpans = link.isNotEmpty() && clickIsExactlyWithinBounds

    if (!intercept && action == MotionEvent.ACTION_UP && clickingSpans) {
      link[0].onClick(widget)
      return true
    }

    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
      intercept = false
    }

    if (!clickingSpans) {
      intercept = true
      postCommentLongtapDetector.passTouchEvent(event)
      return true
    }

    return false
  }

}