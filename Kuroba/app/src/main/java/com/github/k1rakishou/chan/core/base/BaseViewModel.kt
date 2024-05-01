package com.github.k1rakishou.chan.core.base

import androidx.annotation.CallSuper
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.k1rakishou.chan.Chan
import com.github.k1rakishou.chan.core.di.component.viewmodel.ViewModelComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Stable
abstract class BaseViewModel : ViewModel() {

  init {
    Chan.getComponent()
      .viewModelComponentBuilder()
      .build()
      .also { component -> injectDependencies(component) }

    viewModelScope.launch(Dispatchers.Main) {
      onViewModelReady()
    }
  }

  @CallSuper
  override fun onCleared() {

  }

  protected inline fun <T> MutableStateFlow<T>.updateState(crossinline updater: T.() -> T?) {
    update { oldValue ->
      val newValue = updater(oldValue)
      if (newValue == null || oldValue === newValue) {
        return
      }

      return@update newValue
    }
  }

  abstract fun injectDependencies(component: ViewModelComponent)
  abstract suspend fun onViewModelReady()
}