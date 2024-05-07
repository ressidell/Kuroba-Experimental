package com.github.k1rakishou.chan.features.album

import android.os.SystemClock
import com.github.k1rakishou.common.mutableMapWithCap
import com.github.k1rakishou.core_logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ActorScope
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class AutoClearDownloadingAlbumItemState(
  private val viewModelScope: CoroutineScope,
  private val clearDownloadingAlbumItemState: (List<Long>) -> Unit
) {
  private val _downloadingAlbumItemKeys = mutableMapWithCap<Long, Long>(128)

  private val _autoCleanupJob = viewModelScope.actor<Unit>(capacity = 0) {
    for (update in channel) {
      processCurrentKeys()
    }
  }

  fun enqueue(downloadingAlbumItemKey: Long) {
    val prevValue = _downloadingAlbumItemKeys.put(downloadingAlbumItemKey, SystemClock.elapsedRealtime() + 5_000)
    if (prevValue != null) {
      return
    }

    _autoCleanupJob.trySend(Unit)
  }

  private suspend fun ActorScope<Unit>.processCurrentKeys() {
    while (isActive) {
      delay(1000)

      if (_downloadingAlbumItemKeys.isEmpty()) {
        break
      }

      val now = SystemClock.elapsedRealtime()

      val toDelete = _downloadingAlbumItemKeys.entries
        .filter { (_, deleteAt) -> now >= deleteAt }
        .map { (id, _) -> id }

      if (toDelete.isEmpty()) {
        continue
      }

      Logger.verbose(TAG) { "Deleting ${toDelete.size} downloading album item states" }

      toDelete.forEach { albumItemDataId -> _downloadingAlbumItemKeys.remove(albumItemDataId) }
      clearDownloadingAlbumItemState(toDelete)
    }
  }

  companion object {
    private const val TAG = "AutoClearDownloadingAlbumItemState"
  }

}