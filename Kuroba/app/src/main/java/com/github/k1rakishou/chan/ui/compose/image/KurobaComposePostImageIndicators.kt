package com.github.k1rakishou.chan.ui.compose.image

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.ChanSettings
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.core.manager.PrefetchState
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeIcon
import com.github.k1rakishou.chan.ui.compose.providers.LocalChanTheme
import com.github.k1rakishou.chan.utils.appDependencies
import com.github.k1rakishou.common.quantize
import com.github.k1rakishou.model.data.descriptor.ChanDescriptor
import com.github.k1rakishou.model.data.descriptor.PostDescriptor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

private const val CircleMaxSegmentsCount = 32

@Composable
fun KurobaComposePostImageIndicators(
  modifier: Modifier,
  chanDescriptor: ChanDescriptor,
  postDescriptor: PostDescriptor,
  imageFullUrlString: String,
  backgroundAlpha: Float,
  displayDownloadedImageIndicator: Boolean = true,
  displayPrefetchMediaIndicator: Boolean = true,
  displayThirdEyeIndicator: Boolean = true
) {
  var iconsState by remember(key1 = Unit) { mutableStateOf(IconsState()) }
  val iconSize = 18.dp

  AnimatedVisibility(
    visible = iconsState.hasAny(),
    enter = fadeIn(),
    exit = fadeOut()
  ) {
    Row(
      modifier = modifier.then(
        Modifier
          .wrapContentHeight()
          .wrapContentWidth()
          .drawBehind { drawRect(Color.Black.copy(alpha = backgroundAlpha)) }
          .padding(vertical = 2.dp, horizontal = 2.dp)
      ),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      if (iconsState.showImageDownloadedIndicator) {
        ImageDownloadedIndicator(
          iconSize = iconSize
        )
      }

      if (iconsState.showThirdEyeIndicator) {
        ThirdEyeIndicator(
          iconSize = iconSize,
          playThirdEyePulseAnimation = iconsState.playThirdEyePulseAnimation
        )
      }

      if (iconsState.showPrefetchLoadingIndicator) {
        PrefetchIndicator(
          iconSize = iconSize,
          postDescriptor = postDescriptor,
          imageFullUrlString = imageFullUrlString,
          onPrefetchEnded = {
            iconsState = iconsState.copy(showPrefetchLoadingIndicator = false)
          }
        )
      }
    }
  }

  if (displayThirdEyeIndicator) {
    ListenForThirdEyeUpdates(
      chanDescriptor = chanDescriptor,
      postDescriptor = postDescriptor,
      imageFullUrlString = imageFullUrlString,
      onThirdEyeStateChanged = { hasThirdEyeImage -> iconsState = iconsState.copy(showThirdEyeIndicator = hasThirdEyeImage) },
      startOrStopPulseAnimation = { start -> iconsState = iconsState.copy(playThirdEyePulseAnimation = start) }
    )
  }

  if (displayPrefetchMediaIndicator) {
    val chanThreadsCache = appDependencies().chanThreadsCache

    LaunchedEffect(key1 = Unit) {
      val chanPostImage = withContext(Dispatchers.Default) {
        val chanPost = chanThreadsCache.getPost(chanDescriptor, postDescriptor)
        if (chanPost == null) {
          return@withContext null
        }

        return@withContext chanPost.firstPostImageOrNull { chanPostImage ->
          chanPostImage.imageUrl?.toString() == imageFullUrlString
        }
      }

      combine(
        ChanSettings.prefetchMedia.listenForChanges().asFlow(),
        ChanSettings.showPrefetchLoadingIndicator.listenForChanges().asFlow()
      ) { prefetchMedia, showPrefetchLoadingIndicator -> prefetchMedia && showPrefetchLoadingIndicator }
        .onEach { showIndicator ->
          val isAlreadyPrefetched = chanPostImage?.isPrefetched == true
          iconsState = iconsState.copy(showPrefetchLoadingIndicator = showIndicator && !isAlreadyPrefetched)
        }
        .collect()
    }
  }

  if (displayDownloadedImageIndicator) {
    val downloadedImagesManager = appDependencies().downloadedImagesManager

    LaunchedEffect(key1 = Unit) {
      val imageFullUrl = imageFullUrlString.toHttpUrlOrNull()
      if (imageFullUrl == null) {
        return@LaunchedEffect
      }

      try {
        val isImageDownloaded = downloadedImagesManager.isImageDownloaded(
          postDescriptor = postDescriptor,
          imageFullUrl = imageFullUrl
        )

        if (isImageDownloaded) {
          iconsState = iconsState.copy(showImageDownloadedIndicator = true)
        }
      } catch (error: CancellationException) {
        // no-op
      }

      downloadedImagesManager.downloadedImageKeyEventsFlow
        .onEach { downloadedImageKey ->
          if (downloadedImageKey.postDescriptor == postDescriptor && downloadedImageKey.fullImageUrl.toString() == imageFullUrlString) {
            iconsState = iconsState.copy(showImageDownloadedIndicator = true)
          }
        }
        .collect()
    }
  }
}

