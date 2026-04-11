package dk.itu.moapd.x9.s25134.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.graphics.scale
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

class StorageRepository(private val context: Context) {

    companion object {
        private const val TAG = "StorageRepository"
        private const val MAX_IMAGE_DIM = 1280
        private const val JPEG_QUALITY = 85
    }

    private val storage = Firebase.storage

    /**
     * Compresses the image at [uri] to JPEG and uploads it to Storage at
     * "report-images/{userId}/{reportId}.jpg".
     * Returns a [Result] containing the public download URL on success.
     */
    suspend fun uploadReportImage(
        uri: Uri,
        userId: String,
        reportId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bytes = compressImage(uri)
            val ref = storage.reference.child("report-images/$userId/$reportId.jpg")
            ref.putBytes(bytes).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Log.d(TAG, "Upload success for report $reportId: $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed for report $reportId: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun compressImage(uri: Uri): ByteArray {
        // BitmapFactory.decodeStream returns null for corrupted or unsupported files.
        // Treat this as a hard failure so the caller can surface it to the user.
        val original = context.contentResolver.openInputStream(uri)
            ?.use { BitmapFactory.decodeStream(it) }
            ?: throw IOException("Cannot open image stream for uri=$uri")
        val bitmap = if (original.width > MAX_IMAGE_DIM || original.height > MAX_IMAGE_DIM) {
            val scaleFactor = MAX_IMAGE_DIM.toFloat() / maxOf(original.width, original.height)
            val scaled = original.scale(
                (original.width * scaleFactor).toInt(),
                (original.height * scaleFactor).toInt()
            )
            original.recycle()
            scaled
        } else {
            original
        }

        return ByteArrayOutputStream().use { out ->
            val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            bitmap.recycle()
            if (!compressed) throw IOException("Failed to compress bitmap for uri=$uri")
            out.toByteArray()
        }
    }
}
