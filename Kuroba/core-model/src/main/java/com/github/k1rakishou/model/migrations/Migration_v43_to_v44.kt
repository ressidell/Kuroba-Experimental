package com.github.k1rakishou.model.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration_v43_to_v44 : Migration(43, 44) {
  override fun migrate(db: SupportSQLiteDatabase) {
      db.doWithoutForeignKeys {
          db.execSQL("ALTER TABLE chan_filter ADD COLUMN filter_watch_autosave INTEGER NOT NULL DEFAULT 0")
          db.execSQL("ALTER TABLE chan_filter ADD COLUMN filter_watch_autosave_media INTEGER NOT NULL DEFAULT 0")
      }
  }
}