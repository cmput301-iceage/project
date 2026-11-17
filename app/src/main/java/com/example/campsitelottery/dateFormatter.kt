package com.example.campsitelottery

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


fun dateFormatPretty() = SimpleDateFormat("EEE MMM d, yyyy", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
    isLenient = false
}

fun dateISOFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
    isLenient = false
}

fun dateLongUIFormat(date: Date?): String =
    date?.let { dateFormatPretty().format(it) } ?: "Not Set"



