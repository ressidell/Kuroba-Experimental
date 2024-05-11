package com.github.k1rakishou.chan.core.image.loader

import android.graphics.Bitmap
import coil.memory.MemoryCache

interface KurobaImageFromMemoryLoader {
  suspend fun loadFromMemoryCache(
    memoryCacheKey: MemoryCache.Key
  ): Bitmap?
}