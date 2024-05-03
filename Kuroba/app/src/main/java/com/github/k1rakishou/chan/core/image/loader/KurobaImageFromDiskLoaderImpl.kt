package com.github.k1rakishou.chan.core.image.loader

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.memory.MemoryCache
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Dimension
import coil.size.Scale
import coil.transform.Transformation
import com.github.k1rakishou.chan.core.cache.CacheFileType
import com.github.k1rakishou.chan.core.cache.CacheHandler
import com.github.k1rakishou.chan.core.image.InputFile
import com.github.k1rakishou.chan.core.manager.ThreadDownloadManager
import com.github.k1rakishou.chan.utils.BackgroundUtils
import com.github.k1rakishou.chan.utils.MediaUtils
import com.github.k1rakishou.chan.utils.lifecycleFromContextOrNull
import com.github.k1rakishou.common.ModularResult
import com.github.k1rakishou.common.errorMessageOrClassName
import com.github.k1rakishou.common.rethrowCancellationException
import com.github.k1rakishou.core_logger.Logger
import com.github.k1rakishou.fsaf.FileManager
import com.github.k1rakishou.model.data.descriptor.PostDescriptor
import dagger.Lazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import kotlin.time.measureTimedValue

class KurobaImageFromDiskLoaderImpl(
  private val cacheHandlerLazy: Lazy<CacheHandler>,
  private val threadDownloadManagerLazy: Lazy<ThreadDownloadManager>,
  private val fileManagerLazy: Lazy<FileManager>,
  private val coilImageLoaderLazy: Lazy<ImageLoader>
) : KurobaImageFromDiskLoader {
  private val cacheHandler: CacheHandler
    get() = cacheHandlerLazy.get()
  private val threadDownloadManager: ThreadDownloadManager
    get() = threadDownloadManagerLazy.get()
  private val fileManager: FileManager
    get() = fileManagerLazy.get()
  private val coilImageLoader: ImageLoader
    get() = coilImageLoaderLazy.get()

  override suspend fun isImageCachedLocally(cacheFileType: CacheFileType, url: String): Boolean {
    return withContext(Dispatchers.IO) {
      return@withContext runInterruptible {
        val exists = cacheHandler.cacheFileExists(cacheFileType, url)
        val downloaded = cacheHandler.isAlreadyDownloaded(cacheFileType, url)

        return@runInterruptible exists && downloaded
      }
    }
  }

  override suspend fun loadFromDisk(
    context: Context,
    inputFile: InputFile,
    imageSize: KurobaImageSize,
    scale: Scale,
    transformations: List<Transformation>
  ): ModularResult<BitmapDrawable> {
    return ModularResult.Try {
      Logger.verbose(TAG) { "loadFromDisk() inputFilePath: ${inputFile.path()}, imageSize: ${imageSize}" }

      val fileName = inputFile.fileName()
      if (fileName == null) {
        throw KurobaImageLoaderException("Input file has no name")
      }

      val bitmapDrawable = getBitmapDrawable(
        context = context,
        fileName = fileName,
        inputFile = inputFile,
        imageSize = imageSize,
        scale = scale,
        transformations = transformations
      )
        .onError { throwable ->
          Logger.error(TAG) {
            "loadFromDisk() inputFilePath: '${inputFile.path()}', imageSize: ${imageSize} error or canceled " +
              "(throwable: ${throwable.errorMessageOrClassName()})"
          }
        }
        .onSuccess {
          Logger.verbose(TAG) {
            "loadFromDisk() inputFilePath: ${inputFile.path()}, imageSize: ${imageSize} success"
          }
        }
        .unwrap()

      return@Try bitmapDrawable
    }
  }

  override suspend fun tryToLoadFromDisk(
    context: Context,
    url: String,
    postDescriptor: PostDescriptor?,
    cacheFileType: CacheFileType,
    imageSize: KurobaImageSize,
    scale: Scale,
    transformations: List<Transformation>
  ): ModularResult<BitmapDrawable?> {
    return withContext(Dispatchers.IO) {
      return@withContext ModularResult.Try {
        val httpUrl = url.toHttpUrlOrNull()
        if (postDescriptor != null && httpUrl != null) {
          val foundFile = threadDownloadManager.findDownloadedFile(
            httpUrl = httpUrl,
            threadDescriptor = postDescriptor.threadDescriptor()
          )

          if (foundFile != null) {
            val fileLength = fileManager.getLength(foundFile)
            val canRead = fileManager.canRead(foundFile)
            val fileName = fileManager.getName(foundFile)

            if (fileLength > 0 && canRead && fileName.isNotEmpty()) {
              val fileUri = Uri.parse(foundFile.getFullPath())

              return@Try getBitmapDrawable(
                context = context,
                fileName = fileName,
                inputFile = InputFile.FileUri(uri = fileUri),
                imageSize = imageSize,
                scale = scale,
                transformations = transformations
              ).unwrap()
            }
          }

          // fallthrough
        }

        val cacheFile = cacheHandler.getCacheFileOrNull(cacheFileType, url)
        if (cacheFile == null) {
          return@Try null
        }

        if (!cacheFile.exists() || cacheFile.length() == 0L) {
          return@Try null
        }

        return@Try getBitmapDrawable(
          context = context,
          fileName = cacheFile.name,
          inputFile = InputFile.JavaFile(file = cacheFile),
          imageSize = imageSize,
          scale = scale,
          transformations = transformations
        ).unwrap()
      }
    }
  }

  private suspend fun getBitmapDrawable(
    context: Context,
    fileName: String,
    inputFile: InputFile,
    imageSize: KurobaImageSize,
    scale: Scale,
    transformations: List<Transformation>
  ): ModularResult<BitmapDrawable> {
    val appContext = context.applicationContext

    return ModularResult.Try {
      BackgroundUtils.ensureBackgroundThread()

      val videoFrameBitmap = withContext(Dispatchers.IO) {
        if (!fileIsProbablyVideoInterruptible(appContext, fileName, inputFile)) {
          return@withContext null
        }

        val (width, height) = checkNotNull(imageSize.size())

        val key = MemoryCache.Key(inputFile.path())
        val fromCache = coilImageLoader.memoryCache?.get(key)?.bitmap
        if (fromCache != null) {
          return@withContext BitmapDrawable(context.resources, fromCache)
        }

        val decoded = decodedFilePreview(
          isProbablyVideo = true,
          inputFile = inputFile,
          context = context,
          width = width,
          height = height,
          scale = scale,
          addAudioIcon = false
        ).unwrap()

        coilImageLoader.memoryCache?.set(key, MemoryCache.Value(decoded.bitmap))
        return@withContext decoded
      }

      if (videoFrameBitmap != null) {
        return@Try videoFrameBitmap
      }

      val request = with(ImageRequest.Builder(context)) {
        when (inputFile) {
          is InputFile.JavaFile -> data(inputFile.file)
          is InputFile.FileUri -> data(inputFile.uri)
        }

        lifecycle(context.lifecycleFromContextOrNull())
        transformations(transformations)
        scale(scale)
        applyImageSize(imageSize)

        build()
      }

      when (val imageResult = coilImageLoader.execute(request)) {
        is SuccessResult -> {
          val bitmap = imageResult.drawable.toBitmap()
          return@Try BitmapDrawable(context.resources, bitmap)
        }
        is ErrorResult -> {
          throw imageResult.throwable
        }
      }
    }
  }

  private suspend fun decodedFilePreview(
    isProbablyVideo: Boolean,
    inputFile: InputFile,
    context: Context,
    width: Dimension,
    height: Dimension,
    scale: Scale,
    addAudioIcon: Boolean
  ): ModularResult<BitmapDrawable> {
    return ModularResult.Try {
      BackgroundUtils.ensureBackgroundThread()

      if (isProbablyVideo) {
        val videoFrameDecodeMaybe = ModularResult.Try {
          return@Try measureTimedValue {
            return@measureTimedValue MediaUtils.decodeVideoFilePreviewImageInterruptible(
              context = context,
              inputFile = inputFile,
              maxWidth = width,
              maxHeight = height,
              addAudioIcon = addAudioIcon
            )
          }
        }

        if (videoFrameDecodeMaybe is ModularResult.Value) {
          val (videoFrame, decodeVideoFilePreviewImageDuration) = videoFrameDecodeMaybe.value
          Logger.d(TAG, "decodeVideoFilePreviewImageInterruptible duration=$decodeVideoFilePreviewImageDuration")

          if (videoFrame != null) {
            return@Try videoFrame
          }
        }

        videoFrameDecodeMaybe.errorOrNull()
          ?.rethrowCancellationException()

        // Failed to decode the file as video let's try decoding it as an image
      }

      val fileImagePreviewMaybe = getFileImagePreview(
        context = context,
        inputFile = inputFile,
        transformations = emptyList(),
        scale = scale,
        width = width,
        height = height
      )

      fileImagePreviewMaybe.errorOrNull()
        ?.rethrowCancellationException()

      // Do not recycle bitmaps that are supposed to always stay in memory
      return fileImagePreviewMaybe
    }
  }

  private suspend fun getFileImagePreview(
    context: Context,
    inputFile: InputFile,
    transformations: List<Transformation>,
    scale: Scale,
    width: Dimension,
    height: Dimension
  ): ModularResult<BitmapDrawable> {
    return ModularResult.Try {
      val lifecycle = context.lifecycleFromContextOrNull()

      val request = with(ImageRequest.Builder(context)) {
        when (inputFile) {
          is InputFile.FileUri -> data(inputFile.uri)
          is InputFile.JavaFile -> data(inputFile.file)
        }

        lifecycle(lifecycle)
        transformations(transformations)
        scale(scale)
        size(width, height)

        build()
      }

      when (val imageResult = coilImageLoader.execute(request)) {
        is SuccessResult -> return@Try imageResult.drawable as BitmapDrawable
        is ErrorResult -> throw imageResult.throwable
      }
    }
  }

  companion object {
    private const val TAG = "KurobaImageFromDiskLoaderImpl"
  }

}