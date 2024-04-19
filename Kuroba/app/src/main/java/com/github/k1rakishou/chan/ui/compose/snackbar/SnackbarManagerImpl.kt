package com.github.k1rakishou.chan.ui.compose.snackbar

import android.content.Context
import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.ui.controller.base.ControllerKey
import com.github.k1rakishou.chan.ui.globalstate.GlobalUiStateHolder
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class SnackbarManagerImpl(
  private val appContext: Context,
  private val globalUiStateHolder: GlobalUiStateHolder
) : SnackbarManager {
  private val _snackbarEventFlow = MutableSharedFlow<SnackbarInfoEvent>(extraBufferCapacity = Channel.UNLIMITED)
  override val snackbarEventFlow: SharedFlow<SnackbarInfoEvent>
    get() = _snackbarEventFlow.asSharedFlow()

  private val _swipedAwaySnackbars = MutableSharedFlow<SnackbarInfo>(extraBufferCapacity = Channel.UNLIMITED)
  override val swipedAwaySnackbars: SharedFlow<SnackbarInfo>
    get() = _swipedAwaySnackbars.asSharedFlow()

  private val _currentActiveControllers = mutableSetOf<ControllerKey>()

  override fun onControllerCreated(navigatorLevel: Int, controllerKey: ControllerKey) {
    _snackbarEventFlow.tryEmit(SnackbarInfoEvent.RemoveAllForControllerKeys(_currentActiveControllers))
    _currentActiveControllers += controllerKey
  }

  override fun onControllerDestroyed(navigatorLevel: Int, controllerKey: ControllerKey) {
    _snackbarEventFlow.tryEmit(SnackbarInfoEvent.RemoveAllForControllerKeys(setOf(controllerKey)))
    _currentActiveControllers -= controllerKey
  }

  override fun onSnackbarSwipedAway(snackbarInfo: SnackbarInfo) {
    _swipedAwaySnackbars.tryEmit(snackbarInfo)
  }

  override fun snackbar(
    snackbarId: SnackbarId,
    lifetime: Duration?,
    text: SnackbarContentItem.Text,
    button: SnackbarContentItem.Button?
  ): SnackbarId {
    return snackbar(
      snackbarControllerType = SnackbarControllerType.Main,
      snackbarId = snackbarId,
      lifetime = lifetime,
      text = text,
      button = button
    )
  }

  override fun snackbar(
    snackbarControllerType: SnackbarControllerType,
    snackbarId: SnackbarId,
    lifetime: Duration?,
    text: SnackbarContentItem.Text,
    button: SnackbarContentItem.Button?
  ): SnackbarId {
    val now = SystemClock.elapsedRealtime()

    val snackbarInfo = SnackbarInfo(
      snackbarId = snackbarId,
      createdAt = now,
      aliveUntil = lifetime?.inWholeMilliseconds?.plus(now),
      content = buildList {
        add(SnackbarContentItem.Spacer(space = 8.dp))
        add(text)

        if (button != null && button.show) {
          add(SnackbarContentItem.Spacer(space = 8.dp))
          add(button)
        }

        add(SnackbarContentItem.Spacer(space = 8.dp))
      },
      snackbarControllerType = snackbarControllerType
    )

    showSnackbar(snackbarInfo)
    return snackbarId
  }

  // A toast is a snack too
  override fun toast(
    @StringRes messageId: Int,
    toastId: String,
    duration: Duration
  ): SnackbarId {
    return toast(
      message = appContext.getString(messageId),
      toastId = toastId,
      duration = duration
    )
  }

  override fun errorToast(
    @StringRes messageId: Int,
    toastId: String,
    duration: Duration
  ): SnackbarId {
    return errorToast(
      message = appContext.getString(messageId),
      toastId = toastId,
      duration = duration
    )
  }

  override fun toast(
    message: String,
    toastId: String,
    duration: Duration
  ): SnackbarId {
    val snackbarId = toastIdAsSnackbarId(toastId)

    showSnackbar(
      SnackbarInfo(
        snackbarId = snackbarId,
        aliveUntil = SnackbarInfo.snackbarDuration(duration),
        content = listOf(
          SnackbarContentItem.Icon(R.drawable.ic_baseline_clear_24),
          SnackbarContentItem.Spacer(8.dp),
          SnackbarContentItem.Text(
            text = message,
            takeWholeWidth = false
          )
        ),
        snackbarType = SnackbarType.Toast,
        snackbarControllerType = SnackbarControllerType.Main
      )
    )

    return snackbarId
  }

  override fun errorToast(
    message: String,
    toastId: String,
    duration: Duration
  ): SnackbarId {
    val snackbarId = toastIdAsSnackbarId(toastId)

    showSnackbar(
      SnackbarInfo(
        snackbarId = snackbarId,
        aliveUntil = SnackbarInfo.snackbarDuration(duration),
        content = listOf(
          SnackbarContentItem.Icon(R.drawable.ic_baseline_clear_24),
          SnackbarContentItem.Spacer(8.dp),
          SnackbarContentItem.Text(
            text = message,
            takeWholeWidth = false
          )
        ),
        snackbarType = SnackbarType.ErrorToast,
        snackbarControllerType = SnackbarControllerType.Main
      )
    )

    return snackbarId
  }

  override fun hideToast(
    snackbarId: String
  ) {
    _snackbarEventFlow.tryEmit(SnackbarInfoEvent.Pop(id = toastIdAsSnackbarId(snackbarId)))
  }

  override fun showSnackbar(snackbarInfo: SnackbarInfo) {
    _snackbarEventFlow.tryEmit(SnackbarInfoEvent.Push(snackbarInfo))
  }

  override fun dismissSnackbar(snackbarId: SnackbarId) {
    _snackbarEventFlow.tryEmit(SnackbarInfoEvent.Pop(snackbarId))
  }

  override fun onSnackbarCreated(snackbarId: SnackbarId, snackbarControllerType: SnackbarControllerType) {
    globalUiStateHolder.updateSnackbarState {
      updateSnackbarVisibility(snackbarControllerType, true)
    }
  }

  override fun onSnackbarDestroyed(snackbarId: SnackbarId, snackbarControllerType: SnackbarControllerType) {
    globalUiStateHolder.updateSnackbarState {
      updateSnackbarVisibility(snackbarControllerType, false)
    }
  }

  private fun toastIdAsSnackbarId(id: String): SnackbarId {
    return SnackbarId("Toast_${id}")
  }

  private fun standardAliveUntil() = SystemClock.elapsedRealtime() + STANDARD_DURATION.inWholeMilliseconds

  companion object {
    private val TOAST_ID_COUNTER = AtomicLong(0L)
    val SHORT_DURATION = 2000.milliseconds
    val STANDARD_DURATION = 4000.milliseconds
    val LONG_DURATION = 8000.milliseconds

    fun nextToastId(): String = "toast_${TOAST_ID_COUNTER.getAndIncrement()}"
  }

}