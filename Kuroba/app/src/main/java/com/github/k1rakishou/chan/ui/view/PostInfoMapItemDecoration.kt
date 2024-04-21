package com.github.k1rakishou.chan.ui.view

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.core.usecase.PostMapInfoEntry
import com.github.k1rakishou.chan.core.usecase.PostMapInfoHolder

class PostInfoMapItemDecoration(
  private val context: Context
) {
  private var postInfoHolder = PostMapInfoHolder()

  fun isEmpty(): Boolean = postInfoHolder.isEmpty()

  fun setItems(
    newPostMapInfoHolder: PostMapInfoHolder
  ) {
    if (postInfoHolder.isTheSame(newPostMapInfoHolder)) {
      return
    }

    postInfoHolder = newPostMapInfoHolder
  }

  fun hide() {
    postInfoHolder = PostMapInfoHolder()
  }

  fun draw(
    contentDrawScope: ContentDrawScope,
    scrollbarMarkWidth: Float,
    recyclerTopPadding: Float,
    recyclerBottomPadding: Float,
    recyclerView: RecyclerView,
    alpha: Float
  ) {
    if (postInfoHolder.isEmpty()) {
      return
    }

    with(contentDrawScope) {
      drawRanges(
        scrollbarMarkWidth = scrollbarMarkWidth,
        recyclerTopPadding = recyclerTopPadding,
        recyclerBottomPadding = recyclerBottomPadding,
        recyclerView = recyclerView,
        postMapInfoEntries = postInfoHolder.myPostsPositionRanges,
        color = Color(context.resources.getColor(R.color.my_post_color)),
        alpha = alpha
      )

      drawRanges(
        scrollbarMarkWidth = scrollbarMarkWidth,
        recyclerTopPadding = recyclerTopPadding,
        recyclerBottomPadding = recyclerBottomPadding,
        recyclerView = recyclerView,
        postMapInfoEntries = postInfoHolder.replyPositionRanges,
        color = Color(context.resources.getColor(R.color.reply_post_color)),
        alpha = alpha
      )

      drawRanges(
        scrollbarMarkWidth = scrollbarMarkWidth,
        recyclerTopPadding = recyclerTopPadding,
        recyclerBottomPadding = recyclerBottomPadding,
        recyclerView = recyclerView,
        postMapInfoEntries = postInfoHolder.crossThreadQuotePositionRanges,
        color = Color(context.resources.getColor(R.color.cross_thread_reply_post_color)),
        alpha = alpha
      )

      drawRanges(
        scrollbarMarkWidth = scrollbarMarkWidth,
        recyclerTopPadding = recyclerTopPadding,
        recyclerBottomPadding = recyclerBottomPadding,
        recyclerView = recyclerView,
        postMapInfoEntries = postInfoHolder.postFilterHighlightRanges,
        color = Color(context.resources.getColor(R.color.cross_thread_reply_post_color)),
        alpha = alpha
      )

      drawRanges(
        scrollbarMarkWidth = scrollbarMarkWidth,
        recyclerTopPadding = recyclerTopPadding,
        recyclerBottomPadding = recyclerBottomPadding,
        recyclerView = recyclerView,
        postMapInfoEntries = postInfoHolder.deletedPostsPositionRanges,
        color = Color(context.resources.getColor(R.color.deleted_post_color)),
        alpha = alpha
      )

      drawRanges(
        scrollbarMarkWidth = scrollbarMarkWidth,
        recyclerTopPadding = recyclerTopPadding,
        recyclerBottomPadding = recyclerBottomPadding,
        recyclerView = recyclerView,
        postMapInfoEntries = postInfoHolder.hotPostsPositionRanges,
        color = Color(context.resources.getColor(R.color.hot_post_color)),
        alpha = alpha
      )

      drawRanges(
        scrollbarMarkWidth = scrollbarMarkWidth,
        recyclerTopPadding = recyclerTopPadding,
        recyclerBottomPadding = recyclerBottomPadding,
        recyclerView = recyclerView,
        postMapInfoEntries = postInfoHolder.thirdEyePostsPositionRanges,
        color = Color(context.resources.getColor(R.color.third_eye_post_color)),
        alpha = alpha
      )
    }
  }

  private fun ContentDrawScope.drawRanges(
    scrollbarMarkWidth: Float,
    recyclerTopPadding: Float,
    recyclerBottomPadding: Float,
    recyclerView: RecyclerView,
    postMapInfoEntries: List<PostMapInfoEntry>,
    color: Color,
    alpha: Float
  ) {
    val recyclerViewHeight = recyclerView.height

    val totalItemsCount = recyclerView.layoutManager
      ?.itemCount
      ?.takeIf { itemCount -> itemCount > 0 }
      ?: return

    val onePostHeightRaw = recyclerView.computeVerticalScrollRange() / totalItemsCount
    val recyclerHeight = (recyclerViewHeight.toFloat() - (recyclerTopPadding + recyclerBottomPadding))
    val unit = ((recyclerHeight / recyclerView.computeVerticalScrollRange()) * onePostHeightRaw)
    val halfUnit = unit / 2f

    translate(top = recyclerTopPadding + halfUnit) {
      postMapInfoEntries.forEach { postMapInfoEntry ->
        val positionRange = postMapInfoEntry.range
        val postMapInfoEntryColor = postMapInfoEntry.color
        val startPosition = positionRange.first
        val scrollbarTopY = startPosition * unit - halfUnit

        val finalColor = if (postMapInfoEntryColor != 0) {
          Color(postMapInfoEntryColor)
        } else {
          color
        }

        val scrollbarMarkHeight = unit.coerceAtLeast(1.dp.toPx())

        drawRect(
          color = finalColor,
          topLeft = Offset(
            x = 0f,
            y = scrollbarTopY
          ),
          size = Size(
            width = scrollbarMarkWidth,
            height = scrollbarMarkHeight
          ),
          alpha = alpha
        )
      }
    }
  }
}