package com.github.k1rakishou.chan.core.image.loader

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import coil.transform.Transformation
import com.github.k1rakishou.chan.utils.lifecycleFromContextOrNull
import com.github.k1rakishou.common.ModularResult
import com.github.k1rakishou.common.errorMessageOrClassName
import com.github.k1rakishou.core_logger.Logger
import dagger.Lazy

class KurobaImageFromResourcesLoaderImpl(
  private val coilImageLoaderLazy: Lazy<ImageLoader>,
) : KurobaImageFromResourcesLoader {
  private val coilImageLoader: ImageLoader
    get() = coilImageLoaderLazy.get()

  override suspend fun loadFromResources(
    context: Context,
    drawableId: Int,
    scale: Scale,
    imageSize: KurobaImageSize,
    transformations: List<Transformation>
  ): ModularResult<BitmapDrawable> {
    return ModularResult.Try {
      val lifecycle = context.lifecycleFromContextOrNull()

      val request = with(ImageRequest.Builder(context)) {
        data(drawableId)
        lifecycle(lifecycle)
        transformations(transformations)
        scale(scale)
        applyImageSize(imageSize)

        build()
      }

      Logger.verbose(TAG) { "loadFromResources() Loading '$drawableId' with size $imageSize" }

      val result = when (val imageResult = coilImageLoader.execute(request)) {
        is SuccessResult -> {
          val bitmap = imageResult.drawable.toBitmap()
          Logger.verbose(TAG) { "loadFromResources() Loading '$drawableId' success, bitmap: ${bitmap}" }

          BitmapDrawable(context.resources, bitmap)
        }
        is ErrorResult -> {
          Logger.error(TAG) {
            "loadFromResources() Loading '$drawableId' failure, " +
              "error: ${imageResult.throwable.errorMessageOrClassName()}"
          }

          throw imageResult.throwable
        }
      }

      return@Try result
    }
  }

  companion object {
    private const val TAG = "KurobaImageFromResourcesLoaderImpl"
  }

}