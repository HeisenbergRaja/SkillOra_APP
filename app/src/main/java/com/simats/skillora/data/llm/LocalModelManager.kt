package com.simats.skillora.data.llm

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class LocalModelManager(private val context: Context) {
    private val TAG = "LocalModelManager"
    private val _modelStatus = MutableStateFlow(ModelStatus())
    val modelStatus: StateFlow<ModelStatus> = _modelStatus

    private val config = LocalModelConfig()
    private val modelDir = File(context.filesDir, "models")

    init {
        if (!modelDir.exists()) {
            modelDir.mkdirs()
        }
        extractBundledModel()
    }

    fun checkModelStatus() {
        val modelFile = File(modelDir, config.modelFileName)
        if (modelFile.exists()) {
            _modelStatus.value = ModelStatus(
                state = ModelState.INSTALLED,
                fileSize = modelFile.length(),
                modelPath = modelFile.absolutePath
            )
        } else {
            _modelStatus.value = ModelStatus(state = ModelState.NOT_INSTALLED)
        }
    }

    private fun extractBundledModel() {
        val modelFile = File(modelDir, config.modelFileName)
        if (modelFile.exists()) {
            if (modelFile.length() < 1024 * 1024) { // Less than 1MB is a dummy/placeholder
                Log.w(TAG, "Found suspiciously small model file (${modelFile.length()} bytes). Deleting to force re-extraction.")
                modelFile.delete()
            } else {
                checkModelStatus()
                return
            }
        }

        _modelStatus.value = ModelStatus(state = ModelState.INSTALLING, progress = 0f)
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.assets.open("models/${config.modelFileName}")
                val outputStream = FileOutputStream(modelFile)
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.close()
                inputStream.close()
                
                checkModelStatus()
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting bundled model", e)
                _modelStatus.value = ModelStatus(state = ModelState.ERROR, errorMessage = "Failed to extract bundled model")
            }
        }
    }

    fun removeModel() {
        val modelFile = File(modelDir, config.modelFileName)
        if (modelFile.exists()) {
            modelFile.delete()
        }
        checkModelStatus()
    }
}
