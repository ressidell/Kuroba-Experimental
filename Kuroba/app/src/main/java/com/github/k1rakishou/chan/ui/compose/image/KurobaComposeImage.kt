package com.github.k1rakishou.chan.ui.compose.image

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import coil.network.HttpException
import coil.transform.Transformation
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.core.cache.CacheFileType
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeText
import com.github.k1rakishou.chan.ui.compose.ktu
import com.github.k1rakishou.chan.ui.compose.providers.LocalChanTheme
import com.github.k1rakishou.chan.utils.appDependencies
import com.github.k1rakishou.common.ExceptionWithShortErrorMessage
import okhttp3.HttpUrl
import javax.net.ssl.SSLException

@Composable
fun KurobaComposeImage(
  modifier: Modifier,
  request: ImageLoaderRequest,
  contentScale: ContentScale = ContentScale.Crop,
  loading: (@Composable BoxScope.() -> Unit)? = null,
  error: (@Composable BoxScope.(Throwable) -> Unit)? = { throwable -> DefaultErrorHandler(throwable) },
  success: (@Composable () -> Unit)? = null
) {
  var size by remember { mutableStateOf<IntSize?>(null) }

  val measureModifier = Modifier.onSizeChanged { newSize ->
    if (newSize.width > 0 || newSize.height > 0) {
      size = newSize
    }
  }

  Box(modifier = modifier.then(measureModifier)) {
    BuildInnerImage(
      size = size,
      request = request,
      contentScale = contentScale,
      loading = loading,
      error = error,
      success = success
    )
  }
}

@Composable
private fun BoxScope.DefaultErrorHandler(throwable: Throwable) {
  val errorMsg = when (throwable) {
    is ExceptionWithShortErrorMessage -> throwable.shortErrorMessage()
    is SSLException -> stringResource(id = R.string.ssl_error)
    is HttpException -> stringResource(id = R.string.http_error, throwable.response.code)
    else -> throwable::class.java.simpleName
  }

  val chanTheme = LocalChanTheme.current
  val textColor = chanTheme.textColorSecondaryCompose

  KurobaComposeText(
    modifier = Modifier.align(Alignment.Center),
    text = errorMsg,
    color = textColor,
    fontSize = 11.ktu.fixedSize(),
    maxLines = 3,
    textAlign = TextAlign.Center
  )
}

@Composable
private fun BuildInnerImage(
  size: IntSize?,
  request: ImageLoaderRequest,
  contentScale: ContentScale,
  loading: (@Composable BoxScope.() -> Unit)? = null,
  error: (@Composable BoxScope.(Throwable) -> Unit)? = null,
  success: (@Composable () -> Unit)? = null
) {
  val context = LocalContext.current
  val kurobaImageLoader = appDependencies().kurobaImageLoader

  val imageLoaderResult by produceState<ImageLoaderResult>(
    initialValue = ImageLoaderResult.NotInitialized,
    key1 = request,
    key2 = size,
    producer = { loadImage(kurobaImageLoader, context, request, size) }
  )

  when (val result = imageLoaderResult) {
    ImageLoaderResult.NotInitialized -> {
      Spacer(modifier = Modifier.fillMaxSize())
      return
    }
    ImageLoaderResult.Loading -> {
      if (loading != null) {
        Box(modifier = Modifier.fillMaxSize()) {
          loading()
        }
      }

      return
    }
    is ImageLoaderResult.Error -> {
      if (error != null) {
        Box(modifier = Modifier.fillMaxSize()) {
          error(result.throwable)
        }
      }

      return
    }
    is ImageLoaderResult.Success -> {
      success?.invoke()

      Image(
        painter = result.painter,
        contentDescription = null,
        contentScale = contentScale,
        modifier = Modifier.fillMaxSize()
      )
    }
  }
}

@Immutable
data class ImageLoaderRequest(
  val data: ImageLoaderRequestData,
  val transformations: List<Transformation> = emptyList<Transformation>(),
) {
  override fun toString(): String {
    return "ImageLoaderRequest(data=$data, transformations=${transformations.size})"
  }
}

@Immutable
sealed class ImageLoaderRequestData {
  fun uniqueKey(): String {
    return when (this) {
      is DrawableResource -> drawableId.toString()
      is File -> absolutePath
      is Uri -> uri.toString()
      is Url -> httpUrl.toString()
    }
  }

  fun asUrlOrNull(): HttpUrl? {
    if (this is Url) {
      return httpUrl
    }

    return null
  }

  data class File(val file: java.io.File) : ImageLoaderRequestData() {
    val absolutePath: String = file.absolutePath

    override fun toString(): String {
      return "File(file=${absolutePath})"
    }
  }

  data class Uri(val uri: android.net.Uri) : ImageLoaderRequestData()

  data class Url(
    val httpUrl: HttpUrl,
    val cacheFileType: CacheFileType
  ) : ImageLoaderRequestData()

  data class DrawableResource(
    @DrawableRes val drawableId: Int
  ) : ImageLoaderRequestData()
}