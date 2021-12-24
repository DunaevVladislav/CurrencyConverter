package com.worldofcurrency.currencyconverter

import java.util.*

data class ExchangeModel(
    var fromCurrency: String,
    var toCurrency: String,
    var fromCount: Double,
    var toCount: Double,
    var exchangeDate: Date
) {
    override fun toString(): String {
        return "$fromCount $fromCurrency => $toCount $toCurrency " +
                "(${
                    android.text.format.DateFormat.format(
                        "HH:mm:ss dd.MM.yy",
                        exchangeDate
                    )
                })"
    }
}
