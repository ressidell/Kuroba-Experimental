package com.github.k1rakishou.chan.core.image.loader

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.view.View
import androidx.annotation.DrawableRes
import coil.size.Scale
import coil.size.Size
import coil.size.ViewSizeResolver
import coil.transform.Transformation
import com.github.k1rakishou.chan.core.cache.CacheFileType
import com.github.k1rakishou.chan.core.image.InputFile
import com.github.k1rakishou.chan.ui.view.widget.FixedViewSizeResolver
import com.github.k1rakishou.common.ModularResult
import com.github.k1rakishou.model.data.descriptor.PostDescriptor

interface KurobaImageLoader {
  suspend fun isImageCachedLocally(cacheFileType: CacheFileType, url: String): Boolean

  suspend fun loadFromResources(
    context: Context,
    @DrawableRes drawableId: Int,
    imageSize: KurobaImageSize,
    scale: Scale = Scale.FIT,
    transformations: List<Transformation> = emptyList()
  ): ModularResult<BitmapDrawable>

  suspend fun loadFromNetwork(
    context: Context,
    url: String,
    cacheFileType: CacheFileType,
    imageSize: KurobaImageSize,
    scale: Scale = Scale.FIT,
    postDescriptor: PostDescriptor? = null,
    transformations: List<Transformation> = emptyList(),
  ): ModularResult<BitmapDrawable>

  suspend fun loadFromDisk(
    context: Context,
    inputFile: InputFile,
    imageSize: KurobaImageSize,
    scale: Scale = Scale.FIT,
    transformations: List<Transformation> = emptyList(),
  ): ModularResult<BitmapDrawable>

}

sealed class KurobaImageSize {
  suspend fun size(): Size {
    return when (this) {
      is FixedImageSize -> Size(width, height)
      is MeasurableImageSize -> sizeResolver.size()
      is Unspecified -> Size(0, 0)
    }
  }

  data object Unspecified : KurobaImageSize()

  class FixedImageSize(val width: Int, val height: Int) : KurobaImageSize() {
    override fun toString(): String = "FixedImageSize{${width}x${height}}"
  }

  class MeasurableImageSize private constructor(val sizeResolver: ViewSizeResolver<View>) : KurobaImageSize() {
    override fun toString(): String = "MeasurableImageSize"

    companion object {
      @JvmStatic
      fun create(view: View): MeasurableImageSize {
        return MeasurableImageSize(FixedViewSizeResolver(view))
      }
    }
  }
}