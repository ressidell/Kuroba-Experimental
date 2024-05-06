package com.github.k1rakishou.chan.core.manager

import android.net.Uri
import androidx.annotation.GuardedBy
import com.github.k1rakishou.common.mutableMapWithCap
import com.github.k1rakishou.fsaf.FileManager
import com.github.k1rakishou.model.data.descriptor.ChanDescriptor
import com.github.k1rakishou.model.data.descriptor.PostDescriptor
import com.github.k1rakishou.model.data.download.ImageDownloadRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

class DownloadedImagesManager(
  private val fileManager: FileManager
) {
  private val _downloadedImages = ConcurrentHashMap<ChanDescriptor.ThreadDescriptor, DownloadedImagesInThread>(32)

  private val _downloadedImageKeyEventsFlow = MutableSharedFlow<DownloadedImageKey>(extraBufferCapacity = Channel.UNLIMITED)
  val downloadedImageKeyEventsFlow: SharedFlow<DownloadedImageKey>
    get() = _downloadedImageKeyEventsFlow.asSharedFlow()

  suspend fun isImageDownloaded(
    postDescriptor: PostDescriptor,
    imageFullUrl: HttpUrl
  ): Boolean {
    return _downloadedImages[postDescriptor.threadDescriptor()]
      ?.isImageDownloaded(postDescriptor, imageFullUrl) ?: false
  }

  suspend fun onImageDownloaded(
    outputFileUri: Uri,
    imageDownloadRequest: ImageDownloadRequest
  ) {
    val postDescriptor = PostDescriptor.deserializeFromString(imageDownloadRequest.postDescriptorString)
    if (postDescriptor == null) {
      return
    }

    val revealedImagesInThread = _downloadedImages.getOrPut(
      key = postDescriptor.threadDescriptor(),
      defaultValue = { DownloadedImagesInThread(fileManager = fileManager) }
    )

    val revealedSpoilerImage = revealedImagesInThread.add(
      postDescriptor = postDescriptor,
      outputFileUri = outputFileUri,
      imageDownloadRequest = imageDownloadRequest
    )

    _downloadedImageKeyEventsFlow.emit(revealedSpoilerImage)
  }

  class DownloadedImagesInThread(
    private val fileManager: FileManager
  ) {
    private val mutex = Mutex()

    @GuardedBy("mutex")
    private val _downloadedImages = mutableMapWithCap<DownloadedImageKey, DownloadedImageValue>(16)

    suspend fun add(
      postDescriptor: PostDescriptor,
      outputFileUri: Uri,
      imageDownloadRequest: ImageDownloadRequest
    ): DownloadedImageKey {
      val downloadedImageKey = imageDownloadRequest.toDownloadedImageKey(postDescriptor)
      val downloadedImageValue = DownloadedImageValue(outputFileUri = outputFileUri)

      mutex.withLock { _downloadedImages.put(downloadedImageKey, downloadedImageValue) }
      return downloadedImageKey
    }

    suspend fun isImageDownloaded(postDescriptor: PostDescriptor, imageFullUrl: HttpUrl): Boolean {
      val downloadedImageKey = DownloadedImageKey(
        postDescriptor = postDescriptor,
        fullImageUrl = imageFullUrl
      )

      return isImageDownloaded(downloadedImageKey)
    }

    suspend fun isImageDownloaded(downloadedImageKey: DownloadedImageKey): Boolean {
      val downloadedImageValue = mutex.withLock { _downloadedImages.get(downloadedImageKey) }
      if (downloadedImageValue == null) {
        return false
      }

      val existsOnDisk = withContext(Dispatchers.IO) {
        return@withContext runInterruptible {
          val file = fileManager.fromUri(downloadedImageValue.outputFileUri)
          if (file == null) {
            return@runInterruptible false
          }

          return@runInterruptible fileManager.exists(file) && fileManager.getLength(file) > 0L
        }
      }

      if (!existsOnDisk) {
        mutex.withLock { _downloadedImages.remove(downloadedImageKey) }
      }

      return existsOnDisk
    }

    private fun ImageDownloadRequest.toDownloadedImageKey(
      postDescriptor: PostDescriptor
    ): DownloadedImageKey {
      return DownloadedImageKey(
        postDescriptor = postDescriptor,
        fullImageUrl = imageFullUrl
      )
    }

  }

  data class DownloadedImageKey(
    val postDescriptor: PostDescriptor,
    val fullImageUrl: HttpUrl
  )

  data class DownloadedImageValue(
    val outputFileUri: Uri
  )

}