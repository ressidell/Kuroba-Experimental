package com.github.k1rakishou.chan.core.image.loader

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import coil.memory.MemoryCache
import coil.transform.Transformation
import com.github.k1rakishou.chan.core.cache.CacheFileType
import com.github.k1rakishou.common.ModularResult

interface KurobaImageFromNetworkLoader {

  suspend fun loadFromNetwork(
    context: Context,
    url: String,
    memoryCacheKey: MemoryCache.Key?,
    cacheFileType: CacheFileType,
    imageSize: KurobaImageSize,
    transformations: List<Transformation> = emptyList(),
  ): ModularResult<BitmapDrawable>

}