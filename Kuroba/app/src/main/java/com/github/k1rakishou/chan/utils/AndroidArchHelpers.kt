package com.github.k1rakishou.chan.utils

import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.os.bundleOf
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.github.k1rakishou.chan.core.di.module.shared.IHasViewModelProviderFactory
import com.github.k1rakishou.chan.ui.controller.base.Controller
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.isAccessible

private const val TAG = "AndroidArchHelpers"
const val ControllerParams = "controller_params"

interface IHasViewModelScope {
  val viewModelScope: ViewModelScope
}

sealed interface ViewModelScope {
  val viewModelStore: ViewModelStore
    get() {
      return when (this) {
        is ActivityScope -> activity.viewModelStore
        is ControllerScope -> controller.viewModelStore
      }
    }

  class ActivityScope(
    val activity: ComponentActivity,
  ) : ViewModelScope

  class ControllerScope(
    val controller: Controller,
  ) : ViewModelScope
}

@Composable
inline fun <reified VM : ViewModel> IHasViewModelScope.rememberViewModel(
  key: String? = null,
  noinline params: (() -> Parcelable?)? = null
): VM {
  return remember(key1 = VM::class.java, key2 = key) {
    viewModelByKeyEager<VM>(
      key = key,
      params = params
    )
  }
}

inline fun <reified VM : ViewModel> IHasViewModelScope.viewModelByKeyEager(
  key: String? = null,
  noinline params: (() -> Parcelable?)? = null
): VM {
  return with(viewModelScope) { viewModelByKey(key, params, VM::class.java) }
}

inline fun <reified VM : ViewModel> IHasViewModelScope.viewModelByKey(
  key: String? = null,
  noinline params: (() -> Parcelable?)? = null
): Lazy<VM> {
  return lazy(LazyThreadSafetyMode.NONE) {
    return@lazy with(viewModelScope) { viewModelByKey(key, params, VM::class.java) }
  }
}

@PublishedApi
internal fun <VM : ViewModel> ViewModelScope.viewModelByKey(
  key: String? = null,
  params: (() -> Parcelable?)? = null,
  clazz: Class<VM>
): VM {
  val viewModelProviderFactory = when (this) {
    is ViewModelScope.ActivityScope -> {
      (this.activity as? IHasViewModelProviderFactory)
        ?: throw IllegalStateException("Activity (${this.activity::class.java.name}) is not an instance of IHasViewModelProviderFactory")
    }
    is ViewModelScope.ControllerScope -> {
      this.controller
    }
  }

  val viewModelFactory = viewModelProviderFactory.viewModelFactory
  val factory = viewModelFactory as? AbstractSavedStateViewModelFactory
    ?: throw IllegalStateException("ViewModelFactory (${viewModelFactory}) is not an instance of AbstractSavedStateViewModelFactory")

  val defaultArgs = params?.let { paramsFunc ->
    val paramsData = paramsFunc()
    return@let bundleOf(ControllerParams to paramsData)
  }

  factory.updateDefaultArgs(defaultArgs)

  val viewModelProvider = ViewModelProvider(viewModelStore, factory)
  return if (key != null) {
    viewModelProvider.get(key, clazz)
  } else {
    viewModelProvider.get(clazz)
  }
}

private fun AbstractSavedStateViewModelFactory.updateDefaultArgs(newArgs: Bundle?) {
  val kClass = this::class
  var superClass: KClass<*>? = kClass.superclasses.firstOrNull()
  var defaultArgsField: KProperty1<out Any, *>? = null

  while (true) {
    if (superClass == null) {
      error("Superclass not found. (kClass: ${kClass.simpleName})")
    }

    defaultArgsField = superClass.memberProperties.firstOrNull { it.name == "defaultArgs" }
    if (defaultArgsField != null) {
      break
    }

    val nextSuperClass = superClass.superclasses.firstOrNull()
    if (nextSuperClass == null) {
      throw NoSuchFieldException("Superclass not found. " +
        "(kClass: ${kClass.simpleName}, nextSuperClass: ${superClass::simpleName})")
    }

    superClass = nextSuperClass
  }

  if (defaultArgsField == null) {
    error("defaultArgsField is null")
  }

  if (defaultArgsField is KMutableProperty<*>) {
    defaultArgsField.isAccessible = true

    @Suppress("UNCHECKED_CAST")
    val mutableDefaultArgs = defaultArgsField as KMutableProperty1<Any, Bundle?>
    mutableDefaultArgs.set(this, newArgs)
  } else {
    throw IllegalAccessException("Field 'defaultArgs' is not a var or is inaccessible.")
  }
}

fun <T : Parcelable> SavedStateHandle.paramsOrNull(): T? {
  return get<T>(ControllerParams)
}

inline fun <reified T : Parcelable> SavedStateHandle.requireParams(): T {
  return requireNotNull(paramsOrNull<T>()) { "Params were not passed: ${T::class.java.simpleName}" }
}