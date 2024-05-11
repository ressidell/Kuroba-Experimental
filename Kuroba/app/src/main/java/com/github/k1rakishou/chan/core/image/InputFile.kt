package com.github.k1rakishou.chan.core.image

import android.net.Uri
import com.github.k1rakishou.common.StringUtils
import java.io.File

sealed class InputFile {

  fun extension(): String? {
    when (this) {
      is FileUri -> {
        return uri.lastPathSegment
          ?.let { lastPathSegment -> StringUtils.extractFileNameExtension(lastPathSegment) }
      }
      is JavaFile -> {
        return StringUtils.extractFileNameExtension(file.name)
      }
    }
  }

  fun fileName(): String? {
    when (this) {
      is FileUri -> {
        return uri.lastPathSegment
      }
      is JavaFile -> {
        return file.name
      }
    }
  }

  fun path(): String {
    when (this) {
      is FileUri -> {
        return uri.toString()
      }
      is JavaFile -> {
        return file.absolutePath
      }
    }
  }

  data class JavaFile(val file: File) : InputFile() {
    val filePath = file.absolutePath

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as JavaFile

      return filePath == other.filePath
    }

    override fun hashCode(): Int {
      return filePath.hashCode() ?: 0
    }

    override fun toString(): String {
      return "JavaFile(filePath='$filePath')"
    }

  }

  class FileUri(val uri: Uri) : InputFile() {
    val uriString = uri.toString()

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as FileUri

      return uriString == other.uriString
    }

    override fun hashCode(): Int {
      return uriString.hashCode()
    }

    override fun toString(): String {
      return "FileUri(uriString=$uriString)"
    }
  }
}