@Composable
private fun ImageDownloadedIndicator(iconSize: Dp) {
  KurobaComposeIcon(
    modifier = Modifier
      .size(iconSize),
    drawableId = com.github.k1rakishou.chan.R.drawable.ic_file_download_white_24dp
  )
}

@Composable
private fun PrefetchIndicator(
  iconSize: Dp,
  postDescriptor: PostDescriptor,
  imageFullUrlString: String,
  onPrefetchEnded: () -> Unit
) {
  val chanTheme = LocalChanTheme.current

  val prefetchStateManager = appDependencies().prefetchStateManager
  val prefetchProgress = remember { mutableFloatStateOf(1f) }

  LaunchedEffect(key1 = postDescriptor, key2 = imageFullUrlString) {
    prefetchStateManager.listenForPrefetchStateUpdates()
      .asFlow()
      .filter { prefetchState ->
        val postImage = prefetchState.postImage
        return@filter postImage.ownerPostDescriptor == postDescriptor &&
          postImage.imageUrl?.toString() == imageFullUrlString
      }
      .onEach { prefetchState ->
        onPrefetchStateChanged(
          prefetchState = prefetchState,
          onPrefetchStarted = {
            // no-op
          },
          onPrefetchProgressUpdated = { progress ->
            if (prefetchProgress.floatValue != progress) {
              prefetchProgress.floatValue = progress
            }
          },
          onPrefetchEnded = {
            onPrefetchEnded()
          }
        )
      }
      .collect()
  }

  Box(
    modifier = Modifier
      .size(iconSize)
      .drawBehind {
        val currentFilledSegmentsCount = (prefetchProgress.floatValue * CircleMaxSegmentsCount).toInt()
        val anglesPerSegment = 360f / CircleMaxSegmentsCount

        inset(horizontal = 2.dp.toPx(), vertical = 2.dp.toPx()) {
          repeat(currentFilledSegmentsCount) { index ->
            drawArc(
              color = chanTheme.accentColorCompose,
              startAngle = index * anglesPerSegment,
              sweepAngle = anglesPerSegment,
              useCenter = true
            )
          }
        }
      }
  )
}

@Composable
private fun ThirdEyeIndicator(
  iconSize: Dp,
  playThirdEyePulseAnimation: Boolean
) {
  val iconAlpha = remember { androidx.compose.animation.core.Animatable(initialValue = 1f) }

  LaunchedEffect(
    key1 = playThirdEyePulseAnimation
  ) {
    iconAlpha.snapTo(1f)

    while (isActive && playThirdEyePulseAnimation) {
      iconAlpha.animateTo(0.3f, tween(durationMillis = 1000))
      iconAlpha.animateTo(1f, tween(durationMillis = 1000))
    }
  }

  KurobaComposeIcon(
    modifier = Modifier
      .size(iconSize)
      .graphicsLayer { alpha = iconAlpha.value },
    drawableId = R.drawable.ic_baseline_eye_24
  )
}

