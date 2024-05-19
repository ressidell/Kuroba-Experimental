package com.github.k1rakishou.chan.core.site

abstract class ImageSearch {
  abstract val id: Int
  abstract val name: String
  abstract fun getUrl(imageUrl: String): String

  companion object {
    val engines: MutableList<ImageSearch> = ArrayList()

    init {
      engines.add(object : ImageSearch() {
        override val id: Int
          get() = 0

        override val name: String
          get() = "Google"

        override fun getUrl(imageUrl: String): String {
          return "https://www.google.com/searchbyimage?sbisrc=cr_1&safe=off&image_url=$imageUrl"
        }
      })

      engines.add(object : ImageSearch() {
        override val id: Int
          get() = 1

        override val name: String
          get() = "Google Lens"

        override fun getUrl(imageUrl: String): String {
          return "https://lens.google.com/uploadbyurl?url=$imageUrl"
        }
      })

      engines.add(object : ImageSearch() {
        override val id: Int
          get() = 2

        override val name: String
          get() = "iqdb"

        override fun getUrl(imageUrl: String): String {
          return "http://iqdb.org/?url=$imageUrl"
        }
      })

      engines.add(object : ImageSearch() {
        override val id: Int
          get() = 3

        override val name: String
          get() = "SauceNao"

        override fun getUrl(imageUrl: String): String {
          return "https://saucenao.com/search.php?url=$imageUrl"
        }
      })

      engines.add(object : ImageSearch() {
        override val id: Int
          get() = 4

        override val name: String
          get() = "TinEye"

        override fun getUrl(imageUrl: String): String {
          return "http://tineye.com/search/?url=$imageUrl"
        }
      })

      engines.add(object : ImageSearch() {
        override val id: Int
          get() = 5

        override val name: String
          get() = "WAIT"

        override fun getUrl(imageUrl: String): String {
          return "https://trace.moe/?url=$imageUrl"
        }
      })

      engines.add(object : ImageSearch() {
        override val id: Int
          get() = 6

        override val name: String
          get() = "Yandex"

        override fun getUrl(imageUrl: String): String {
          return "https://yandex.com/images/search?rpt=imageview&url=$imageUrl"
        }
      })
    }
  }
}
