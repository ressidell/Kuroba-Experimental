/*
 * KurobaEx - *chan browser https://github.com/K1rakishou/Kuroba-Experimental/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.k1rakishou.chan.ui.adapter

import android.text.TextUtils
import com.github.k1rakishou.chan.core.helper.PostHideHelper
import com.github.k1rakishou.core_logger.Logger
import com.github.k1rakishou.model.data.descriptor.ChanDescriptor
import com.github.k1rakishou.model.data.post.ChanOriginalPost
import com.github.k1rakishou.model.data.post.ChanPost
import com.github.k1rakishou.model.data.post.PostIndexed
import java.util.*

class PostsFilter(
  private val postHideHelper: PostHideHelper,
  private val order: Order,
  private val query: String?
) {

  suspend fun applyFilter(chanDescriptor: ChanDescriptor, posts: MutableList<ChanPost>): List<PostIndexed> {
    val postsCount = posts.size

    if (order != Order.BUMP && chanDescriptor is ChanDescriptor.CatalogDescriptor) {
      processOrder(posts as MutableList<ChanOriginalPost>)
    }

    if (!TextUtils.isEmpty(query)) {
      processSearch(posts)
    }

    // Process hidden by filter and post/thread hiding
    val retainedPosts: List<ChanPost> = postHideHelper.filterHiddenPosts(posts)
      .safeUnwrap { error ->
        Logger.e(TAG, "postHideHelper.filterHiddenPosts error", error)
        return emptyList()
      }

    Logger.d(TAG, "originalPosts.size=${postsCount}, " +
      "retainedPosts.size=${retainedPosts.size}, " +
      "query=\"$query\"")

    val indexedPosts: MutableList<PostIndexed> = ArrayList(retainedPosts.size)

    for (currentPostIndex in retainedPosts.indices) {
      val retainedPost = retainedPosts[currentPostIndex]
      indexedPosts.add(PostIndexed(retainedPost, currentPostIndex))
    }

    return indexedPosts
  }

  private fun processOrder(posts: List<ChanOriginalPost>) {
    when (order) {
      Order.IMAGE -> Collections.sort(posts, IMAGE_COMPARATOR)
      Order.REPLY -> Collections.sort(posts, REPLY_COMPARATOR)
      Order.NEWEST -> Collections.sort(posts, NEWEST_COMPARATOR)
      Order.OLDEST -> Collections.sort(posts, OLDEST_COMPARATOR)
      Order.MODIFIED -> Collections.sort(posts, MODIFIED_COMPARATOR)
      Order.ACTIVITY -> Collections.sort(posts, THREAD_ACTIVITY_COMPARATOR)
      Order.BUMP -> {
        // no-op
      }
    }
  }

  private fun processSearch(posts: MutableList<ChanPost>) {
    val lowerQuery = query?.toLowerCase(Locale.ENGLISH)
      ?: return

    var add: Boolean
    val iterator = posts.iterator()

    while (iterator.hasNext()) {
      val chanPost = iterator.next()
      add = false

      if (chanPost.postComment.originalComment().toString().toLowerCase(Locale.ENGLISH).contains(lowerQuery)) {
        add = true
      } else if (chanPost.subject.toString().toLowerCase(Locale.ENGLISH).contains(lowerQuery)) {
        add = true
      } else if (chanPost.name?.toLowerCase(Locale.ENGLISH)?.contains(lowerQuery) == true) {
        add = true
      } else if (chanPost.postImages.isNotEmpty()) {
        for (image in chanPost.postImages) {
          if (image.filename != null && image.filename?.toLowerCase(Locale.ENGLISH)?.contains(lowerQuery) == true) {
            add = true
            break
          }
        }
      }

      if (!add) {
        iterator.remove()
      }
    }
  }

  enum class Order(var orderName: String) {
    BUMP("bump"),
    REPLY("reply"),
    IMAGE("image"),
    NEWEST("newest"),
    OLDEST("oldest"),
    MODIFIED("modified"),
    ACTIVITY("activity");

    companion object {
      fun find(name: String): Order? {

        return values().firstOrNull { it.orderName == name }
      }

      @JvmStatic
      fun isNotBumpOrder(orderString: String): Boolean {
        val o = find(orderString)
        return BUMP != o
      }
    }
  }

  companion object {
    private const val TAG = "PostsFilter"

    private val IMAGE_COMPARATOR = Comparator<ChanOriginalPost> { lhs, rhs -> rhs.catalogImagesCount - lhs.catalogImagesCount }
    private val REPLY_COMPARATOR = Comparator<ChanOriginalPost> { lhs, rhs -> rhs.catalogRepliesCount - lhs.catalogRepliesCount }
    private val NEWEST_COMPARATOR = Comparator<ChanOriginalPost> { lhs, rhs -> (rhs.timestamp - lhs.timestamp).toInt() }
    private val OLDEST_COMPARATOR = Comparator<ChanOriginalPost> { lhs, rhs -> (lhs.timestamp - rhs.timestamp).toInt() }
    private val MODIFIED_COMPARATOR = Comparator<ChanOriginalPost> { lhs, rhs -> (rhs.lastModified - lhs.lastModified).toInt() }

    private val THREAD_ACTIVITY_COMPARATOR = Comparator<ChanOriginalPost> { lhs, rhs ->
      val currentTimeSeconds = System.currentTimeMillis() / 1000
      // we can't divide by zero, but we can divide by the smallest thing that's closest to 0 instead
      val eps = 0.0001f

      val lhsDivider = if (lhs.catalogRepliesCount > 0) {
        lhs.catalogRepliesCount.toFloat()
      } else {
        eps
      }

      val rhsDivider = if (rhs.catalogRepliesCount > 0) {
        rhs.catalogRepliesCount.toFloat()
      } else {
        eps
      }

      val score1 = ((currentTimeSeconds - lhs.timestamp).toFloat() / lhsDivider).toLong()
      val score2 = ((currentTimeSeconds - rhs.timestamp).toFloat() / rhsDivider).toLong()

      return@Comparator score1.compareTo(score2)
    }
  }

}