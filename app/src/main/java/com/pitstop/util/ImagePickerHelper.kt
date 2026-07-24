package com.pitstop.util

import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import android.Manifest

class ImagePickerHelper (
    private val activity: AppCompatActivity,
    private val onImagePicked: (String) -> Unit
){
    private var currentPhotoPath: String? = null

    private val cameraLauncher = activity.registerForActivityResult(ActivityResultContracts.TakePicture()) { berhasil ->
        if (berhasil) {
            currentPhotoPath?.let { onImagePicked(it) }}
        }

    private val galleryLauncher = activity.registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val path = salinKeStorageInternal(it)
            if (path != null) onImagePicked(path)
            else Toast.makeText(activity, "Gagal memuat gambar", Toast.LENGTH_SHORT).show()
        }
    }

    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { diizinkan ->
        if (diizinkan) bukaKamera ()
        else Toast.makeText(activity, "Izin kamera dibutuhkan untuk ambil foto", Toast.LENGTH_SHORT).show()
    }

    fun ambilFoto () {
        val sudahDiizinkan = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (sudahDiizinkan) bukaKamera() else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    fun pilihDariGaleri() {
        galleryLauncher.launch("image/*")
    }

    private fun bukaKamera() {
        val file = File(activity.filesDir, "produk_${System.currentTimeMillis()}.jpg")
        currentPhotoPath = file.absolutePath
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
        cameraLauncher.launch(uri)
    }

    private fun salinKeStorageInternal(uri: Uri): String? {
        return try {
            val input = activity.contentResolver.openInputStream(uri) ?: return null
            val file = File(activity.filesDir, "produk_${System.currentTimeMillis()}.jpg")
            input.use { inp -> file.outputStream().use { out -> inp.copyTo(out) } }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}