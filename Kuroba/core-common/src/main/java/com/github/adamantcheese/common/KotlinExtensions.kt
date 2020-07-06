package com.github.adamantcheese.common

import android.view.View
import android.view.ViewGroup
import com.github.adamantcheese.common.ModularResult.Companion.Try
import kotlinx.coroutines.*
import okhttp3.*
import java.io.IOException
import java.util.*
import java.util.regex.Matcher
import kotlin.collections.ArrayList
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun OkHttpClient.suspendCall(request: Request): Response {
  return suspendCancellableCoroutine { continuation ->
    val call = newCall(request)

    continuation.invokeOnCancellation {
      Try { call.cancel() }.ignore()
    }

    call.enqueue(object : Callback {
      override fun onFailure(call: Call, e: IOException) {
        continuation.resumeWithException(e)
      }

      override fun onResponse(call: Call, response: Response) {
        continuation.resume(response)
      }
    })
  }
}

public inline fun <T, R> Iterable<T>.flatMapNotNull(transform: (T) -> Iterable<R>?): List<R> {
  return flatMapNotNullTo(ArrayList<R>(), transform)
}

public inline fun <T, R, C : MutableCollection<in R>> Iterable<T>.flatMapNotNullTo(destination: C, transform: (T) -> Iterable<R>?): C {
  for (element in this) {
    val list = transform(element)

    if (list != null) {
      destination.addAll(list)
    }
  }
  return destination
}

public inline fun <T, R> Collection<T>.flatMapIndexed(transform: (Int, T) -> Collection<R>): List<R> {
  val destination = mutableListOf<R>()

  for ((index, element) in this.withIndex()) {
    val list = transform(index, element)

    destination.addAll(list)
  }

  return destination
}

@Suppress("RedundantAsync")
public suspend fun <T> CoroutineScope.myAsyncSafe(func: suspend () -> T): ModularResult<T> {
  return supervisorScope {
    Try { async { func() }.await() }
  }
}

@Suppress("RedundantAsync")
public suspend fun <T> CoroutineScope.myAsync(func: suspend () -> T): T {
  return supervisorScope {
    async { func() }.await()
  }
}

public inline fun <T> Collection<T>.forEachReverseIndexed(action: (index: Int, T) -> Unit): Unit {
  if (this.isEmpty()) {
    return
  }

  var index = this.size - 1

  for (item in this) {
    action(index--, item)
  }
}

public inline fun <T, R : Any, C : MutableCollection<in R>> Collection<T>.mapReverseIndexedNotNullTo(destination: C, transform: (index: Int, T) -> R?): C {
  forEachReverseIndexed { index, element -> transform(index, element)?.let { destination.add(it) } }
  return destination
}

public inline fun <T, R : Any> Collection<T>.mapReverseIndexedNotNull(transform: (index: Int, T) -> R?): List<R> {
  return mapReverseIndexedNotNullTo(ArrayList<R>(), transform)
}

/**
 * Forces the kotlin compiler to require handling of all branches in the "when" operator
 * */
val <T : Any?> T.exhaustive: T
  get() = this

fun Matcher.groupOrNull(group: Int): String? {
  return try {
    if (group < 0 || group > groupCount()) {
      return null
    }

    this.group(group)
  } catch (error: Throwable) {
    null
  }
}

/**
 * Not thread-safe!
 * */
fun <K, V> MutableMap<K, V>.putIfNotContains(key: K, value: V) {
  if (!this.containsKey(key)) {
    this[key] = value
  }
}

/**
 * Not thread-safe!
 * */
fun <K, V> HashMap<K, V>.putIfNotContains(key: K, value: V) {
  if (!this.containsKey(key)) {
    this[key] = value
  }
}

/**
 * Not thread-safe!
 * */
fun <K, V> TreeMap<K, V>.firstKeyOrNull(): K? {
  if (isEmpty()) {
    return null
  }

  return firstKey()
}

fun Throwable.errorMessageOrClassName(): String {
  if (!message.isNullOrBlank()) {
    return message!!
  }

  return this::class.java.name
}

fun View.updateMargins(
  left: Int? = null,
  right: Int? = null,
  start: Int? = null,
  end: Int? = null,
  top: Int? = null,
  bottom: Int? = null
) {
  val layoutParams = layoutParams as? ViewGroup.MarginLayoutParams
    ?: return

  val newLeft = left ?: layoutParams.leftMargin
  val newRight = right ?: layoutParams.rightMargin
  val newStart = start ?: layoutParams.marginStart
  val newEnd = end ?: layoutParams.marginEnd
  val newTop = top ?: layoutParams.topMargin
  val newBottom = bottom ?: layoutParams.bottomMargin

  layoutParams.setMargins(
    newLeft,
    newTop,
    newRight,
    newBottom
  )

  layoutParams.marginStart = newStart
  layoutParams.marginEnd = newEnd
}

fun View.updatePaddings(
  left: Int? = null,
  right: Int? = null,
  top: Int? = null,
  bottom: Int? = null
) {
  val newLeft = left ?: paddingLeft
  val newRight = right ?: paddingRight
  val newTop = top ?: paddingTop
  val newBottom = bottom ?: paddingBottom

  setPadding(newLeft, newTop, newRight, newBottom)
}

fun View.updatePaddings(
  left: Int = paddingLeft,
  right: Int = paddingRight,
  top: Int = paddingTop,
  bottom: Int = paddingBottom
) {
  setPadding(left, top, right, bottom)
}

fun ViewGroup.findChild(predicate: (View) -> Boolean): View? {
  if (predicate(this)) {
    return this
  }

  return findChildRecursively(this, predicate)
}

private fun findChildRecursively(viewGroup: ViewGroup, predicate: (View) -> Boolean): View? {
  for (index in 0 until viewGroup.childCount) {
    val child = viewGroup.getChildAt(index)
    if (predicate(child)) {
      return child
    }

    if (child is ViewGroup) {
      val result = findChildRecursively(child, predicate)
      if (result != null) {
        return result
      }
    }
  }

  return null
}

fun ViewGroup.findChildren(predicate: (View) -> Boolean): Set<View> {
  val children = hashSetOf<View>()

  if (predicate(this)) {
    children += this
  }

  findChildrenRecursively(children, this, predicate)
  return children
}

fun findChildrenRecursively(children: HashSet<View>, viewGroup: ViewGroup, predicate: (View) -> Boolean) {
  for (index in 0 until viewGroup.childCount) {
    val child = viewGroup.getChildAt(index)
    if (predicate(child)) {
      children += child
    }

    if (child is ViewGroup) {
      findChildrenRecursively(children, child, predicate)
    }
  }
}

fun View.updateHeight(newHeight: Int) {
  val updatedLayoutParams = layoutParams
  updatedLayoutParams.height = newHeight
  layoutParams = updatedLayoutParams
}

fun String.ellipsizeEnd(maxLength: Int): String {
  val minStringLength = 5
  val threeDotsLength = 3

  if (maxLength < minStringLength) {
    return this
  }

  if (this.length <= maxLength) {
    return this
  }

  return this.take(maxLength - threeDotsLength) + "..."
}

suspend fun <T> CompletableDeferred<T>.awaitSilently(defaultValue: T): T {
  try {
    return await()
  } catch (ignored: CancellationException) {
    return defaultValue
  }
}

suspend fun CompletableDeferred<*>.awaitSilently() {
  try {
    await()
  } catch (ignored: CancellationException) {
    // no-op
  }
}

fun View.resetClickListener() {
  setOnClickListener(null)

  // setOnClickListener sets isClickable to true even when the callback is null
  // (which is absolutely not obvious)
  isClickable = false
}