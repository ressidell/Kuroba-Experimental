package com.github.k1rakishou.chan.core.image.loader

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import coil.size.Scale
import coil.transform.Transformation
import com.github.k1rakishou.chan.core.cache.CacheFileType
import com.github.k1rakishou.chan.core.image.InputFile
import com.github.k1rakishou.common.ModularResult
import com.github.k1rakishou.model.data.descriptor.PostDescriptor
import dagger.Lazy

class KurobaImageLoaderImpl(
  private val resourcesLoaderLazy: Lazy<KurobaImageFromResourcesLoader>,
  private val diskLoaderLazy: Lazy<KurobaImageFromDiskLoader>,
  private val networkLoaderLazy: Lazy<KurobaImageFromNetworkLoader>,
) : KurobaImageLoader {

  private val resourcesLoader: KurobaImageFromResourcesLoader
    get() = resourcesLoaderLazy.get()
  private val diskLoader: KurobaImageFromDiskLoader
    get() = diskLoaderLazy.get()
  private val networkLoader: KurobaImageFromNetworkLoader
    get() = networkLoaderLazy.get()

  override suspend fun isImageCachedLocally(cacheFileType: CacheFileType, url: String): Boolean {
    return diskLoader.isImageCachedLocally(
      cacheFileType = cacheFileType,
      url = url
    )
  }

  override suspend fun loadFromResources(
    context: Context,
    drawableId: Int,
    imageSize: KurobaImageSize,
    scale: Scale,
    transformations: List<Transformation>
  ): ModularResult<BitmapDrawable> {
    return resourcesLoader.loadFromResources(
      context = context,
      drawableId = drawableId,
      scale = scale,
      imageSize = imageSize,
      transformations = transformations
    )
  }

  override suspend fun loadFromNetwork(
    context: Context,
    url: String,
    cacheFileType: CacheFileType,
    imageSize: KurobaImageSize,
    scale: Scale,
    postDescriptor: PostDescriptor?,
    transformations: List<Transformation>
  ): ModularResult<BitmapDrawable> {
    val loadFromDiskResult = diskLoader.tryToLoadFromDisk(
      context = context,
      url = url,
      postDescriptor = postDescriptor,
      cacheFileType = cacheFileType,
      imageSize = imageSize,
      scale = scale,
      transformations = transformations
    )

    if (loadFromDiskResult is ModularResult.Value) {
      val bitmapDrawable = loadFromDiskResult.value
      if (bitmapDrawable != null) {
        return ModularResult.value(bitmapDrawable)
      }

      // Fallthrough
    }

    return networkLoader.loadFromNetwork(
      context = context,
      url = url,
      scale = scale,
      cacheFileType = cacheFileType,
      imageSize = imageSize,
      transformations = transformations
    )
  }

  override suspend fun loadFromDisk(
    context: Context,
    inputFile: InputFile,
    imageSize: KurobaImageSize,
    scale: Scale,
    transformations: List<Transformation>
  ): ModularResult<BitmapDrawable> {
    return diskLoader.loadFromDisk(
      context = context,
      inputFile = inputFile,
      scale = scale,
      imageSize = imageSize,
      transformations = transformations
    )
  }

  companion object {
    private const val TAG = "KurobaImageLoaderImpl"
  }

}