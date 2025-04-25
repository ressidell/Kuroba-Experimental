package com.github.k1rakishou.model.migrations

import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.github.k1rakishou.model.KurobaDatabase


class Migration_v43_to_v44 : Migration(43, 44) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.doWithoutForeignKeys {
            try {
                database.execSQL("ALTER TABLE chan_filter ADD COLUMN filter_watch_autosave INTEGER NOT NULL DEFAULT ${KurobaDatabase.SQLITE_FALSE}");
                database.execSQL("ALTER TABLE chan_filter ADD COLUMN filter_watch_autosave_media INTEGER NOT NULL DEFAULT ${KurobaDatabase.SQLITE_FALSE}");
            } catch (ex: SQLiteException) {
                if (!ex.message?.startsWith( "duplicate column name" )!!){throw ex}
            }
        }
    }

}