@Composable
private fun ListenForThirdEyeUpdates(
  chanDescriptor: ChanDescriptor,
  postDescriptor: PostDescriptor,
  imageFullUrlString: String,
  onThirdEyeStateChanged: (Boolean) -> Unit,
  startOrStopPulseAnimation: (Boolean) -> Unit
) {
  val thirdEyeManager = appDependencies().thirdEyeManager
  val chanThreadsCache = appDependencies().chanThreadsCache

  LaunchedEffect(
    key1 = chanDescriptor,
    key2 = postDescriptor,
    key3 = imageFullUrlString
  ) {
    if (!thirdEyeManager.isEnabled()) {
      return@LaunchedEffect
    }

    val imageFullUrl = imageFullUrlString.toHttpUrlOrNull()
    if (imageFullUrl == null) {
      return@LaunchedEffect
    }

    val chanPost = chanThreadsCache.getPost(chanDescriptor, postDescriptor)
    if (chanPost == null) {
      return@LaunchedEffect
    }

    val postImage = chanPost.firstPostImageOrNull { chanPostImage -> chanPostImage.imageUrl == imageFullUrl }
    if (postImage == null) {
      return@LaunchedEffect
    }

    val hasThirdEyeImageMaybe = withContext(Dispatchers.Default) {
      thirdEyeManager.extractThirdEyeHashOrNull(postImage) != null
    }

    onThirdEyeStateChanged(hasThirdEyeImageMaybe)

    val thirdEyeImage = thirdEyeManager.imageForPost(postImage.ownerPostDescriptor)
    if (thirdEyeImage == null && hasThirdEyeImageMaybe) {
      // To avoid running the animation again after we rebind the post which happens after PostLoader
      // finishes it's job.
      startOrStopPulseAnimation(true)
    } else {
      startOrStopPulseAnimation(false)
    }

    thirdEyeManager.thirdEyeImageAddedFlow
      .onCompletion { startOrStopPulseAnimation(false) }
      .collect { postDescriptor ->
        ensureActive()

        if (postImage.ownerPostDescriptor != postDescriptor) {
          return@collect
        }

        val hasThirdEyeImageMaybe = withContext(Dispatchers.Default) {
          thirdEyeManager.extractThirdEyeHashOrNull(postImage) != null
        }

        onThirdEyeStateChanged(hasThirdEyeImageMaybe)
        startOrStopPulseAnimation(false)
      }
  }
}

private fun onPrefetchStateChanged(
  prefetchState: PrefetchState,
  onPrefetchStarted: () -> Unit,
  onPrefetchProgressUpdated: (Float) -> Unit,
  onPrefetchEnded: () -> Unit
) {
  when (prefetchState) {
    is PrefetchState.PrefetchStarted -> {
      onPrefetchStarted()
      onPrefetchProgressUpdated(1f)
    }
    is PrefetchState.PrefetchProgress -> {
      val quantizedProgress = prefetchState.progress.quantize(1f / CircleMaxSegmentsCount.toFloat())
      onPrefetchProgressUpdated(quantizedProgress)
    }
    is PrefetchState.PrefetchCompleted -> {
      onPrefetchProgressUpdated(0f)
      onPrefetchEnded()

      // TODO: high-res cells
//    if (postImage != null && canUseHighResCells) {
//      val thumbnailViewOptions = thumbnail.thumbnailViewOptions
//      val canSwapThumbnailToFullImage = postImage?.imageSpoilered == false || ChanSettings.postThumbnailRemoveImageSpoilers.get()
//
//      if (canSwapThumbnailToFullImage && thumbnailViewOptions != null) {
//        bindPostImage(
//          postImage = postImage!!,
//          canUseHighResCells = canUseHighResCells,
//          thumbnailViewOptions = thumbnailViewOptions,
//          forcedAfterPrefetchFinished = true
//        )
//      }
//    }
    }
  }
}

private data class IconsState(
  val showThirdEyeIndicator: Boolean = false,
  val playThirdEyePulseAnimation: Boolean = false,
  val showPrefetchLoadingIndicator: Boolean = false,
  val showImageDownloadedIndicator: Boolean = false
) {

  fun hasAny(): Boolean {
    return showThirdEyeIndicator || showPrefetchLoadingIndicator || showImageDownloadedIndicator
  }

}