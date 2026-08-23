package com.simats.skillora.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository {
    private val TAG = "ChatRepository"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val chatsCollection = firestore.collection("chats")

    companion object {
        fun generateChatId(skillId: String, studentId: String, publisherId: String): String {
            return "${skillId}_${studentId}_${publisherId}"
        }
    }

    suspend fun getOrCreateChat(skillId: String, skillTitle: String, publisherId: String, studentId: String): Result<Chat> {
        return try {
            // Deterministic chatId: skillId_studentId_publisherId
            val chatId = generateChatId(skillId, studentId, publisherId)
            
            val chatRef = chatsCollection.document(chatId)
            val chatDoc = chatRef.get().await()
            if (chatDoc.exists()) {
                Result.success(chatDoc.toObject(Chat::class.java)!!)
            } else {
                val newChat = Chat(
                    chatId = chatId,
                    skillId = skillId,
                    skillTitle = skillTitle,
                    studentId = studentId,
                    publisherId = publisherId
                )
                // Use set with merge to be idempotent/concurrency-safe
                chatsCollection.document(chatId).set(newChat).await()
                // Set initial timestamps if they didn't exist
                chatsCollection.document(chatId).update(
                    "createdAt", FieldValue.serverTimestamp(),
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()
                Result.success(newChat)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getOrCreateChat", e)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(chatId: String, receiverId: String, text: String): Result<Unit> {
        return try {
            val senderId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            val messageId = UUID.randomUUID().toString()
            
            val message = ChatMessage(
                messageId = messageId,
                senderId = senderId,
                receiverId = receiverId,
                text = text
            )

            firestore.runTransaction { transaction ->
                val chatRef = chatsCollection.document(chatId)
                val chatSnapshot = transaction.get(chatRef)
                val chat = chatSnapshot.toObject(Chat::class.java) ?: throw Exception("Chat not found")

                // Add message
                val messageRef = chatRef.collection("messages").document(messageId)
                transaction.set(messageRef, message)
                transaction.update(messageRef, "sentAt", FieldValue.serverTimestamp())

                // Update chat parent
                val isStudentSender = senderId == chat.studentId
                val unreadField = if (isStudentSender) "publisherUnreadCount" else "studentUnreadCount"
                
                transaction.update(chatRef, 
                    "lastMessage", text,
                    "lastMessageAt", FieldValue.serverTimestamp(),
                    "updatedAt", FieldValue.serverTimestamp(),
                    unreadField, FieldValue.increment(1)
                )
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            Result.failure(e)
        }
    }

    fun observeMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = chatsCollection.document(chatId).collection("messages")
            .orderBy("sentAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.toObjects(ChatMessage::class.java) ?: emptyList<ChatMessage>()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    suspend fun markAsRead(chatId: String, isStudent: Boolean): Result<Unit> {
        return try {
            val unreadField = if (isStudent) "studentUnreadCount" else "publisherUnreadCount"
            chatsCollection.document(chatId).update(unreadField, 0).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observePublisherChats(publisherId: String): Flow<List<Chat>> = callbackFlow {
        val listener = chatsCollection
            .whereEqualTo("publisherId", publisherId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val chats = snapshot?.toObjects(Chat::class.java) ?: emptyList<Chat>()
                
                // Filter out self-chats (creators cannot enroll in their own skills)
                // and sort in-memory to avoid composite index requirements
                val filteredAndSorted = chats
                    .filter { it.studentId != it.publisherId }
                    .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                
                trySend(filteredAndSorted)
            }
        awaitClose { listener.remove() }
    }
}
