package com.simats.skillora.data.llm

data class LocalModelConfig(
    val modelName: String = "Qwen3 1.7B",
    val modelFileName: String = "qwen3-1.7b-q4_k_m.gguf",
    val quantization: String = "Q4_K_M",
    val contextSize: Int = 2048,
    val threads: Int = 4,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1024
)

enum class ModelState {
    NOT_INSTALLED,
    INSTALLING,
    INSTALLED,
    ERROR,
    MODEL_NOT_FOUND,
    MODEL_INVALID,
    MODEL_INCOMPATIBLE,
    MODEL_LOAD_FAILED,
    OUT_OF_MEMORY,
    MODEL_READY
}

data class ModelStatus(
    val state: ModelState = ModelState.NOT_INSTALLED,
    val progress: Float = 0f,
    val errorMessage: String? = null,
    val fileSize: Long = 0,
    val modelPath: String? = null
)
