package com.github.k1rakishou.chan.ui.controller.base

import android.content.Context
import android.content.res.Configuration
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.compose.runtime.mutableStateOf
import com.github.k1rakishou.ChanSettings
import com.github.k1rakishou.chan.core.base.ControllerHostActivity
import com.github.k1rakishou.chan.core.di.component.activity.ActivityComponent
import com.github.k1rakishou.chan.features.toolbar.KurobaToolbarState
import com.github.k1rakishou.chan.features.toolbar.KurobaToolbarStateManager
import com.github.k1rakishou.chan.ui.controller.base.transition.FadeTransition
import com.github.k1rakishou.chan.ui.controller.base.transition.TransitionMode
import com.github.k1rakishou.chan.ui.controller.navigation.BottomPanelContract
import com.github.k1rakishou.chan.ui.controller.navigation.DoubleControllerType
import com.github.k1rakishou.chan.ui.controller.navigation.DoubleNavigationController
import com.github.k1rakishou.chan.ui.controller.navigation.NavigationController
import com.github.k1rakishou.chan.ui.controller.navigation.StyledToolbarNavigationController
import com.github.k1rakishou.chan.ui.controller.navigation.ToolbarNavigationController
import com.github.k1rakishou.chan.ui.globalstate.GlobalUiStateHolder
import com.github.k1rakishou.chan.ui.helper.AppResources
import com.github.k1rakishou.chan.ui.view.widget.CancellableToast
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils
import com.github.k1rakishou.common.AndroidUtils
import com.github.k1rakishou.common.DoNotStrip
import com.github.k1rakishou.common.ModularResult
import com.github.k1rakishou.common.errorMessageOrClassName
import com.github.k1rakishou.core_logger.Logger
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@Suppress("LeakingThis")
@DoNotStrip
abstract class Controller(
  @JvmField var context: Context
) {
  lateinit var view: ViewGroup

  @Inject
  lateinit var kurobaToolbarStateManager: KurobaToolbarStateManager
  @Inject
  lateinit var globalUiStateHolder: GlobalUiStateHolder
  @Inject
  lateinit var appResources: AppResources

  open val controllerKey: ControllerKey
    get() = ControllerKey(this::class.java.name)
  open val toolbarState: KurobaToolbarState
    get() = kurobaToolbarStateManager.getOrCreate(controllerKey)
  open var containerToolbarState: KurobaToolbarState
    get() = requireToolbarNavController().containerToolbarState
    set(value) { requireToolbarNavController().containerToolbarState = value }

  @JvmField
  var parentController: Controller? = null
  @JvmField
  var childControllers: MutableList<Controller> = ArrayList()

  // NavigationControllers members
  @JvmField
  var previousSiblingController: Controller? = null
  @JvmField
  var navigationController: NavigationController? = null
  @JvmField
  var doubleNavigationController: DoubleNavigationController? = null
  @JvmField
  var doubleControllerType: DoubleControllerType? = null

  /**
   * Controller that this controller is presented by.
   */
  @JvmField
  var presentedByController: Controller? = null

  /**
   * Controller that this controller is presenting.
   */
  @JvmField
  var presentingThisController: Controller? = null

  val topController: Controller?
    get() = if (childControllers.size > 0) {
      childControllers[childControllers.size - 1]
    } else {
      null
    }

  private val _topControllerState = MutableStateFlow<Controller?>(null)
  val topControllerState: StateFlow<Controller?>
    get() = _topControllerState.asStateFlow()

  private var _alive = false
  val alive: Boolean
    get() = _alive
  private var _shown = false
  val shown: Boolean
    get() = _shown

  protected var compositeDisposable = CompositeDisposable()
    @JvmName("compositeDisposable") get
    private set

  private val job = SupervisorJob()
  protected var controllerScope = CoroutineScope(job + Dispatchers.Main + CoroutineName("Controller_${this::class.java.simpleName}"))

  protected val cancellableToast = CancellableToast()

  private val _navigationFlags = mutableStateOf<DeprecatedNavigationFlags>(DeprecatedNavigationFlags())
  val hasDrawer: Boolean
    get() = _navigationFlags.value.hasDrawer == true
  val hasBack: Boolean
    get() = _navigationFlags.value.hasBack == true
  val swipeable: Boolean
    get() = _navigationFlags.value.swipeable == true

  fun updateNavigationFlags(newNavigationFlags: DeprecatedNavigationFlags) {
    _navigationFlags.value = newNavigationFlags
  }

  fun controllerViewOrNull(): ViewGroup? {
    if (::view.isInitialized) {
      return view
    }

    return null
  }

  fun requireToolbarNavController(): ToolbarNavigationController {
    if (this is ToolbarNavigationController) {
      return this
    }

    val navController = requireNavController()
    check(navController is ToolbarNavigationController) {
      "Expected navController to be 'ToolbarNavigationController' but got '${navController::class.java.name}'"
    }

    return navController
  }

  fun requireBottomPanelContract(): BottomPanelContract {
    return requireToolbarNavController() as BottomPanelContract
  }

  fun requireNavController(): NavigationController {
    if (this is NavigationController) {
      return this
    }

    return requireNotNull(navigationController) { "navigationController was not set" }
  }

  fun requireComponentActivity(): ComponentActivity {
    return (context as? ComponentActivity)
      ?: throw IllegalStateException("Wrong context! Must be ComponentActivity")
  }

  fun isViewInitialized(): Boolean = ::view.isInitialized

  init {
    injectDependencies(AppModuleAndroidUtils.extractActivityComponent(context))
  }

  protected abstract fun injectDependencies(component: ActivityComponent)

  @CallSuper
  open fun onCreate() {
    _alive = true

    if (LOG_STATES) {
      Logger.e("LOG_STATES", javaClass.simpleName + " onCreate")
    }
  }

  @CallSuper
  open fun onShow() {
    _shown = true

    if (LOG_STATES) {
      Logger.e("LOG_STATES", javaClass.simpleName + " onShow")
    }

    view.visibility = View.VISIBLE

    for (controller in childControllers) {
      if (!controller.shown) {
        controller.onShow()
      }
    }
  }

  @CallSuper
  open fun onHide() {
    _shown = false

    if (LOG_STATES) {
      Logger.e("LOG_STATES", javaClass.simpleName + " onHide")
    }

    view.visibility = View.GONE

    for (controller in childControllers) {
      if (controller.shown) {
        controller.onHide()
      }
    }
  }

  @CallSuper
  open fun onDestroy() {
    _alive = false
    compositeDisposable.clear()
    job.cancelChildren()

    if (LOG_STATES) {
      Logger.e("LOG_STATES", javaClass.simpleName + " onDestroy")
    }

    while (childControllers.size > 0) {
      removeChildController(childControllers[0])
    }

    if (::view.isInitialized && AndroidUtils.removeFromParentView(view)) {
      if (LOG_STATES) {
        Logger.e("LOG_STATES", javaClass.simpleName + " view removed onDestroy")
      }
    }
  }

  fun addChildController(controller: Controller) {
    childControllers.add(controller)
    controller.parentController = this

    if (doubleNavigationController != null) {
      controller.doubleNavigationController = doubleNavigationController
    }

    if (navigationController != null) {
      controller.navigationController = navigationController
    }

    if (controller is DoubleNavigationController) {
      controller.leftControllerToolbarState?.init()
      controller.rightControllerToolbarState?.init()
    } else {
      controller.toolbarState.init()
    }

    controller.onCreate()

    if (controller.navigationController is StyledToolbarNavigationController) {
      (controller.navigationController as StyledToolbarNavigationController).onChildControllerPushed(controller)
    }

    _topControllerState.value = topController
  }

  fun removeChildController(controller: Controller?) {
    if (controller == null) {
      return
    }

    controller.onDestroy()
    childControllers.remove(controller)

    if (controller.navigationController is StyledToolbarNavigationController) {
      (controller.navigationController as StyledToolbarNavigationController).onChildControllerPopped(controller)
    }

    if (controller is DoubleNavigationController) {
      controller.leftControllerToolbarState?.destroy()
      controller.rightControllerToolbarState?.destroy()
    } else {
      controller.toolbarState.destroy()
    }

    kurobaToolbarStateManager.remove(controller.controllerKey)
    _topControllerState.value = topController
  }

  fun attachToParentView(parentView: ViewGroup?) {
    if (view.parent != null) {
      if (LOG_STATES) {
        Logger.e("LOG_STATES", javaClass.simpleName + " view removed")
      }

      AndroidUtils.removeFromParentView(view)
    }

    if (parentView != null) {
      if (LOG_STATES) {
        Logger.e("LOG_STATES", javaClass.simpleName + " view attached")
      }

      attachToView(parentView)
    }
  }

  open fun onConfigurationChanged(newConfig: Configuration) {
    for (controller in childControllers) {
      controller.onConfigurationChanged(newConfig)
    }
  }

  open fun dispatchKeyEvent(event: KeyEvent): Boolean {
    for (i in childControllers.indices.reversed()) {
      val controller = childControllers[i]
      if (controller.dispatchKeyEvent(event)) {
        return true
      }
    }

    return false
  }

  open fun onBack(): Boolean {
    for (index in childControllers.indices.reversed()) {
      val controller = childControllers[index]
      if (controller.onBack()) {
        return true
      }
    }
    return false
  }

  @JvmOverloads
  open fun presentController(controller: Controller, animated: Boolean = true) {
    val contentView = (context as ControllerHostActivity).contentView
    presentingThisController = controller

    controller.presentedByController = this
    (context as ControllerHostActivity).pushController(controller)

    controller.onCreate()
    controller.attachToView(contentView)
    controller.onShow()

    if (animated) {
      val transition = FadeTransition(transitionMode = TransitionMode.In)
      transition.to = controller
      transition.perform()

      return
    }
  }

  fun isAlreadyPresenting(predicate: (Controller) -> Boolean): Boolean {
    return (context as ControllerHostActivity).isControllerAdded(predicate)
  }

  fun getControllerOrNull(predicate: (Controller) -> Boolean): Controller? {
    return (context as ControllerHostActivity).getControllerOrNull(predicate)
  }

  open fun stopPresenting() {
    stopPresenting(true)
  }

  open fun stopPresenting(animated: Boolean) {
    val startActivity = (context as ControllerHostActivity)
    if (!startActivity.containsController(this)) {
      return
    }

    if (animated) {
      val transition = FadeTransition(transitionMode = TransitionMode.Out)
      transition.from = this
      transition.onTransitionFinished { finishPresenting() }
      transition.perform()
    } else {
      finishPresenting()
    }

    startActivity.popController(this)
    presentedByController?.presentingThisController = null
  }

  @JvmOverloads
  protected fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    cancellableToast.showToast(context, message, duration)
  }

  @JvmOverloads
  protected fun showToast(@StringRes messageId: Int, duration: Int = Toast.LENGTH_SHORT) {
    cancellableToast.showToast(context, messageId, duration)
  }

  private fun finishPresenting() {
    onHide()
    onDestroy()
  }

  private fun attachToView(parentView: ViewGroup) {
    var params = view.layoutParams
    if (params == null) {
      params = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
    } else {
      params.width = ViewGroup.LayoutParams.MATCH_PARENT
      params.height = ViewGroup.LayoutParams.MATCH_PARENT
    }

    view.layoutParams = params
    parentView.addView(view, view.layoutParams)
  }

  fun withLayoutMode(
    phone: (() -> Unit)? = null,
    tablet: (() -> Unit)? = null
  ) {
    if (ChanSettings.isSplitLayoutMode()) {
      tablet?.invoke()
    } else {
      phone?.invoke()
    }
  }

  protected fun <T> ModularResult<T>.toastOnError(
    longToast: Boolean = false,
    message: ((Throwable) -> String)? = null
  ): ModularResult<T> {
    when (this) {
      is ModularResult.Error -> {
        error.toastOnError(longToast, message)
      }
      is ModularResult.Value -> {
        // no-op
      }
    }

    return this
  }

  protected fun Throwable.toastOnError(
    longToast: Boolean = false,
    message: ((Throwable) -> String)? = null
  ) {
    val message = message?.invoke(this)
      ?: this.errorMessageOrClassName()

    val duration = if (longToast) {
      Toast.LENGTH_LONG
    } else {
      Toast.LENGTH_SHORT
    }

    showToast(message, duration)
  }

  protected fun <T> ModularResult<T>.toastOnSuccess(
    longToast: Boolean = false,
    message: () -> String
  ): ModularResult<T> {
    when (this) {
      is ModularResult.Error -> {
        // no-op
      }
      is ModularResult.Value -> {
        val duration = if (longToast) {
          Toast.LENGTH_LONG
        } else {
          Toast.LENGTH_SHORT
        }

        showToast(message(), duration)
      }
    }

    return this
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Controller

    return controllerKey == other.controllerKey
  }

  override fun hashCode(): Int {
    return javaClass.hashCode()
  }

  override fun toString(): String {
    return "Controller(controllerKey=$controllerKey)"
  }

  companion object {
    private const val LOG_STATES = false
  }

}