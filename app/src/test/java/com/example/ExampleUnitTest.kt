package com.example

import com.example.data.model.UserProfile
import com.example.util.TimeFormatter
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun userProfile_defaultValuesAndCopy() {
    val profile = UserProfile(
      displayName = "Guru Kaya Raya",
      email = "guru.kayaraya.id@gmail.com",
      status = "Ada menggunakan WhatsChat E2EE"
    )
    assertEquals("Guru Kaya Raya", profile.displayName)
    assertEquals("guru.kayaraya.id@gmail.com", profile.email)

    val updated = profile.copy(displayName = "Ahmad Pratama", avatarUrl = "https://firebasestorage.googleapis.com/test.jpg")
    assertEquals("Ahmad Pratama", updated.displayName)
    assertEquals("https://firebasestorage.googleapis.com/test.jpg", updated.avatarUrl)
  }

  @Test
  fun timeFormatter_relativeTime() {
    val now = System.currentTimeMillis()
    val messageTime = TimeFormatter.formatMessageTime(now)
    assertFalse(messageTime.isEmpty())

    val relativeToday = TimeFormatter.formatRelativeTime(now)
    assertFalse(relativeToday.isEmpty())

    val yesterday = now - 24 * 3600_100L
    val relativeYesterday = TimeFormatter.formatRelativeTime(yesterday)
    assertTrue(relativeYesterday == "Yesterday" || relativeYesterday == "Kemarin")
  }
}
