package com.simats.skillora.data.llm

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LocalLLMEngine {
    private val TAG = "LocalLLMEngine"
    
    private val mutex = Mutex()

    companion object {
        private var isLibraryLoaded = false
        private var nativeLoadError: String? = null
        
        init {
            try {
                System.loadLibrary("ggml-base")
                System.loadLibrary("ggml-cpu")
                System.loadLibrary("ggml")
                System.loadLibrary("llama")
                System.loadLibrary("llama-android")
                isLibraryLoaded = true
            } catch (e: Throwable) {
                nativeLoadError = e.message ?: e.toString()
                Log.e("LocalLLMEngine", "Failed to load llama-android library: $nativeLoadError", e)
            }
        }
        
        fun isAvailable() = isLibraryLoaded
        fun getNativeError() = nativeLoadError
    }

    // Native methods (JNI)
    private external fun loadModelNative(modelPath: String, contextSize: Int, threads: Int): Long
    private external fun generateNative(contextPtr: Long, prompt: String, maxTokens: Int, temperature: Float): String
    private external fun releaseNative(contextPtr: Long)
    private external fun cancelNative(contextPtr: Long)
    private external fun countTokensNative(contextPtr: Long, text: String): Int

    private val _engineState = MutableStateFlow<ModelState>(ModelState.NOT_INSTALLED)
    val engineState: StateFlow<ModelState> = _engineState.asStateFlow()

    private var contextPtr: Long = 0

    suspend fun countTokens(text: String): Result<Int> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (contextPtr == 0L) {
                return@withLock Result.failure(Exception("Engine not initialized"))
            }
            try {
                val count = countTokensNative(contextPtr, text)
                Result.success(count)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun initialize(modelPath: String, config: LocalModelConfig): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val file = java.io.File(modelPath)
                Log.i(TAG, "Kotlin file check: path=$modelPath, exists=${file.exists()}, canRead=${file.canRead()}, length=${file.length()}")
                
                if (contextPtr != 0L) {
                    releaseNative(contextPtr)
                    contextPtr = 0L
                }
                val newContextPtr = loadModelNative(modelPath, config.contextSize, config.threads)
                
                if (newContextPtr in -4L..0L) {
                    val errorMsg = when (newContextPtr) {
                        -1L -> { _engineState.value = ModelState.MODEL_NOT_FOUND; "MODEL_NOT_FOUND" }
                        -2L -> { _engineState.value = ModelState.MODEL_INVALID; "MODEL_INVALID (File size too small)" }
                        -3L -> { _engineState.value = ModelState.MODEL_LOAD_FAILED; "MODEL_LOAD_FAILED (Native loading failed, check logcat)" }
                        -4L -> { _engineState.value = ModelState.OUT_OF_MEMORY; "OUT_OF_MEMORY (Context initialization failed)" }
                        0L -> { _engineState.value = ModelState.ERROR; "Failed to load model in native engine (Null pointer)" }
                        else -> { _engineState.value = ModelState.ERROR; "Failed to load model in native engine (Generic Error)" }
                    }
                    Log.e(TAG, "Initialization failed: $errorMsg")
                    Result.failure(Exception(errorMsg))
                } else {
                    Log.i(TAG, "Native initialization result = SUCCESS. Pointer: $newContextPtr")
                    contextPtr = newContextPtr
                    _engineState.value = ModelState.MODEL_READY
                    Result.success(Unit)
                }
            } catch (e: UnsatisfiedLinkError) {
                _engineState.value = ModelState.ERROR
                Log.e(TAG, "Native method not found", e)
                Result.failure(Exception("Native library not loaded correctly"))
            } catch (e: Exception) {
                _engineState.value = ModelState.ERROR
                Result.failure(e)
            }
        }
    }

    suspend fun generate(prompt: String, config: LocalModelConfig): Result<String> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (contextPtr == 0L) {
                return@withLock Result.failure(Exception("Engine not initialized"))
            }
            try {
                val result = generateNative(contextPtr, prompt, config.maxTokens, config.temperature)
                if (result.isBlank()) {
                    Result.failure(Exception("Generation failed or was cancelled"))
                } else {
                    Result.success(result)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun testInference(config: LocalModelConfig): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (contextPtr == 0L) {
                return@withContext Result.failure(Exception("Model not initialized"))
            }
            val prompt = "<|im_start|>system\nYou are a test assistant. Reply exactly with: LLAMA_ANDROID_TEST_SUCCESS and nothing else.<|im_end|>\n<|im_start|>user\nOutput the test string.<|im_end|>\n<|im_start|>assistant\n"
            val result = generateNative(contextPtr, prompt, 20, 0.1f)
            if (result.contains("LLAMA_ANDROID_TEST_SUCCESS")) {
                Result.success("LLAMA_ANDROID_TEST_SUCCESS")
            } else {
                Result.failure(Exception("Test inference failed. Model generated: ${result}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun release() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (contextPtr != 0L) {
                releaseNative(contextPtr)
                contextPtr = 0L
            }
        }
    }

    fun cancel() {
        if (contextPtr != 0L) {
            cancelNative(contextPtr)
        }
    }
}
