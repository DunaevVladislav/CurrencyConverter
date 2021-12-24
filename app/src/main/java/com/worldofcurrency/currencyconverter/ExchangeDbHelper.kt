package com.worldofcurrency.currencyconverter

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.sql.Time

class ExchangeDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "Exchange.db"
        const val ID = "id"
        const val TABLE_NAME = "exchange"
        const val COLUMN_NAME_FROM_CURRENCY = "from_currency"
        const val COLUMN_NAME_TO_CURRENCY = "to_currency"
        const val COLUMN_NAME_FROM_COUNT = "from_count"
        const val COLUMN_NAME_TO_COUNT = "to_count"
        const val COLUMN_NAME_EXCHANGE_DATE = "exchange_date"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE_NAME (" +
                    "$ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "$COLUMN_NAME_FROM_CURRENCY TEXT," +
                    "$COLUMN_NAME_TO_CURRENCY TEXT," +
                    "$COLUMN_NAME_FROM_COUNT REAL," +
                    "$COLUMN_NAME_TO_COUNT REAL," +
                    "$COLUMN_NAME_EXCHANGE_DATE INTEGER)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    fun addEntry(model: ExchangeModel): Boolean {
        val cv = ContentValues()
        cv.put(COLUMN_NAME_FROM_CURRENCY, model.fromCurrency)
        cv.put(COLUMN_NAME_TO_CURRENCY, model.toCurrency)
        cv.put(COLUMN_NAME_FROM_COUNT, model.fromCount)
        cv.put(COLUMN_NAME_TO_COUNT, model.toCount)
        cv.put(COLUMN_NAME_EXCHANGE_DATE, model.exchangeDate.time)

        return writableDatabase.insert(TABLE_NAME, null, cv) != -1L
    }

    fun getAllEntries(): List<ExchangeModel> {
        val entries = mutableListOf<ExchangeModel>()
        val cursor = readableDatabase.rawQuery("SELECT * FROM $TABLE_NAME", null)
        if (cursor.moveToFirst()) {
            do {
                entries.add(
                    ExchangeModel(
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getDouble(3),
                        cursor.getDouble(4),
                        Time(cursor.getLong(5))
                    )
                )

            } while (cursor.moveToNext())
        }
        cursor.close()
        return entries
    }

}