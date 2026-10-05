package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MessageStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, lastTimestamp DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    fun getConversationById(id: String): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationDirect(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(conversations: List<ConversationEntity>)

    @Update
    suspend fun update(conversation: ConversationEntity)

    @Query("UPDATE conversations SET lastMessage = :lastMsg, lastTimestamp = :timestamp, unreadCount = :unread WHERE id = :id")
    suspend fun updateLastMessage(id: String, lastMsg: String, timestamp: Long, unread: Int)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE conversations SET avatarUrl = :avatarUrl WHERE id = :id")
    suspend fun updateGroupAvatar(id: String, avatarUrl: String)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMessages(conversationId: String, limit: Int = 50): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE isSyncPending = 1 OR status = 'SENDING' ORDER BY timestamp ASC")
    suspend fun getPendingSyncMessages(): List<MessageEntity>

    @Query("UPDATE messages SET status = :status WHERE id = :id")
    suspend fun updateMessageStatus(id: String, status: MessageStatus)

    @Query("UPDATE messages SET status = :status, isSyncPending = 0 WHERE id = :id")
    suspend fun markMessageSynced(id: String, status: MessageStatus = MessageStatus.SENT)

    @Query("UPDATE messages SET status = 'READ', seenByJson = :seenByJson WHERE conversationId = :conversationId AND isOutgoing = 0")
    suspend fun markMessagesAsReadForConversation(conversationId: String, seenByJson: String = "[\"me\"]")

    @Query("UPDATE messages SET status = 'READ', seenByJson = :seenByJson WHERE id = :id")
    suspend fun updateMessageSeenBy(id: String, seenByJson: String)

    @Query("UPDATE messages SET mediaUri = :mediaUri, status = :status WHERE id = :id")
    suspend fun updateMessageMediaUriAndStatus(id: String, mediaUri: String, status: MessageStatus = MessageStatus.READ)

    @Query("UPDATE messages SET localFilePath = :localPath WHERE id = :id")
    suspend fun updateMediaLocalPath(id: String, localPath: String)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND text LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchMessages(conversationId: String, query: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE text LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchAllMessages(query: String): Flow<List<MessageEntity>>

    @Query("SELECT COUNT(*) FROM messages WHERE isOutgoing = 0 AND status != 'READ'")
    fun getUnreadMessageCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMessage(conversationId: String): MessageEntity?

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun clearChat(conversationId: String)

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun getMessageCount(): Int
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE isRegistered = 1 ORDER BY name ASC")
    fun getRegisteredContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE name LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchContacts(query: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun getContactById(id: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE email = :email LIMIT 1")
    suspend fun getContactByEmail(email: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE phone = :phone LIMIT 1")
    suspend fun getContactByPhone(phone: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ContactEntity>)

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContact(id: String)

    @Query("DELETE FROM contacts")
    suspend fun deleteAllContacts()

    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun getContactCount(): Int
}

