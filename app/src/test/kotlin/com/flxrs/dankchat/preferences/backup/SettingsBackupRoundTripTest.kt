package com.flxrs.dankchat.preferences.backup

import com.flxrs.dankchat.data.database.entity.MessageHighlightEntity
import com.flxrs.dankchat.data.database.entity.MessageHighlightEntityType
import com.flxrs.dankchat.data.database.entity.UserDisplayEntity
import com.flxrs.dankchat.data.database.entity.UserIgnoreEntity
import com.flxrs.dankchat.preferences.appearance.AppearanceSettings
import com.flxrs.dankchat.preferences.chat.ChatSettings
import com.flxrs.dankchat.preferences.developer.DeveloperSettings
import com.flxrs.dankchat.preferences.notifications.NotificationsSettings
import com.flxrs.dankchat.preferences.stream.StreamsSettings
import com.flxrs.dankchat.preferences.tools.ToolsSettings
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SettingsBackupRoundTripTest {
    private val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true; coerceInputValues = true }

    @Test
    fun `backup survives export and import`() {
        val backup = SettingsBackup(
            appVersion = "3.11.11",
            appearance = AppearanceSettings(),
            chat = ChatSettings(scrollbackLength = 1234, timestampFormat = "HH:mm:ss"),
            notifications = NotificationsSettings(),
            tools = ToolsSettings(ttsEnabled = true, ttsUserIgnoreList = setOf("nightbot")),
            streams = StreamsSettings(splitFraction = 0.45f),
            developer = DeveloperSettings(),
            channels = listOf("forsen", "xqc"),
            channelRenames = """{"xqc":"juicer"}""",
            messageHighlights = listOf(MessageHighlightEntity(id = 3, enabled = true, type = MessageHighlightEntityType.Custom, pattern = "pog.*", isRegex = true, customColor = 0x7fff0000)),
            userIgnores = listOf(UserIgnoreEntity(id = 1, enabled = true, username = "spambot")),
            userDisplays = listOf(UserDisplayEntity(id = 2, targetUser = "friend", enabled = true, colorEnabled = true, color = -16711936, aliasEnabled = true, alias = "buddy")),
        )
        val text = json.encodeToString(SettingsBackup.serializer(), backup)
        println(text.take(1500))
        val restored = json.decodeFromString(SettingsBackup.serializer(), text)
        assertEquals(backup, restored)
    }

    @Test
    fun `partial file leaves missing sections untouched`() {
        val restored = json.decodeFromString(SettingsBackup.serializer(), """{"streams":{"splitFraction":0.3},"unknownField":1}""")
        assertEquals(0.3f, restored.streams?.splitFraction)
        assertEquals(null, restored.chat)
        assertEquals(null, restored.channels)
    }
}
