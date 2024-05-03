package com.github.k1rakishou.chan.core.image.loader

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import coil.size.Scale
import coil.transform.Transformation
import com.github.k1rakishou.chan.core.cache.CacheFileType
import com.github.k1rakishou.chan.core.image.InputFile
import com.github.k1rakishou.common.ModularResult
import com.github.k1rakishou.model.data.descriptor.PostDescriptor

interface KurobaImageFromDiskLoader {

  suspend fun isImageCachedLocally(
    cacheFileType: CacheFileType,
    url: String
  ): Boolean

  suspend fun loadFromDisk(
    context: Context,
    inputFile: InputFile,
    imageSize: KurobaImageSize,
    scale: Scale,
    transformations: List<Transformation>,
  ): ModularResult<BitmapDrawable>

  suspend fun tryToLoadFromDisk(
    context: Context,
    url: String,
    postDescriptor: PostDescriptor?,
    cacheFileType: CacheFileType,
    imageSize: KurobaImageSize,
    scale: Scale,
    transformations: List<Transformation>,
  ): ModularResult<BitmapDrawable?>

}