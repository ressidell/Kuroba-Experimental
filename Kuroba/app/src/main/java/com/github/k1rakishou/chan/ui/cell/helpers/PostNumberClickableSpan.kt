package com.github.k1rakishou.chan.ui.cell.helpers

import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import com.github.k1rakishou.chan.ui.cell.PostCellInterface
import com.github.k1rakishou.model.data.post.ChanPost

class PostNumberClickableSpan(
  private val postCellCallback: PostCellInterface.PostCellCallback?,
  private val post: ChanPost?
) : ClickableSpan() {

  override fun onClick(widget: View) {
    post?.let { post ->
      postCellCallback?.onPostNoClicked(post)
    }
  }

  override fun updateDrawState(ds: TextPaint) {
    ds.isUnderlineText = false
  }

}