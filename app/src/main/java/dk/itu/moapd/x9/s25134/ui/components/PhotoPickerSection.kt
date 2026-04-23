package dk.itu.moapd.x9.s25134.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import dk.itu.moapd.x9.s25134.R
import java.io.File

private const val TAG = "PhotoPickerSection"

private fun createCameraOutputUri(context: Context): Pair<File, Uri> {
    // File.createTempFile is an IO operation. It is fast enough in practice
    // not to cause an ANR (no data is written here), but it does trigger
    // StrictMode in debug builds. For this exercise scope this is acceptable.
    val dir = File(context.cacheDir, "camera_images").also { it.mkdirs() }
    val file = File.createTempFile("report_photo_", ".jpg", dir)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    return file to uri
}

@Composable
fun PhotoPickerSection(
    selectedImageUri: Uri?,
    existingImageUrl: String?,
    photoExplicitlyRemoved: Boolean,
    onPhotoSelected: (Uri) -> Unit,
    onRemovePhoto: () -> Unit
) {
    val context = LocalContext.current

    // rememberSaveable is required: TakePicture opens an external Activity and Android
    // may recreate the caller while the camera is in the foreground. Uri is Parcelable.
    val cameraOutputUri = rememberSaveable { mutableStateOf<Uri?>(null) }
    // Plain remember is sufficient: if the process is killed while the camera is open,
    // the temp file persists on disk and cannot be cleaned up until the next launch.
    val cameraOutputFile = remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraOutputUri.value?.let { onPhotoSelected(it) }
        } else {
            // User canceled — delete the orphaned temp file to free space.
            cameraOutputFile.value?.delete()
            cameraOutputFile.value = null
            cameraOutputUri.value = null
        }
    }

    // cameraLauncher must be declared above this block because the permission callback
    // references it via closure.
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                val (file, uri) = createCameraOutputUri(context)
                cameraOutputFile.value = file
                cameraOutputUri.value = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create temp file: ${e.message}")
            }
        }
    }

    // PickVisualMedia uses the Android Photo Picker on API 33+ and falls back to
    // ACTION_GET_CONTENT on older devices. No CAMERA or READ_MEDIA_IMAGES permission needed.
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onPhotoSelected(uri)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_spacing))
    ) {
        OutlinedButton(
            onClick = {
                val hasCameraPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (hasCameraPermission) {
                    try {
                        val (file, uri) = createCameraOutputUri(context)
                        cameraOutputFile.value = file
                        cameraOutputUri.value = uri
                        cameraLauncher.launch(uri)
                    } catch (e: Exception) {
                        Log.e(TAG, "Camera launch failed: ${e.message}")
                    }
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_size_small))
            )
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_xs)))
            Text(stringResource(R.string.button_camera))
        }

        OutlinedButton(
            onClick = {
                // PickVisualMediaRequest wraps the media type filter.
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_size_small))
            )
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_xs)))
            Text(stringResource(R.string.button_gallery))
        }
    }

    // Preview logic:
    // 1. A newly picked local URI takes priority.
    // 2. If no new URI and the user has not explicitly removed the photo,
    //    show the existing remote URL (edit mode only).
    // 3. Otherwise, no preview.
    val showingNewImage = selectedImageUri != null
    val showingExistingImage = !showingNewImage && !photoExplicitlyRemoved && existingImageUrl != null

    if (showingNewImage || showingExistingImage) {
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
        AsyncImage(
            model = if (showingNewImage) selectedImageUri else existingImageUrl,
            contentDescription = stringResource(R.string.cd_report_photo),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.map_embed_height))
                .clip(RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)))
        )
        TextButton(onClick = {
            // Clean up internal camera temp file reference before signalling removal.
            cameraOutputFile.value?.delete()
            cameraOutputFile.value = null
            cameraOutputUri.value = null
            onRemovePhoto()
        }) {
            Text(stringResource(R.string.button_remove_photo))
        }
    }
}
