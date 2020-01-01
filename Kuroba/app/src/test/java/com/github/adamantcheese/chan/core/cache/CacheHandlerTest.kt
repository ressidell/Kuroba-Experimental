package com.github.adamantcheese.chan.core.cache

import com.github.adamantcheese.chan.utils.AndroidUtils
import com.github.k1rakishou.fsaf.BadPathSymbolResolutionStrategy
import com.github.k1rakishou.fsaf.FileManager
import com.github.k1rakishou.fsaf.file.RawFile
import com.github.k1rakishou.fsaf.manager.base_directory.DirectoryManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CacheHandlerTest {
    private lateinit var cacheHandler: CacheHandler
    private lateinit var fileManager: FileManager
    private lateinit var cacheDirFile: RawFile
    private lateinit var chunksCacheDirFile: RawFile

    @Before
    fun init() {
        val context = RuntimeEnvironment.application.applicationContext
        AndroidUtils.init(RuntimeEnvironment.application)

        fileManager = FileManager(
                context,
                BadPathSymbolResolutionStrategy.ThrowAnException,
                DirectoryManager()
        )

        cacheDirFile = fileManager.fromRawFile(File(context.cacheDir, "cache_dir"))
        assertNotNull(fileManager.create(cacheDirFile))
        assertTrue(fileManager.deleteContent(cacheDirFile))

        chunksCacheDirFile = fileManager.fromRawFile(File(context.cacheDir, "chunks_cache_dir"))
        assertNotNull(fileManager.create(chunksCacheDirFile))
        assertTrue(fileManager.deleteContent(chunksCacheDirFile))

        cacheHandler = CacheHandler(
                fileManager,
                cacheDirFile,
                chunksCacheDirFile,
                false
        )
    }

    @After
    fun tearDown() {
        cacheHandler.clearCache()
    }

    @Test
    fun `simple test create new cache file and mark it as downloaded`() {
        val url = "http://4chan.org/image.jpg"
        val cacheFile = checkNotNull(cacheHandler.getOrCreateCacheFile(url))
        assertFalse(cacheHandler.isAlreadyDownloaded(cacheFile))

        assertTrue(cacheHandler.markFileDownloaded(cacheFile))
        assertTrue(cacheHandler.isAlreadyDownloaded(cacheFile))
    }

    @Test
    fun `test create new cache file and malform cache file meta should delete both files`() {
        val url = "http://4chan.org/image.jpg"
        val cacheFile = checkNotNull(cacheHandler.getOrCreateCacheFile(url))
        val cacheFileMeta = cacheHandler.getCacheFileMetaInternal(url)
        val fileLength = fileManager.getLength(cacheFileMeta)

        checkNotNull(fileManager.getInputStream(cacheFileMeta)).use { inputStream ->
            val array = ByteArray(fileLength.toInt())
            inputStream.read(array)

            checkNotNull(fileManager.getOutputStream(cacheFileMeta)).use { outputStream ->
                // Malform the "True/False" boolean parameter by replacing it's last character with
                // comma
                array[array.lastIndex] = ','.toByte()

                outputStream.write(array)
                outputStream.flush()
            }
        }

        assertFalse(cacheHandler.markFileDownloaded(cacheFile))
        assertFalse(cacheHandler.isAlreadyDownloaded(cacheFile))
        assertTrue(fileManager.listFiles(cacheDirFile).isEmpty())
    }
}