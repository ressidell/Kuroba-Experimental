package com.github.k1rakishou.chan.core.image.loader

import android.graphics.Bitmap
import coil.ImageLoader
import coil.memory.MemoryCache
import dagger.Lazy

class KurobaImageFromMemoryLoaderImpl(
  private val coilImageLoaderLazy: Lazy<ImageLoader>
) : KurobaImageFromMemoryLoader {

  private val coilImageLoader: ImageLoader
    get() = coilImageLoaderLazy.get()

  override suspend fun loadFromMemoryCache(
    memoryCacheKey: MemoryCache.Key
  ): Bitmap? {
    return coilImageLoader.memoryCache?.get(memoryCacheKey)?.bitmap
  }

}