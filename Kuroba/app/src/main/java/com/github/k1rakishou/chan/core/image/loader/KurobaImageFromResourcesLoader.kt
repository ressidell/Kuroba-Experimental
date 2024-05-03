package com.github.k1rakishou.chan.core.image.loader

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.DrawableRes
import coil.size.Scale
import coil.transform.Transformation
import com.github.k1rakishou.common.ModularResult

interface KurobaImageFromResourcesLoader {

  suspend fun loadFromResources(
    context: Context,
    @DrawableRes drawableId: Int,
    scale: Scale,
    imageSize: KurobaImageSize,
    transformations: List<Transformation> = emptyList()
  ): ModularResult<BitmapDrawable>

}