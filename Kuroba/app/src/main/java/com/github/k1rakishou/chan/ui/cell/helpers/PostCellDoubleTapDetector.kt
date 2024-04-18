package com.github.k1rakishou.chan.ui.cell.helpers

import android.view.GestureDetector
import android.view.MotionEvent
import com.github.k1rakishou.chan.ui.view.PostCommentTextView

class PostCellDoubleTapDetector(
  private val commentMovementMethod: PostViewMovementMethod,
  private val commentFunc: () -> PostCommentTextView,
  private val performRequestParentDisallowInterceptTouchEvents: (Boolean) -> Unit
) : GestureDetector.SimpleOnGestureListener() {
  override fun onDoubleTap(e: MotionEvent): Boolean {
    val comment = commentFunc()

    val touchOverlapsAnyClickableSpan = commentMovementMethod.touchOverlapsAnyClickableSpan(comment, e)
    if (touchOverlapsAnyClickableSpan) {
      return true
    }

    comment.startSelectionMode(e.x, e.y)
    performRequestParentDisallowInterceptTouchEvents(true)

    return true
  }
}