package com.github.k1rakishou.chan.ui.cell.helpers

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import com.github.k1rakishou.chan.ui.view.PostCommentTextView

class PostCommentLongtapDetector(
  private val context: Context,
  private val commentFunc: () -> PostCommentTextView
) {
  private val scaledTouchSlop = ViewConfiguration.get(context).scaledTouchSlop

  private var blocking = false
  private var upOrCancelSent = false
  private var initialTouchEvent: MotionEvent? = null

  var postCellContainer: ViewGroup? = null
  var commentView: View? = null

  fun passTouchEvent(event: MotionEvent) {
    if (event.pointerCount != 1) {
      return
    }

    val action = event.actionMasked
    val blockedByFlags = blocking || upOrCancelSent || initialTouchEvent != null

    if ((action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL) && blockedByFlags) {
      return
    }

    val comment = commentFunc()

    when (action) {
      MotionEvent.ACTION_DOWN -> {
        val postCommentMovementMethod = comment.movementMethod as? PostViewMovementMethod

        if (postCommentMovementMethod != null
          && postCommentMovementMethod.touchOverlapsAnyClickableSpan(comment, event)) {
          blocking = true
          sendUpOrCancel(event)
          return
        }

        initialTouchEvent = MotionEvent.obtain(event)

        modifyEventPosition(event) { updatedEvent ->
          postCellContainer?.onTouchEvent(updatedEvent)
        }
      }
      MotionEvent.ACTION_MOVE -> {
        if (initialTouchEvent == null) {
          blocking = true
          sendUpOrCancel(event)
          return
        }

        val deltaX = Math.abs(event.x - initialTouchEvent!!.x)
        val deltaY = Math.abs(event.y - initialTouchEvent!!.y)

        if (deltaX > scaledTouchSlop || deltaY > scaledTouchSlop) {
          blocking = true
          sendUpOrCancel(event)
          return
        }
      }
      MotionEvent.ACTION_UP,
      MotionEvent.ACTION_CANCEL -> {
        sendUpOrCancel(event)

        blocking = false
        upOrCancelSent = false

        initialTouchEvent?.recycle()
        initialTouchEvent = null
      }
    }
  }

  private fun sendUpOrCancel(event: MotionEvent) {
    if (upOrCancelSent) {
      return
    }

    upOrCancelSent = true

    val action = if (blocking || event.actionMasked == MotionEvent.ACTION_CANCEL) {
      MotionEvent.ACTION_CANCEL
    } else {
      MotionEvent.ACTION_UP
    }

    val motionEvent = MotionEvent.obtain(
      SystemClock.uptimeMillis(),
      SystemClock.uptimeMillis(),
      action,
      event.x,
      event.y,
      event.metaState
    )

    modifyEventPosition(motionEvent) { updatedEvent ->
      postCellContainer?.onTouchEvent(updatedEvent)
    }

    motionEvent.recycle()
  }

  private fun modifyEventPosition(inputEvent: MotionEvent, applier: (MotionEvent) -> Unit) {
    val deltaX = (commentView!!.left - postCellContainer!!.left).coerceAtLeast(0)
    val deltaY = (commentView!!.top - postCellContainer!!.top).coerceAtLeast(0)

    val event = MotionEvent.obtain(
      inputEvent.downTime,
      inputEvent.eventTime,
      inputEvent.action,
      inputEvent.x + deltaX,
      inputEvent.y + deltaY,
      inputEvent.metaState
    )

    applier(event)
    event.recycle()
  }

}