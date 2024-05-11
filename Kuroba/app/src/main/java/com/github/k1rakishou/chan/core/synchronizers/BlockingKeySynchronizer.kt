package com.github.k1rakishou.chan.core.synchronizers

import androidx.annotation.GuardedBy
import androidx.annotation.VisibleForTesting
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class BlockingKeySynchronizer<Key : Any> {

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  @GuardedBy("this")
  val synchronizerMap = WeakHashMap<Key, Synchronizer>()

  private val globalLock = ReentrantLock(true)

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  fun getOrCreate(key: Key): Synchronizer {
    var value = synchronizerMap[key]
    if (value == null) {
      synchronized(this) {
        value = synchronizerMap[key]
        if (value == null) {
          value = Synchronizer()
          synchronizerMap[key] = value
        }
      }
    }

    return value!!
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  fun get(key: Key): Synchronizer? {
    var value = synchronizerMap[key]
    if (value == null) {
      synchronized(this) {
        value = synchronizerMap[key]
      }
    }

    return value
  }

  fun isLocalLockLocked(key: Key): Boolean {
    return get(key)?.isLocked() ?: false
  }

  fun isGlobalLockLocked(): Boolean = globalLock.isLocked

  /**
   * Get keys of all synchronizers that are currently active (Synchronizer.counter > 0)
   * */
  @Synchronized
  fun getHeldLockKeys(): Set<Key> {
    return synchronizerMap.entries
      .filter { (_, synchronizer) -> synchronizer.counter > 0 }
      .map { (key, _) -> key }.toSet()
  }

  fun <T : Any?> withLocalLock(key: Key, func: () -> T): T {
    if (globalLock.isLocked) {
      return globalLock.withLock { getOrCreate(key).withLock { func() } }
    }

    return getOrCreate(key).withLock { func() }
  }

  fun <T : Any?> withGlobalLock(func: () -> T): T {
    return globalLock.withLock { func() }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  class Synchronizer(
    private val lock: ReentrantLock = ReentrantLock(true),
    private val _counter: AtomicLong = AtomicLong(0)
  ) {
    val counter: Long
      get() = _counter.get()

    fun isLocked(): Boolean {
      return lock.isLocked
    }

    fun <T> withLock(block: () -> T): T {
      try {
        _counter.incrementAndGet()
        return lock.withLock(block)
      } finally {
        _counter.decrementAndGet()
      }
    }
  }

}