package com.github.k1rakishou.chan.ui.compose.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.network.HttpException
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.ui.compose.Shimmer
import com.github.k1rakishou.chan.ui.compose.components.IconTint
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeIcon
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeText
import com.github.k1rakishou.chan.ui.compose.components.kurobaClickable
import com.github.k1rakishou.chan.ui.compose.ktu
import com.github.k1rakishou.chan.ui.compose.providers.LocalChanTheme
import com.github.k1rakishou.chan.utils.KurobaMediaType
import com.github.k1rakishou.chan.utils.appDependencies
import com.github.k1rakishou.common.ExceptionWithShortErrorMessage
import javax.net.ssl.SSLException

@Immutable
interface PostImageThumbnailKey

@Composable
fun KurobaComposePostImageThumbnail(
  modifier: Modifier,
  key: PostImageThumbnailKey,
  request: ImageLoaderRequest,
  mediaType: KurobaMediaType,
  backgroundColor: Color = LocalChanTheme.current.backColorSecondaryCompose,
  hasAudio: Boolean = false,
  displayErrorMessage: Boolean = true,
  showShimmerEffectWhenLoading: Boolean = true,
  contentScale: ContentScale = ContentScale.Crop,
  contentDescription: String? = null,
  onClick: (PostImageThumbnailKey) -> Unit,
  onLongClick: (PostImageThumbnailKey) -> Unit
) {
  val context = LocalContext.current
  val kurobaImageLoader = appDependencies().kurobaImageLoader

  var size by remember { mutableStateOf<IntSize?>(null) }

  Box(
    modifier = Modifier
      .kurobaClickable(
        bounded = true,
        onLongClick = { onLongClick(key) },
        onClick = { onClick(key) }
      )
      .then(modifier)
      .background(backgroundColor)
      .onSizeChanged { intSize -> size = intSize }
  ) {
    val imageLoaderResultMut by produceState<ImageLoaderResult>(
      initialValue = ImageLoaderResult.Loading,
      key1 = request,
      key2 = size,
      producer = { loadImage(kurobaImageLoader, context, request, size) }
    )
    val imageLoaderResult = imageLoaderResultMut

    if (imageLoaderResult is ImageLoaderResult.Success) {
      Image(
        modifier = Modifier
          .fillMaxSize(),
        painter = imageLoaderResult.painter,
        contentDescription = contentDescription,
        contentScale = contentScale
      )
    }

    PostImageThumbnailOverlay(
      modifier = Modifier.fillMaxSize(),
      hasAudio = hasAudio,
      displayErrorMessage = displayErrorMessage,
      showShimmerEffectWhenLoading = showShimmerEffectWhenLoading,
      mediaType = mediaType,
      backgroundColor = backgroundColor,
      imageLoaderResult = imageLoaderResult
    )
  }
}

@Composable
fun PostImageThumbnailOverlay(
  modifier: Modifier = Modifier,
  hasAudio: Boolean,
  displayErrorMessage: Boolean,
  showShimmerEffectWhenLoading: Boolean,
  mediaType: KurobaMediaType,
  backgroundColor: Color,
  imageLoaderResult: ImageLoaderResult
) {
  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center
  ) {
    if (imageLoaderResult is ImageLoaderResult.Success) {
      if (mediaType is KurobaMediaType.Video || mediaType is KurobaMediaType.Gif) {
        Row {
          if (mediaType is KurobaMediaType.Video && hasAudio) {
            KurobaComposeIcon(
              drawableId = R.drawable.ic_volume_up_white_24dp,
              iconTint = IconTint.TintWithColor(Color.White)
            )
          } else {
            KurobaComposeIcon(
              drawableId = R.drawable.ic_play_circle_outline_white_24dp,
              iconTint = IconTint.TintWithColor(Color.White)
            )
          }
        }
      }
    } else if (imageLoaderResult is ImageLoaderResult.Error) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        PostImageThumbnailError(
          displayErrorMessage = displayErrorMessage,
          error = imageLoaderResult.throwable
        )
      }
    } else if (showShimmering(imageLoaderResult, showShimmerEffectWhenLoading)) {
      Shimmer(
        modifier = Modifier.fillMaxSize(),
        mainShimmerColor = backgroundColor
      )
    }
  }
}

@Composable
private fun PostImageThumbnailError(
  displayErrorMessage: Boolean,
  error: Throwable
) {
  val appResources = appDependencies().appResources

  Column(
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    KurobaComposeIcon(
      drawableId = R.drawable.ic_baseline_warning_24
    )

    var errorTextMut by remember { mutableStateOf<String?>(null) }
    val errorText = errorTextMut

    LaunchedEffect(error, displayErrorMessage) {
      if (!displayErrorMessage) {
        return@LaunchedEffect
      }

      errorTextMut = when (error) {
        is ExceptionWithShortErrorMessage -> error.shortErrorMessage()
        is SSLException -> appResources.string(R.string.ssl_error)
        is HttpException -> appResources.string(R.string.http_error, error.response.code)
        else -> error::class.java.simpleName
      }
    }

    if (errorText != null) {
      Spacer(modifier = Modifier.height(4.dp))

      KurobaComposeText(
        modifier = Modifier.fillMaxWidth(),
        text = errorText,
        fontSize = 11.ktu.fixedSize(),
        textAlign = TextAlign.Center,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

private fun showShimmering(
  imageLoaderResult: ImageLoaderResult,
  showShimmerEffectWhenLoading: Boolean
): Boolean {
  return (imageLoaderResult is ImageLoaderResult.NotInitialized || imageLoaderResult is ImageLoaderResult.Loading)
    && showShimmerEffectWhenLoading
}
