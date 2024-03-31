package com.github.k1rakishou.common

import org.junit.Assert
import org.junit.Test
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class QuantizeFloatTest {
  private val EPSILON = 0.001

  @Test
  fun quantizePositive() {
    kotlin.run {
      val res = 0.0f.quantize(0.025f)
      Assert.assertTrue("res=$res", (0.0f - res).absoluteValue <= EPSILON)
    }

    kotlin.run {
      val res = 1.0f.quantize(0.025f)
      Assert.assertTrue("res=$res", (1.0f - res).absoluteValue <= EPSILON)
    }

    kotlin.run {
      val res = 0.025f.quantize(0.025f)
      Assert.assertTrue("res=$res", (0.025f - res).absoluteValue <= EPSILON)
    }

    kotlin.run {
      val res = 0.333f.quantize(0.025f)
      Assert.assertTrue("res=$res", (0.325f - res).absoluteValue <= EPSILON)
    }

    kotlin.run {
      val res = 0.351f.quantize(0.025f)
      Assert.assertTrue("res=$res", (0.35f - res).absoluteValue <= EPSILON)
    }

    kotlin.run {
      val res = 1000f.quantize(25f)
      Assert.assertTrue("res=$res", (1000f - res).absoluteValue <= EPSILON)
    }

    kotlin.run {
      val res = 974f.quantize(25f)
      Assert.assertEquals(975, res.roundToInt())
    }

    kotlin.run {
      val res = 1000f.quantize(25f)
      Assert.assertEquals(1000, res.roundToInt())
    }
  }

  @Test
  fun quantizeNegative() {
    kotlin.run {
      val res = (-0.0f).quantize(0.025f)
      Assert.assertTrue("res=$res", ((-0.0f) - res).absoluteValue <= EPSILON)
    }

    kotlin.run {
      val res = (-1.0f).quantize(0.025f)
      Assert.assertTrue("res=$res", ((-1.0f) - res).absoluteValue <= EPSILON)
    }

    kotlin.run {
      val res = (-0.025f).quantize(0.025f)
      Assert.assertTrue("res=$res", ((-0.025f) - res).absoluteValue <= EPSILON)
    }

    kotlin.run {
      val res = (-0.333f).quantize(0.025f)
      Assert.assertTrue("res=$res", ((-0.325f) - res).absoluteValue <= EPSILON)
    }

    kotlin.run {
      val res = (-0.351f).quantize(0.025f)
      Assert.assertTrue("res=$res", ((-0.35f) - res).absoluteValue <= EPSILON)
    }

    kotlin.run {
      val res = (-974f).quantize(25f)
      Assert.assertEquals(-975, res.roundToInt())
    }

    kotlin.run {
      val res = (-1000f).quantize(25f)
      Assert.assertEquals((-1000), res.roundToInt())
    }
  }
}