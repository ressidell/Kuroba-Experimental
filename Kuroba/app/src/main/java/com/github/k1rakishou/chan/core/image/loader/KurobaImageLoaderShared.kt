package com.github.k1rakishou.chan.core.image.loader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Dimension
import coil.size.Scale
import coil.size.Size
import coil.transform.Transformation
import com.github.k1rakishou.ChanSettings
import com.github.k1rakishou.chan.core.cache.CacheFileType
import com.github.k1rakishou.chan.core.cache.CacheHandler
import com.github.k1rakishou.chan.core.cache.downloader.ChunkedMediaDownloader
import com.github.k1rakishou.chan.core.image.InputFile
import com.github.k1rakishou.chan.utils.KurobaMediaType
import com.github.k1rakishou.chan.utils.MediaUtils
import com.github.k1rakishou.chan.utils.asKurobaMediaType
import com.github.k1rakishou.chan.utils.lifecycleFromContextOrNull
import com.github.k1rakishou.common.ModularResult
import com.github.k1rakishou.common.StringUtils
import com.github.k1rakishou.common.errorMessageOrClassName
import com.github.k1rakishou.common.isCancellationException
import com.github.k1rakishou.core_logger.Logger
import com.github.k1rakishou.fsaf.file.AbstractFile
import com.github.k1rakishou.fsaf.file.ExternalFile
import com.github.k1rakishou.fsaf.file.RawFile
import com.google.android.exoplayer2.util.MimeTypes
import java.io.File

private const val TAG = "KurobaImageLoaderShared"

suspend fun fileIsProbablyVideoInterruptible(
  appContext: Context,
  fileName: String,
  inputFile: InputFile
): Boolean {
  val kurobaMediaType = StringUtils.extractFileNameExtension(fileName)
    ?.asKurobaMediaType()

  val hasVideoMimeType = when (kurobaMediaType) {
    KurobaMediaType.Video -> true
    KurobaMediaType.Gif,
    KurobaMediaType.Image,
    KurobaMediaType.Unknown,
    null -> false
  }

  if (hasVideoMimeType) {
    return true
  }

  return MediaUtils.decodeFileMimeTypeInterruptible(appContext, inputFile)
    ?.let { mimeType -> MimeTypes.isVideo(mimeType) }
    ?: false
}

internal fun ImageRequest.Builder.applyImageSize(imageSize: KurobaImageSize) {
  when (imageSize) {
    is KurobaImageSize.FixedImageSize -> {
      val width = imageSize.width
      val height = imageSize.height

      if ((width > 0) && (height > 0)) {
        size(width, height)
      }
    }
    is KurobaImageSize.MeasurableImageSize -> {
      size(imageSize.sizeResolver)
    }
    is KurobaImageSize.Unspecified -> {
      // no-op
    }
  }
}

internal suspend fun applyTransformationsToDrawable(
  coilImageLoader: ImageLoader,
  chunkedMediaDownloader: ChunkedMediaDownloader,
  cacheHandler: CacheHandler,
  context: Context,
  imageFile: AbstractFile,
  url: String,
  cacheFileType: CacheFileType,
  scale: Scale,
  imageSize: KurobaImageSize,
  transformations: List<Transformation>
): ModularResult<BitmapDrawable> {
  return ModularResult.Try {
    val fileLocation = when (imageFile) {
      is RawFile -> File(imageFile.getFullPath())
      is ExternalFile -> imageFile.getUri()
      else -> error("Unknown file type: ${imageFile.javaClass.simpleName}")
    }

    // TODO: should probably move this to the side of the caller
    // When using any transformations at all we won't be able to use HARDWARE bitmaps. We only really
    // need the RESIZE_TRANSFORMATION when highResCells setting is turned on because we load original
    // images which we then want to resize down to ThumbnailView dimensions.
    val combinedTransformations = if (ChanSettings.highResCells.get()) {
      transformations + ResizeTransformation()
    } else {
      transformations
    }

    val request = with(ImageRequest.Builder(context)) {
      lifecycle(context.lifecycleFromContextOrNull())
      data(fileLocation)
      scale(scale)
      transformations(combinedTransformations)
      applyImageSize(imageSize)

      build()
    }

    when (val result = coilImageLoader.execute(request)) {
      is SuccessResult -> {
        val bitmap = result.drawable.toBitmap()
        return@Try BitmapDrawable(context.resources, bitmap)
      }
      is ErrorResult -> {
        if (!result.throwable.isCancellationException()) {
          Logger.error(TAG) {
            "applyTransformationsToDrawable() error, " +
              "fileLocation: ${fileLocation}, error: ${result.throwable.errorMessageOrClassName()}"
          }

          if (!chunkedMediaDownloader.isRunning(url)) {
            cacheHandler.deleteCacheFileByUrl(cacheFileType, url)
          }
        }

        throw result.throwable
      }
    }
  }
}

class KurobaImageLoaderException(message: String) : Exception()

private class ResizeTransformation : Transformation {
  override val cacheKey: String = "LoaderShared_ResizeTransformation"

  override suspend fun transform(input: Bitmap, size: Size): Bitmap {
    val availableWidth = when (val width = size.width) {
      is Dimension.Pixels -> width.px
      Dimension.Undefined -> input.width
    }

    val availableHeight = when (val height = size.height) {
      is Dimension.Pixels -> height.px
      Dimension.Undefined -> input.height
    }

    if (input.width <= availableWidth && input.height <= availableHeight) {
      // If the bitmap fits into the availableSize then do not re-scale it again to avoid
      // re-allocations and all that stuff
      return input
    }

    return scale(input, availableWidth, availableHeight)
  }

  private fun config(): Bitmap.Config {
    if (ChanSettings.isLowRamDevice()) {
      return Bitmap.Config.RGB_565
    }

    return Bitmap.Config.ARGB_8888
  }

  private fun scale(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
    val width: Int
    val height: Int
    val widthRatio = bitmap.width.toFloat() / maxWidth
    val heightRatio = bitmap.height.toFloat() / maxHeight

    if (widthRatio >= heightRatio) {
      width = maxWidth
      height = (width.toFloat() / bitmap.width * bitmap.height).toInt()
    } else {
      height = maxHeight
      width = (height.toFloat() / bitmap.height * bitmap.width).toInt()
    }

    val scaledBitmap = Bitmap.createBitmap(width, height, bitmap.config ?: config())
    val ratioX = width.toFloat() / bitmap.width
    val ratioY = height.toFloat() / bitmap.height
    val middleX = width / 2.0f
    val middleY = height / 2.0f
    val scaleMatrix = Matrix()
    scaleMatrix.setScale(ratioX, ratioY, middleX, middleY)

    val canvas = Canvas(scaledBitmap)
    canvas.setMatrix(scaleMatrix)
    canvas.drawBitmap(
      bitmap,
      middleX - bitmap.width / 2,
      middleY - bitmap.height / 2,
      Paint(Paint.FILTER_BITMAP_FLAG)
    )

    return scaledBitmap
  }
}