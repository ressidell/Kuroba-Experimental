package com.github.k1rakishou.common

import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Test

class KotlinExtensionsKtTest {

    @Test
    fun testAddOrReplaceCookieHeader() {
        val requestBuilder = Request.Builder()
            .url("http://test.com")
            .get()

        requestBuilder
            .addOrReplaceCookieHeader("aaabbb=abc")
            .addOrReplaceCookieHeader("test_cookie=123")

        assertEquals("aaabbb=abc; test_cookie=123", requestBuilder.build().header("Cookie"))

        requestBuilder
            .addOrReplaceCookieHeader("test_cookie=124")

        assertEquals("aaabbb=abc; test_cookie=124", requestBuilder.build().header("Cookie"))

        requestBuilder
            .addOrReplaceCookieHeader("aaabbb=aaa")

        assertEquals("aaabbb=aaa; test_cookie=124", requestBuilder.build().header("Cookie"))

        requestBuilder
            .addOrReplaceCookieHeader("test_cookie=125")
            .addOrReplaceCookieHeader("aaabbb=bbb")

        assertEquals("aaabbb=bbb; test_cookie=125", requestBuilder.build().header("Cookie"))
    }

}