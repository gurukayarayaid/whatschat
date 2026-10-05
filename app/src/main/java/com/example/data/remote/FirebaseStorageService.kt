package com.example.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

/**
 * FirebaseStorageService:
 * Manages uploading and retrieving images and media files to/from Firebase Storage.
 * Provides granular progress tracking, metadata association, secure retrieval,
 * and direct integration with the local Room database.
 */
class FirebaseStorageService(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    companion object {
        private const val TAG = "FirebaseStorageService"
        private const val CHAT_IMAGES_FOLDER = "chat_images"
        private const val GROUP_AVATARS_FOLDER = "group_avatars"

        @Volatile
        private var INSTANCE: FirebaseStorageService? = null

        fun getInstance(): FirebaseStorageService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseStorageService().also { INSTANCE = it }
            }
        }
    }

    private val rootRef: StorageReference
        get() = storage.reference

    /**
     * Upload custom group profile image to Firebase Storage with percentage progress tracking.
     * Path: group_avatars/{conversationId}/{timestamp}.jpg
     */
    suspend fun uploadGroupAvatar(
        conversationId: String,
        imageUri: Uri,
        onProgress: ((Int) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fileName = "avatar_${System.currentTimeMillis()}.jpg"
            val storageRef = rootRef.child("$GROUP_AVATARS_FOLDER/$conversationId/$fileName")

            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("conversationId", conversationId)
                .setCustomMetadata("type", "group_avatar")
                .setCustomMetadata("uploadedAt", System.currentTimeMillis().toString())
                .build()

            val uploadTask = storageRef.putFile(imageUri, metadata)

            if (onProgress != null) {
                uploadTask.addOnProgressListener { taskSnapshot ->
                    val totalBytes = taskSnapshot.totalByteCount
                    val transferredBytes = taskSnapshot.bytesTransferred
                    if (totalBytes > 0) {
                        val progress = ((transferredBytes * 100) / totalBytes).toInt().coerceIn(0, 99)
                        onProgress(progress)
                    }
                }
            }

            uploadTask.await()
            onProgress?.invoke(100)

            val downloadUri = storageRef.downloadUrl.await()
            val downloadUrl = downloadUri.toString()

            Log.d(TAG, "Group avatar uploaded successfully for group $conversationId: $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading group avatar to Firebase Storage: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Upload an image from an Android Uri (content:// or file://) to Firebase Storage
     * with real-time percentage progress callback.
     *
     * @param conversationId The ID of the chat conversation
     * @param messageId The unique ID of the message
     * @param imageUri Android Uri of the image file
     * @param onProgress Callback receiving progress percentage (0..100)
     * @return Result containing the public download URL on success
     */
    suspend fun uploadChatImage(
        conversationId: String,
        messageId: String,
        imageUri: Uri,
        onProgress: ((Int) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val storageRef = rootRef.child("$CHAT_IMAGES_FOLDER/$conversationId/$messageId.jpg")

            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("conversationId", conversationId)
                .setCustomMetadata("messageId", messageId)
                .setCustomMetadata("uploadedAt", System.currentTimeMillis().toString())
                .build()

            val uploadTask = storageRef.putFile(imageUri, metadata)

            // Attach progress listener
            if (onProgress != null) {
                uploadTask.addOnProgressListener { taskSnapshot ->
                    val totalBytes = taskSnapshot.totalByteCount
                    val transferredBytes = taskSnapshot.bytesTransferred
                    if (totalBytes > 0) {
                        val progress = ((transferredBytes * 100) / totalBytes).toInt().coerceIn(0, 99)
                        onProgress(progress)
                    }
                }
            }

            // Await completion
            uploadTask.await()
            onProgress?.invoke(100)

            // Retrieve permanent download URL
            val downloadUri = storageRef.downloadUrl.await()
            val downloadUrl = downloadUri.toString()

            Log.d(TAG, "Image successfully uploaded for message $messageId: $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image to Firebase Storage: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Upload image byte array directly to Firebase Storage.
     */
    suspend fun uploadImageBytes(
        conversationId: String,
        messageId: String,
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg",
        onProgress: ((Int) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val extension = if (mimeType.contains("png")) "png" else "jpg"
            val storageRef = rootRef.child("$CHAT_IMAGES_FOLDER/$conversationId/$messageId.$extension")

            val metadata = StorageMetadata.Builder()
                .setContentType(mimeType)
                .setCustomMetadata("conversationId", conversationId)
                .setCustomMetadata("messageId", messageId)
                .setCustomMetadata("uploadedAt", System.currentTimeMillis().toString())
                .build()

            val uploadTask = storageRef.putBytes(imageBytes, metadata)

            if (onProgress != null) {
                uploadTask.addOnProgressListener { taskSnapshot ->
                    val totalBytes = taskSnapshot.totalByteCount
                    val transferredBytes = taskSnapshot.bytesTransferred
                    if (totalBytes > 0) {
                        val progress = ((transferredBytes * 100) / totalBytes).toInt().coerceIn(0, 99)
                        onProgress(progress)
                    }
                }
            }

            uploadTask.await()
            onProgress?.invoke(100)

            val downloadUri = storageRef.downloadUrl.await()
            val downloadUrl = downloadUri.toString()

            Log.d(TAG, "Image bytes uploaded successfully for message $messageId: $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image bytes: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Upload from an InputStream.
     */
    suspend fun uploadImageStream(
        conversationId: String,
        messageId: String,
        inputStream: InputStream,
        mimeType: String = "image/jpeg",
        onProgress: ((Int) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val storageRef = rootRef.child("$CHAT_IMAGES_FOLDER/$conversationId/$messageId.jpg")
            val metadata = StorageMetadata.Builder()
                .setContentType(mimeType)
                .setCustomMetadata("conversationId", conversationId)
                .setCustomMetadata("messageId", messageId)
                .build()

            val uploadTask = storageRef.putStream(inputStream, metadata)
            if (onProgress != null) {
                uploadTask.addOnProgressListener { taskSnapshot ->
                    val totalBytes = taskSnapshot.totalByteCount
                    val transferredBytes = taskSnapshot.bytesTransferred
                    if (totalBytes > 0) {
                        val progress = ((transferredBytes * 100) / totalBytes).toInt().coerceIn(0, 99)
                        onProgress(progress)
                    }
                }
            }

            uploadTask.await()
            onProgress?.invoke(100)

            val downloadUri = storageRef.downloadUrl.await()
            Result.success(downloadUri.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image stream: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieve the download URL for an existing chat image.
     */
    suspend fun getChatImageDownloadUrl(
        conversationId: String,
        messageId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val storageRef = rootRef.child("$CHAT_IMAGES_FOLDER/$conversationId/$messageId.jpg")
            val downloadUri = storageRef.downloadUrl.await()
            Result.success(downloadUri.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving image download URL: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Download image bytes from Firebase Storage directly (e.g. for offline caching or processing).
     */
    suspend fun downloadImageBytes(
        downloadUrlOrPath: String,
        maxByteSize: Long = 15 * 1024 * 1024 // 15 MB
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val storageRef = if (downloadUrlOrPath.startsWith("gs://") || downloadUrlOrPath.startsWith("https://")) {
                storage.getReferenceFromUrl(downloadUrlOrPath)
            } else {
                rootRef.child(downloadUrlOrPath)
            }

            val bytes = storageRef.getBytes(maxByteSize).await()
            Result.success(bytes)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading image bytes: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete an image from Firebase Storage.
     */
    suspend fun deleteChatImage(
        conversationId: String,
        messageId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val storageRef = rootRef.child("$CHAT_IMAGES_FOLDER/$conversationId/$messageId.jpg")
            storageRef.delete().await()
            Log.d(TAG, "Deleted image for message $messageId from storage")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting image from storage: ${e.message}", e)
            Result.failure(e)
        }
    }
}
