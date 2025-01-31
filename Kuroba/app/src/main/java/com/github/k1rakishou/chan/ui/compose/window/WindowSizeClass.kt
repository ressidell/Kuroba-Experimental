package com.github.k1rakishou.chan.ui.compose.window

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Stolen from material3
 *
 * Window size classes are a set of opinionated viewport breakpoints to design, develop, and test
 * responsive application layouts against.
 * For more details check <a href="https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes" class="external" target="_blank">Support different screen sizes</a> documentation.
 *
 * WindowSizeClass contains a [WindowWidthSizeClass] and [WindowHeightSizeClass], representing the
 * window size classes for this window's width and height respectively.
 *
 * See [calculateWindowSizeClass] to calculate the WindowSizeClass for an Activity's current window
 *
 * @property widthSizeClass width-based window size class ([WindowWidthSizeClass])
 * @property heightSizeClass height-based window size class ([WindowHeightSizeClass])
 */
@Immutable
class WindowSizeClass private constructor(
  val widthSizeClass: WindowWidthSizeClass,
  val heightSizeClass: WindowHeightSizeClass
) {
  companion object {
    /**
     * Calculates [WindowSizeClass] for a given [size]. Should be used for testing purposes only
     * - to calculate a [WindowSizeClass] for the Activity's current window see
     * [calculateWindowSizeClass].
     *
     * @param size of the window
     * @return [WindowSizeClass] corresponding to the given width and height
     */
    fun calculateFromSize(size: DpSize): WindowSizeClass {
      val windowWidthSizeClass = WindowWidthSizeClass.fromWidth(size.width)
      val windowHeightSizeClass = WindowHeightSizeClass.fromHeight(size.height)
      return WindowSizeClass(windowWidthSizeClass, windowHeightSizeClass)
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class != other::class) return false

    other as WindowSizeClass

    if (widthSizeClass != other.widthSizeClass) return false
    if (heightSizeClass != other.heightSizeClass) return false

    return true
  }

  override fun hashCode(): Int {
    var result = widthSizeClass.hashCode()
    result = 31 * result + heightSizeClass.hashCode()
    return result
  }

  override fun toString() = "WindowSizeClass($widthSizeClass, $heightSizeClass)"
}

/**
 * Width-based window size class.
 *
 * A window size class represents a breakpoint that can be used to build responsive layouts. Each
 * window size class breakpoint represents a majority case for typical device scenarios so your
 * layouts will work well on most devices and configurations.
 *
 * For more details see <a href="https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes#window_size_classes" class="external" target="_blank">Window size classes documentation</a>.
 */
@Immutable
@kotlin.jvm.JvmInline
value class WindowWidthSizeClass private constructor(private val value: Int) : Comparable<WindowWidthSizeClass> {

  fun asKurobaWindowWidthSizeClass(): KurobaWindowWidthSizeClass {
    return KurobaWindowWidthSizeClass.from(this)
  }

  override operator fun compareTo(other: WindowWidthSizeClass) = value.compareTo(other.value)

  override fun toString(): String {
    return "WindowWidthSizeClass." + when (this) {
      Compact -> "Compact"
      Medium -> "Medium"
      Expanded -> "Expanded"
      else -> ""
    }
  }

  companion object {
    /** Represents the majority of phones in portrait. */
    val Compact = WindowWidthSizeClass(0)

    /**
     * Represents the majority of tablets in portrait and large unfolded inner displays in
     * portrait.
     */
    val Medium = WindowWidthSizeClass(1)

    /**
     * Represents the majority of tablets in landscape and large unfolded inner displays in
     * landscape.
     */
    val Expanded = WindowWidthSizeClass(2)

    const val CompactWindowMaxWidth = 600
    const val MediumWindowMaxWidth = 840

    /** Calculates the [WindowWidthSizeClass] for a given [width] */
    internal fun fromWidth(width: Dp): WindowWidthSizeClass {
      require(width >= 0.dp) { "Width must not be negative" }
      return when {
        width < CompactWindowMaxWidth.dp -> Compact
        width < MediumWindowMaxWidth.dp -> Medium
        else -> Expanded
      }
    }
  }
}

/**
 * Height-based window size class.
 *
 * A window size class represents a breakpoint that can be used to build responsive layouts. Each
 * window size class breakpoint represents a majority case for typical device scenarios so your
 * layouts will work well on most devices and configurations.
 *
 * For more details see <a href="https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes#window_size_classes" class="external" target="_blank">Window size classes documentation</a>.
 */
@Immutable
@kotlin.jvm.JvmInline
value class WindowHeightSizeClass private constructor(private val value: Int) : Comparable<WindowHeightSizeClass> {

  fun asKurobaWindowHeightSizeClass(): KurobaWindowHeightSizeClass {
    return KurobaWindowHeightSizeClass.from(this)
  }

  override operator fun compareTo(other: WindowHeightSizeClass) = value.compareTo(other.value)

  override fun toString(): String {
    return "WindowHeightSizeClass." + when (this) {
      Compact -> "Compact"
      Medium -> "Medium"
      Expanded -> "Expanded"
      else -> ""
    }
  }

  companion object {
    /** Represents the majority of phones in landscape */
    val Compact = WindowHeightSizeClass(0)

    /** Represents the majority of tablets in landscape and majority of phones in portrait */
    val Medium = WindowHeightSizeClass(1)

    /** Represents the majority of tablets in portrait */
    val Expanded = WindowHeightSizeClass(2)

    /** Calculates the [WindowHeightSizeClass] for a given [height] */
    fun fromHeight(height: Dp): WindowHeightSizeClass {
      require(height >= 0.dp) { "Height must not be negative" }
      return when {
        height < 480.dp -> Compact
        height < 900.dp -> Medium
        else -> Expanded
      }
    }

  }
}

enum class KurobaWindowWidthSizeClass(val value: Int) : Comparable<KurobaWindowWidthSizeClass> {
  Compact(0),
  Medium(1),
  Expanded(2);

  companion object {
    fun from(windowWidthSizeClass: WindowWidthSizeClass): KurobaWindowWidthSizeClass {
      return when (windowWidthSizeClass) {
        WindowWidthSizeClass.Compact -> KurobaWindowWidthSizeClass.Compact
        WindowWidthSizeClass.Medium -> KurobaWindowWidthSizeClass.Medium
        WindowWidthSizeClass.Expanded -> KurobaWindowWidthSizeClass.Expanded
        else -> error("Unsupported windowWidthSizeClass: ${windowWidthSizeClass}")
      }
    }
  }

}

enum class KurobaWindowHeightSizeClass(val value: Int) : Comparable<KurobaWindowHeightSizeClass> {
  Compact(0),
  Medium(1),
  Expanded(2);

  companion object {
    fun from(windowHeightSizeClass: WindowHeightSizeClass): KurobaWindowHeightSizeClass {
      return when (windowHeightSizeClass) {
        WindowHeightSizeClass.Compact -> KurobaWindowHeightSizeClass.Compact
        WindowHeightSizeClass.Medium -> KurobaWindowHeightSizeClass.Medium
        WindowHeightSizeClass.Expanded -> KurobaWindowHeightSizeClass.Expanded
        else -> error("Unsupported windowHeightSizeClass: ${windowHeightSizeClass}")
      }
    }
  }

}