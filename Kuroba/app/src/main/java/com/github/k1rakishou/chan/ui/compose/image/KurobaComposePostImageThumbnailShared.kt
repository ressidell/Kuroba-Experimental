package com.github.k1rakishou.chan.ui.compose.image

import android.content.Context
import androidx.compose.runtime.ProduceStateScope
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.IntSize
import com.github.k1rakishou.chan.core.image.InputFile
import com.github.k1rakishou.chan.core.image.loader.KurobaImageLoader
import com.github.k1rakishou.chan.core.image.loader.KurobaImageSize
import com.github.k1rakishou.chan.utils.isResumed
import com.github.k1rakishou.chan.utils.lifecycleFromContent
import com.github.k1rakishou.common.ModularResult
import com.github.k1rakishou.core_logger.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import okio.IOException

private const val TAG = "KurobaComposePostImageThumbnailShared"
private val Delays = arrayOf<Long>(100, 200, 500, 1000, 2000, 5000, 10000)

internal suspend fun ProduceStateScope<ImageLoaderResult>.loadImage(
  kurobaImageLoader: KurobaImageLoader,
  context: Context,
  request: ImageLoaderRequest,
  size: IntSize?
) {
  val lifecycle = context.lifecycleFromContent()
  val maxRetries = 7

  this.value = ImageLoaderResult.Loading

  if (size == null || size.width <= 0 || size.height <= 0) {
    Logger.verbose(TAG) { "loadImage(${request}, ${size}) bad size: ${size}" }
    return
  }

  var imageLoadResult: ModularResult<BitmapPainter> = ModularResult.error(IOException("Unknown error"))

  for (reloadAttempt in 0 until maxRetries) {
    if (!isActive) {
      break
    }

    if (!lifecycle.isResumed()) {
      Logger.error(TAG) {
        "loadImage(${request}, ${size}) attempt ${reloadAttempt + 1}/${maxRetries}, " +
          "lifecycle is not resumed: ${lifecycle.currentState}"
      }

      break
    }

    Logger.verbose(TAG) { "loadImage(${request}, ${size}) attempt ${reloadAttempt + 1}/${maxRetries}..." }

    val result = when (val data = request.data) {
      is ImageLoaderRequestData.File,
      is ImageLoaderRequestData.Uri -> {
        val inputFile = if (data is ImageLoaderRequestData.File) {
          InputFile.JavaFile(data.file)
        } else {
          data as ImageLoaderRequestData.Uri
          InputFile.FileUri(data.uri)
        }

        kurobaImageLoader.loadFromDisk(
          context = context,
          inputFile = inputFile,
          imageSize = KurobaImageSize.FixedImageSize(size.width, size.height),
          transformations = request.transformations
        )
      }
      is ImageLoaderRequestData.Url -> {
        kurobaImageLoader.loadFromNetwork(
          context = context,
          url = data.httpUrl.toString(),
          cacheFileType = data.cacheFileType,
          imageSize = KurobaImageSize.FixedImageSize(size.width, size.height),
          transformations = request.transformations
        )
      }
      is ImageLoaderRequestData.DrawableResource -> {
        kurobaImageLoader.loadFromResources(
          context = context,
          drawableId = data.drawableId,
          imageSize = KurobaImageSize.FixedImageSize(size.width, size.height),
          transformations = request.transformations
        )
      }
    }
      .wrapCancellationException()
      .mapValue { bitmapDrawable -> BitmapPainter(bitmapDrawable.bitmap.asImageBitmap()) }

    imageLoadResult = result

    when (result) {
      is ModularResult.Value -> {
        // Success
        Logger.verbose(TAG) { "loadImage(${request}, ${size}) attempt ${reloadAttempt + 1}/${maxRetries}... success!" }
        break
      }
      is ModularResult.Error -> {
        val error = result.error

        if (error is CancellationException) {
          // Exit on cancellation
          Logger.verbose(TAG) { "loadImage(${request}, ${size}) attempt ${reloadAttempt + 1}/${maxRetries}... canceled." }
          return
        }

        if (error !is IOException) {
          // Exit and display last error
          Logger.verbose(TAG) {
            "loadImage(${request}, ${size}) attempt ${reloadAttempt + 1}/${maxRetries}... " +
              "error: ${error::class.java.simpleName}"
          }

          break
        }

        // Wait and retry if the error is IOException
        val delayMs = Delays.getOrNull(reloadAttempt)

        Logger.verbose(TAG) {
          "loadImage(${request}, ${size}) attempt ${reloadAttempt + 1}/${maxRetries} " +
            "error: ${error::class.java.simpleName}, waiting: ${delayMs}ms..."
        }

        if (delayMs == null) {
          break
        }

        delay(delayMs)
      }
    }
  }

  this.value = when (imageLoadResult) {
    is ModularResult.Error -> ImageLoaderResult.Error(imageLoadResult.error)
    is ModularResult.Value -> ImageLoaderResult.Success(imageLoadResult.value)
  }
}