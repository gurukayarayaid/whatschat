package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Utility for formatting message timestamps into human-readable relative time strings
 * (e.g., '10:30 AM', 'Yesterday', 'Monday', '15/09/2026').
 */
object TimeFormatter {

    /**
     * Formats timestamp for conversation list items (ChatListScreen):
     * - Today: '10:30 AM'
     * - Yesterday: 'Yesterday' (or 'Kemarin' in Indonesian)
     * - Within last 6 days: Day of week (e.g. 'Monday' / 'Senin')
     * - Older: Date string (e.g. '15/09/2026')
     */
    fun formatRelativeTime(
        timestamp: Long,
        locale: Locale = Locale.getDefault()
    ): String {
        if (timestamp <= 0L) return ""

        val now = Calendar.getInstance()
        val msgTime = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }

        val isSameYear = now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR)
        val isToday = isSameYear && now.get(Calendar.DAY_OF_YEAR) == msgTime.get(Calendar.DAY_OF_YEAR)

        if (isToday) {
            val timeFormatter = SimpleDateFormat("h:mm a", locale)
            return timeFormatter.format(Date(timestamp))
        }

        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val isYesterday = yesterday.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == msgTime.get(Calendar.DAY_OF_YEAR)

        if (isYesterday) {
            return if (locale.language == "in" || locale.language == "id") "Kemarin" else "Yesterday"
        }

        // Within last 6 days: show day name (e.g. Monday / Senin)
        val diffDays = (now.timeInMillis - msgTime.timeInMillis) / (24 * 60 * 60 * 1000L)
        if (diffDays in 1..6) {
            val dayFormatter = SimpleDateFormat("EEEE", locale)
            return dayFormatter.format(Date(timestamp))
        }

        // Older dates
        val dateFormatter = if (isSameYear) {
            SimpleDateFormat("d MMM", locale)
        } else {
            SimpleDateFormat("dd/MM/yyyy", locale)
        }
        return dateFormatter.format(Date(timestamp))
    }

    /**
     * Formats timestamp for individual message bubbles (e.g., '10:30 AM').
     */
    fun formatMessageTime(
        timestamp: Long,
        locale: Locale = Locale.getDefault()
    ): String {
        if (timestamp <= 0L) return ""
        val formatter = SimpleDateFormat("h:mm a", locale)
        return formatter.format(Date(timestamp))
    }

    /**
     * Formats timestamp for chat date separator pills (e.g., 'Today', 'Yesterday', '15 September 2026').
     */
    fun formatChatDateHeader(
        timestamp: Long,
        locale: Locale = Locale.getDefault()
    ): String {
        if (timestamp <= 0L) return ""

        val now = Calendar.getInstance()
        val msgTime = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }

        val isSameYear = now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR)
        val isToday = isSameYear && now.get(Calendar.DAY_OF_YEAR) == msgTime.get(Calendar.DAY_OF_YEAR)

        if (isToday) {
            return if (locale.language == "in" || locale.language == "id") "Hari Ini" else "Today"
        }

        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val isYesterday = yesterday.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == msgTime.get(Calendar.DAY_OF_YEAR)

        if (isYesterday) {
            return if (locale.language == "in" || locale.language == "id") "Kemarin" else "Yesterday"
        }

        val diffDays = (now.timeInMillis - msgTime.timeInMillis) / (24 * 60 * 60 * 1000L)
        if (diffDays in 1..6) {
            val dayFormatter = SimpleDateFormat("EEEE", locale)
            return dayFormatter.format(Date(timestamp))
        }

        val fullDateFormatter = SimpleDateFormat("d MMMM yyyy", locale)
        return fullDateFormatter.format(Date(timestamp))
    }

    /**
     * Returns a string key representing the calendar day (e.g., '2026-09-15')
     * to easily group messages by date.
     */
    fun getDateGroupingKey(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }

    /**
     * Formats last seen presence timestamp for contacts and chat headers (e.g. 'Online', 'Terakhir dilihat hari ini pukul 14:30', 'Offline').
     */
    fun formatLastSeen(
        timestamp: Long,
        isOnline: Boolean,
        locale: Locale = Locale.getDefault()
    ): String {
        if (isOnline) {
            return "Online"
        }
        if (timestamp <= 0L) {
            return "Offline"
        }

        val isIndonesian = locale.language == "in" || locale.language == "id"
        val now = Calendar.getInstance()
        val seenTime = Calendar.getInstance().apply { timeInMillis = timestamp }

        val isSameYear = now.get(Calendar.YEAR) == seenTime.get(Calendar.YEAR)
        val isToday = isSameYear && now.get(Calendar.DAY_OF_YEAR) == seenTime.get(Calendar.DAY_OF_YEAR)

        val timeFormat = SimpleDateFormat("HH:mm", locale)
        val timeStr = timeFormat.format(Date(timestamp))

        if (isToday) {
            return if (isIndonesian) "Terakhir dilihat hari ini $timeStr" else "Last seen today at $timeStr"
        }

        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = yesterday.get(Calendar.YEAR) == seenTime.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == seenTime.get(Calendar.DAY_OF_YEAR)

        if (isYesterday) {
            return if (isIndonesian) "Terakhir dilihat kemarin $timeStr" else "Last seen yesterday at $timeStr"
        }

        val dateFormat = if (isSameYear) SimpleDateFormat("d MMM HH:mm", locale) else SimpleDateFormat("dd/MM/yy HH:mm", locale)
        val dateStr = dateFormat.format(Date(timestamp))
        return if (isIndonesian) "Terakhir dilihat $dateStr" else "Last seen $dateStr"
    }
}
