package com.flxrs.dankchat.preferences.backup

import com.flxrs.dankchat.data.database.dao.BadgeHighlightDao
import com.flxrs.dankchat.data.database.dao.BlacklistedUserDao
import com.flxrs.dankchat.data.database.dao.MessageHighlightDao
import com.flxrs.dankchat.data.database.dao.MessageIgnoreDao
import com.flxrs.dankchat.data.database.dao.UserDisplayDao
import com.flxrs.dankchat.data.database.dao.UserHighlightDao
import com.flxrs.dankchat.data.database.dao.UserIgnoreDao
import com.flxrs.dankchat.data.database.entity.BadgeHighlightEntity
import com.flxrs.dankchat.data.database.entity.BlacklistedUserEntity
import com.flxrs.dankchat.data.database.entity.MessageHighlightEntity
import com.flxrs.dankchat.data.database.entity.MessageIgnoreEntity
import com.flxrs.dankchat.data.database.entity.UserDisplayEntity
import com.flxrs.dankchat.data.database.entity.UserHighlightEntity
import com.flxrs.dankchat.data.database.entity.UserIgnoreEntity
import com.flxrs.dankchat.data.toUserNames
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.appearance.AppearanceSettings
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import com.flxrs.dankchat.preferences.chat.ChatSettings
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import com.flxrs.dankchat.preferences.developer.DeveloperSettings
import com.flxrs.dankchat.preferences.developer.DeveloperSettingsDataStore
import com.flxrs.dankchat.preferences.notifications.NotificationsSettings
import com.flxrs.dankchat.preferences.notifications.NotificationsSettingsDataStore
import com.flxrs.dankchat.preferences.stream.StreamsSettings
import com.flxrs.dankchat.preferences.stream.StreamsSettingsDataStore
import com.flxrs.dankchat.preferences.tools.ToolsSettings
import com.flxrs.dankchat.preferences.tools.ToolsSettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

/**
 * Everything a backup file holds. The Twitch login is deliberately not part of it.
 * Every field has a default, so older or partial files can still be imported.
 */
@Serializable
data class SettingsBackup(
    val formatVersion: Int = FORMAT_VERSION,
    val appVersion: String? = null,
    val appearance: AppearanceSettings? = null,
    val chat: ChatSettings? = null,
    val notifications: NotificationsSettings? = null,
    val tools: ToolsSettings? = null,
    val streams: StreamsSettings? = null,
    val developer: DeveloperSettings? = null,
    val channels: List<String>? = null,
    val channelRenames: String? = null,
    val messageHighlights: List<MessageHighlightEntity>? = null,
    val userHighlights: List<UserHighlightEntity>? = null,
    val badgeHighlights: List<BadgeHighlightEntity>? = null,
    val blacklistedUsers: List<BlacklistedUserEntity>? = null,
    val messageIgnores: List<MessageIgnoreEntity>? = null,
    val userIgnores: List<UserIgnoreEntity>? = null,
    val userDisplays: List<UserDisplayEntity>? = null,
) {
    companion object {
        const val FORMAT_VERSION = 1
    }
}

@Single
class SettingsBackupManager(
    private val preferenceStore: DankChatPreferenceStore,
    private val appearanceSettingsDataStore: AppearanceSettingsDataStore,
    private val chatSettingsDataStore: ChatSettingsDataStore,
    private val notificationsSettingsDataStore: NotificationsSettingsDataStore,
    private val toolsSettingsDataStore: ToolsSettingsDataStore,
    private val streamsSettingsDataStore: StreamsSettingsDataStore,
    private val developerSettingsDataStore: DeveloperSettingsDataStore,
    private val messageHighlightDao: MessageHighlightDao,
    private val userHighlightDao: UserHighlightDao,
    private val badgeHighlightDao: BadgeHighlightDao,
    private val blacklistedUserDao: BlacklistedUserDao,
    private val messageIgnoreDao: MessageIgnoreDao,
    private val userIgnoreDao: UserIgnoreDao,
    private val userDisplayDao: UserDisplayDao,
) {

    private val backupJson = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun export(appVersion: String): String {
        val backup = SettingsBackup(
            appVersion = appVersion,
            appearance = appearanceSettingsDataStore.current(),
            chat = chatSettingsDataStore.current(),
            notifications = notificationsSettingsDataStore.current(),
            tools = toolsSettingsDataStore.current(),
            streams = streamsSettingsDataStore.current(),
            developer = developerSettingsDataStore.current(),
            channels = preferenceStore.channels.map { it.value },
            channelRenames = preferenceStore.channelRenamesJson,
            messageHighlights = messageHighlightDao.getMessageHighlights(),
            userHighlights = userHighlightDao.getUserHighlights(),
            badgeHighlights = badgeHighlightDao.getBadgeHighlights(),
            blacklistedUsers = blacklistedUserDao.getBlacklistedUserFlow().first(),
            messageIgnores = messageIgnoreDao.getMessageIgnores(),
            userIgnores = userIgnoreDao.getUserIgnoresFlow().first(),
            userDisplays = userDisplayDao.getUserDisplaysFlow().first(),
        )
        return backupJson.encodeToString(SettingsBackup.serializer(), backup)
    }

    /** Parses the file first, so a broken file changes nothing. Sections missing from the file are left as they are. */
    fun parse(content: String): SettingsBackup = backupJson.decodeFromString(SettingsBackup.serializer(), content)

    suspend fun import(backup: SettingsBackup) {
        backup.appearance?.let { value -> appearanceSettingsDataStore.update { value } }
        backup.chat?.let { value -> chatSettingsDataStore.update { value } }
        backup.notifications?.let { value -> notificationsSettingsDataStore.update { value } }
        backup.tools?.let { value -> toolsSettingsDataStore.update { value } }
        backup.streams?.let { value -> streamsSettingsDataStore.update { value } }
        backup.developer?.let { value -> developerSettingsDataStore.update { value } }

        if (backup.channels != null) {
            preferenceStore.restoreChannels(backup.channels.toUserNames(), backup.channelRenames)
        }

        backup.messageHighlights?.let {
            messageHighlightDao.deleteAllHighlights()
            messageHighlightDao.addHighlights(it)
        }
        backup.userHighlights?.let {
            userHighlightDao.deleteAllHighlights()
            userHighlightDao.addHighlights(it)
        }
        backup.badgeHighlights?.let {
            badgeHighlightDao.deleteAllHighlights()
            badgeHighlightDao.addHighlights(it)
        }
        backup.blacklistedUsers?.let {
            blacklistedUserDao.deleteAllBlacklistedUsers()
            blacklistedUserDao.addBlacklistedUsers(it)
        }
        backup.messageIgnores?.let {
            messageIgnoreDao.deleteAllIgnores()
            messageIgnoreDao.addIgnores(it)
        }
        backup.userIgnores?.let {
            userIgnoreDao.deleteAllIgnores()
            userIgnoreDao.addIgnores(it)
        }
        backup.userDisplays?.let {
            userDisplayDao.deleteAll()
            userDisplayDao.upsertAll(it)
        }
    }
}
