package com.github.k1rakishou.chan.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

fun <T> Flow<T>.bufferWhilePaused(capacity: Int = 16, pause: StateFlow<Boolean>): Flow<T> {
  require(capacity > 0) { "Bad capacity: ${capacity}" }

  return flow {
    val buffer = ArrayList<T>(capacity)

    collect { value ->
      if (pause.value) {
        buffer.add(value)
      } else {
        emit(value)

        buffer.forEach { bufferedValue ->
          emit(bufferedValue)
        }

        buffer.clear()
      }
    }
  }
}