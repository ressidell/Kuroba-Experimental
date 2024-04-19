package com.github.k1rakishou.chan.ui.compose.snackbar

import androidx.annotation.StringRes
import com.github.k1rakishou.chan.ui.controller.base.ControllerKey
import kotlinx.coroutines.flow.SharedFlow
import kotlin.time.Duration

interface SnackbarManager {
  val snackbarEventFlow: SharedFlow<SnackbarInfoEvent>
  val swipedAwaySnackbars: SharedFlow<SnackbarInfo>

  fun onControllerCreated(navigatorLevel: Int, controllerKey: ControllerKey)
  fun onControllerDestroyed(navigatorLevel: Int, controllerKey: ControllerKey)
  fun onSnackbarSwipedAway(snackbarInfo: SnackbarInfo)

  fun snackbar(
    snackbarId: SnackbarId,
    lifetime: Duration? = SnackbarManagerImpl.STANDARD_DURATION,
    text: SnackbarContentItem.Text,
    button: SnackbarContentItem.Button? = null
  ): SnackbarId

  fun snackbar(
    snackbarControllerType: SnackbarControllerType,
    snackbarId: SnackbarId,
    lifetime: Duration? = SnackbarManagerImpl.STANDARD_DURATION,
    text: SnackbarContentItem.Text,
    button: SnackbarContentItem.Button? = null
  ): SnackbarId

  fun toast(
    @StringRes messageId: Int,
    toastId: String = SnackbarManagerImpl.nextToastId(),
    duration: Duration = SnackbarManagerImpl.STANDARD_DURATION
  ): SnackbarId

  fun errorToast(
    @StringRes messageId: Int,
    toastId: String = SnackbarManagerImpl.nextToastId(),
    duration: Duration = SnackbarManagerImpl.STANDARD_DURATION
  ): SnackbarId

  fun toast(
    message: String,
    toastId: String = SnackbarManagerImpl.nextToastId(),
    duration: Duration = SnackbarManagerImpl.STANDARD_DURATION
  ): SnackbarId

  fun errorToast(
    message: String,
    toastId: String = SnackbarManagerImpl.nextToastId(),
    duration: Duration = SnackbarManagerImpl.STANDARD_DURATION
  ): SnackbarId

  fun hideToast(
    snackbarId: String
  )

  fun showSnackbar(snackbarInfo: SnackbarInfo)
  fun dismissSnackbar(snackbarId: SnackbarId)

  fun onSnackbarCreated(snackbarId: SnackbarId, snackbarControllerType: SnackbarControllerType)
  fun onSnackbarDestroyed(snackbarId: SnackbarId, snackbarControllerType: SnackbarControllerType)

  class RemovedSnackbarInfo(
    val snackbarId: SnackbarId,
    val timedOut: Boolean
  )
}