package com.github.k1rakishou.chan.ui.compose.snackbar

import android.os.SystemClock
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.unit.IntSize
import com.github.k1rakishou.chan.ui.controller.base.ControllerKey
import com.github.k1rakishou.common.iteration


@Stable
class SnackbarState(
  val snackbarManager: SnackbarManager,
) {
  private val _activeSnackbars = mutableStateListOf<SnackbarInfo>()
  val activeSnackbars: List<SnackbarInfo>
    get() = _activeSnackbars
  
  private val _snackbarAnimations = mutableStateMapOf<Long, SnackbarAnimation>()
  val snackbarAnimations: Map<Long, SnackbarAnimation>
    get() = _snackbarAnimations

  private var isSnackbarVisible = false

  fun pushSnackbar(snackbarInfo: SnackbarInfo) {
    val indexOfSnackbar = _activeSnackbars
      .indexOfFirst { info -> info.snackbarId == snackbarInfo.snackbarId }

    if (indexOfSnackbar < 0) {
      Snapshot.withMutableSnapshot {
        _activeSnackbars.add(snackbarInfo)

        val snackbarIdForCompose = snackbarInfo.snackbarIdForCompose
        _snackbarAnimations[snackbarIdForCompose] = SnackbarAnimation.Push(snackbarIdForCompose)
        isSnackbarVisible = true
      }
    } else {
      val prevSnackbarInfo = _activeSnackbars[indexOfSnackbar]
      
      if (prevSnackbarInfo != snackbarInfo || !prevSnackbarInfo.contentsEqual(snackbarInfo)) {
        val snackbarIdForCompose = snackbarInfo.snackbarIdForCompose

        _activeSnackbars[indexOfSnackbar] = snackbarInfo

        if (!isSnackbarVisible) {
          _snackbarAnimations[snackbarIdForCompose] = SnackbarAnimation.Push(snackbarIdForCompose)
          isSnackbarVisible = true
        }
      }
    }
  }

  fun popSnackbar(id: SnackbarId) {
    val indexOfSnackbar = _activeSnackbars
      .indexOfFirst { snackbarInfo -> snackbarInfo.snackbarId == id }

    if (indexOfSnackbar >= 0) {
      val snackbarInfo = _activeSnackbars.getOrNull(indexOfSnackbar) 
        ?: return

      val snackbarIdForCompose = snackbarInfo.snackbarIdForCompose
      _snackbarAnimations[snackbarIdForCompose] = SnackbarAnimation.Pop(snackbarIdForCompose)
      isSnackbarVisible = false
    }
  }

  fun popAllOnControllers(controllerKeys: Set<ControllerKey>) {
    // TODO: New snackbars.
  }

  fun onSnackbarSwipedAway(snackbarIdForCompose: Long) {
    val indexOfSnackbar = _activeSnackbars.indexOfFirst { snackbarInfo ->
      snackbarInfo.snackbarIdForCompose == snackbarIdForCompose
    }

    if (indexOfSnackbar < 0) {
      return
    }

    val snackbarInfo = _activeSnackbars.removeAt(indexOfSnackbar)
    snackbarManager.onSnackbarSwipedAway(snackbarInfo)
  }

  fun onAnimationEnded(snackbarAnimation: SnackbarAnimation) {
    when (snackbarAnimation) {
      is SnackbarAnimation.Push -> {
        // no-op
      }
      is SnackbarAnimation.Pop -> {
        val indexOfSnackbar = _activeSnackbars.indexOfFirst { snackbarInfo -> 
          snackbarInfo.snackbarIdForCompose == snackbarAnimation.snackbarId 
        }
        
        if (indexOfSnackbar >= 0) {
          _activeSnackbars.removeAt(indexOfSnackbar)
        }
      }
    }

    _snackbarAnimations.remove(snackbarAnimation.snackbarId)
  }

  fun removeOldSnackbars() {
    val currentTime = SystemClock.elapsedRealtime()
    var currentSnackbarsCount = _activeSnackbars.size
    val activeSnackbarsCopy = _activeSnackbars.toList()

    activeSnackbarsCopy.iteration { _, activeSnackbar ->
      if (activeSnackbar.aliveUntil != null && currentTime >= activeSnackbar.aliveUntil) {
        val removedSnackbar = SnackbarManager.RemovedSnackbarInfo(activeSnackbar.snackbarId, true)
        popSnackbar(removedSnackbar.snackbarId)

        --currentSnackbarsCount
      }

      return@iteration true
    }
  }

  fun removeSnackbarsExceedingAvailableHeight(
    visibleSnackbarSizeMap: Map<SnackbarId, IntSize>,
    maxAvailableHeight: Int
  ) {
    val activeSnackbarsCount = _activeSnackbars.size

    var totalTakenHeight = visibleSnackbarSizeMap.values
      .sumOf { intSize -> intSize.height }

    for (index in 0 until activeSnackbarsCount) {
      if (totalTakenHeight < maxAvailableHeight) {
        return
      }

      val snackbarToRemove = _activeSnackbars.getOrNull(index)
      if (snackbarToRemove == null) {
        return
      }

      val snackbarHeight = visibleSnackbarSizeMap[snackbarToRemove.snackbarId]?.height
        ?: continue

      val removedSnackbar = SnackbarManager.RemovedSnackbarInfo(snackbarToRemove.snackbarId, true)
      popSnackbar(removedSnackbar.snackbarId)

      totalTakenHeight -= snackbarHeight
    }
  }

}