package com.github.k1rakishou.chan.core.manager

import androidx.annotation.GuardedBy
import com.github.k1rakishou.common.hashSetWithCap
import com.github.k1rakishou.model.data.descriptor.PostDescriptor
import com.github.k1rakishou.model.data.post.ChanPostImage
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.HttpUrl
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

// TODO: clear cache when clearing PostImagesFull disk cache
class PrefetchStateManager {
  private val _prefetchEventFlow = MutableSharedFlow<PrefetchState>(
    extraBufferCapacity = 1024,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )
  val prefetchEventFlow: SharedFlow<PrefetchState>
    get() = _prefetchEventFlow.asSharedFlow()

  private val lock = ReentrantReadWriteLock()

  @GuardedBy("lock")
  private val _prefetchedChanPostImages = hashSetWithCap<PrefetchedChanPostImage>(initialCapacity = 1024)

  fun isPrefetched(postImage: ChanPostImage?): Boolean {
    if (postImage == null) {
      return false
    }

    val prefetchedChanPostImage = PrefetchedChanPostImage.fromChanPostImage(postImage)
    return lock.read { _prefetchedChanPostImages.contains(prefetchedChanPostImage) }
  }

  fun onPrefetchStarted(postImage: ChanPostImage) {
    if (isPrefetched(postImage)) {
      return
    }

    _prefetchEventFlow.tryEmit(PrefetchState.PrefetchStarted(postImage))
  }

  fun onPrefetchProgress(postImage: ChanPostImage, progress: Float) {
    if (isPrefetched(postImage)) {
      return
    }

    _prefetchEventFlow.tryEmit(PrefetchState.PrefetchProgress(postImage, progress))
  }

  fun onPrefetchCompleted(postImage: ChanPostImage, success: Boolean) {
    val prefetchedChanPostImage = PrefetchedChanPostImage.fromChanPostImage(postImage)
    if (prefetchedChanPostImage != null) {
      lock.write { _prefetchedChanPostImages.add(prefetchedChanPostImage) }
    }

    _prefetchEventFlow.tryEmit(PrefetchState.PrefetchCompleted(postImage, success))
  }

  private data class PrefetchedChanPostImage(
    val postDescriptor: PostDescriptor,
    val thumbnailUrl: HttpUrl?,
    val fullUrl: HttpUrl
  ) {

    companion object {
      fun fromChanPostImage(chanPostImage: ChanPostImage): PrefetchedChanPostImage? {
        val fullUrl = chanPostImage.imageUrl
        if (fullUrl == null) {
          return null
        }

        return PrefetchedChanPostImage(
          postDescriptor = chanPostImage.ownerPostDescriptor,
          thumbnailUrl = chanPostImage.actualThumbnailUrl,
          fullUrl = fullUrl
        )
      }
    }
  }

  companion object {
    private const val TAG = "PrefetchImageDownloadIndicatorManager"
  }
}

sealed class PrefetchState(val postImage: ChanPostImage) {
  class PrefetchStarted(postImage: ChanPostImage) : PrefetchState(postImage)
  class PrefetchProgress(postImage: ChanPostImage, val progress: Float) : PrefetchState(postImage)
  class PrefetchCompleted(postImage: ChanPostImage, val success: Boolean) : PrefetchState(postImage)
}