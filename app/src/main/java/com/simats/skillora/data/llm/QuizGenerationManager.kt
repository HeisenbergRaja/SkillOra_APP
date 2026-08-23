package com.simats.skillora.data.llm

import com.simats.skillora.data.FinalQuiz
import com.simats.skillora.data.QuizQuestion
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.ceil

class QuizGenerationManager(
    private val modelManager: LocalModelManager,
    private val engine: LocalLLMEngine
) {
    private val TAG = "QuizGenerationManager"
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    class QuizGenerationException(message: String, val rawOutput: String? = null) : Exception(message)

    suspend fun generateQuiz(skillTitle: String, pastedPrompt: String, questionCount: Int = 10): Result<FinalQuiz> {
        if (!LocalLLMEngine.isAvailable()) {
            return Result.failure(Exception("Native AI engine not loaded."))
        }

        val status = modelManager.modelStatus.value
        if (status.state != ModelState.INSTALLED || status.modelPath == null) {
            return Result.failure(Exception("Local AI model is not installed."))
        }

        _isGenerating.value = true
        return withContext(Dispatchers.Default) {
            try {
                val config = LocalModelConfig(
                    maxTokens = LocalQuizConfig.MAX_QUIZ_OUTPUT_TOKENS,
                    temperature = 0.2f
                )
                
                if (engine.engineState.value != ModelState.MODEL_READY) {
                    val initResult = engine.initialize(status.modelPath, config)
                    if (initResult.isFailure) {
                        return@withContext Result.failure(Exception("Native engine failed to initialize"))
                    }
                }

                val QUIZ_BATCH_SIZE = 5
                val numBatches = ceil(questionCount.toDouble() / QUIZ_BATCH_SIZE).toInt()
                val allQuestions = mutableListOf<QuizQuestion>()
                
                var baseUserPrompt = pastedPrompt.trim()

                Log.d(TAG, "QUIZ_REQUESTED_COUNT=$questionCount")
                
                var loopSafetyCounter = 0
                val MAX_LOOPS = numBatches + 5 // Prevent infinite hallucination loops

                while (allQuestions.size < questionCount && loopSafetyCounter < MAX_LOOPS) {
                    loopSafetyCounter++
                    val questionsToGenerate = minOf(QUIZ_BATCH_SIZE, questionCount - allQuestions.size)

                    val sysPrompt = QuizSystemPrompt.INSTRUCTIONS
                    var promptText = QuizSystemPrompt.buildUserPrompt(baseUserPrompt, questionsToGenerate)
                    
                    var chatMlPrompt = "<|im_start|>system\n${sysPrompt}<|im_end|>\n<|im_start|>user\n$promptText<|im_end|>\n<|im_start|>assistant\n{"

                    var sysTokens = sysPrompt.length / 4
                    var userTokens = promptText.length / 4
                    var finalTokens = chatMlPrompt.length / 4
                    
                    try {
                        val countResult = engine.countTokens(chatMlPrompt)
                        if (countResult.isSuccess) {
                            finalTokens = countResult.getOrThrow()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Token count failed", e)
                    }

                    Log.d(TAG, "QUIZ_INPUT_TOKEN_CHECK_START")
                    Log.d(TAG, "SYSTEM_PROMPT_TOKENS=$sysTokens")
                    Log.d(TAG, "USER_PROMPT_TOKENS=$userTokens")
                    Log.d(TAG, "TOTAL_INPUT_TOKENS=$finalTokens")
                    Log.d(TAG, "QUIZ_INPUT_TOKEN_LIMIT=500")
                    Log.d(TAG, "QUIZ_INPUT_TOKEN_CHECK_END")

                    if (finalTokens >= 450) {
                        Log.w(TAG, "Input tokens >= 450, applying fallback compression")
                        baseUserPrompt = QuizPromptCompressor.compress(baseUserPrompt)
                        promptText = QuizSystemPrompt.buildUserPrompt(baseUserPrompt, questionsToGenerate)
                        chatMlPrompt = "<|im_start|>system\n${sysPrompt}<|im_end|>\n<|im_start|>user\n$promptText<|im_end|>\n<|im_start|>assistant\n{"
                        
                        try {
                            val countResult = engine.countTokens(chatMlPrompt)
                            if (countResult.isSuccess) finalTokens = countResult.getOrThrow()
                            else finalTokens = chatMlPrompt.length / 4
                        } catch (e: Exception) {
                            finalTokens = chatMlPrompt.length / 4
                        }
                    }

                    if (finalTokens >= 500) {
                        Log.e(TAG, "QUIZ_PROMPT_TOO_LARGE")
                        return@withContext Result.failure(Exception("PROMPT_TOO_LARGE"))
                    }

                    Log.d(TAG, "QUIZ_CONTEXT_RESET_START")
                    engine.cancel() 
                    val initResult = engine.initialize(status.modelPath, config)
                    if (initResult.isSuccess) {
                        Log.d(TAG, "CONTEXT_RESET=SUCCESS")
                    } else {
                        Log.d(TAG, "CONTEXT_RESET=FAILED")
                        Log.d(TAG, "QUIZ_CONTEXT_RESET_END")
                        return@withContext Result.failure(Exception("CONTEXT_RESET_FAILED"))
                    }
                    Log.d(TAG, "QUIZ_CONTEXT_RESET_END")

                    Log.d(TAG, "QUIZ_GENERATION_START")
                    val generationResult = engine.generate(chatMlPrompt, config)
                    
                    if (generationResult.isFailure) {
                        Log.e(TAG, "QUIZ_NATIVE_ERROR")
                        return@withContext Result.failure(Exception("NATIVE_ENGINE_ERROR"))
                    }

                    val rawOutput = "{" + generationResult.getOrThrow()
                    Log.d(TAG, "GENERATED_OUTPUT_LENGTH=${rawOutput.length}")
                    
                    Log.d(TAG, "RAW_OUTPUT_OBJECT_LENGTH=${rawOutput.length}")
                    Log.d(TAG, "RAW_OUTPUT_FIRST_CHAR=${rawOutput.firstOrNull()}")
                    Log.d(TAG, "RAW_OUTPUT_LAST_CHAR=${rawOutput.lastOrNull()}")
                    Log.d(TAG, "RAW_OUTPUT_START")
                    // Split log if too long, but usually standard logcat handles ~4k
                    val chunkSize = 3500
                    for (i in 0 until rawOutput.length step chunkSize) {
                        val end = minOf(i + chunkSize, rawOutput.length)
                        Log.d(TAG, rawOutput.substring(i, end))
                    }
                    Log.d(TAG, "RAW_OUTPUT_END")

                    val extractedJson = extractQuizJson(rawOutput)
                    
                    if (extractedJson == null) {
                        return@withContext Result.failure(Exception("JSON_INVALID"))
                    } else {
                        val parsedBatch = parseQuestions(extractedJson, questionsToGenerate)
                        if (parsedBatch.isSuccess) {
                            val validBatch = parsedBatch.getOrThrow()
                            allQuestions.addAll(validBatch)
                            
                            // Deduplicate
                            val originalSize = allQuestions.size
                            val distinctQuestions = allQuestions.distinctBy { it.question }
                            allQuestions.clear()
                            allQuestions.addAll(distinctQuestions)
                            
                            if (allQuestions.size < originalSize) {
                                Log.w(TAG, "Removed ${originalSize - allQuestions.size} duplicate questions.")
                            }
                            Log.d(TAG, "QUIZ_JSON_PARSE_SUCCESS")
                            Log.d(TAG, "QUIZ_VALIDATION_SUCCESS")
                        } else {
                            // parseQuestions logs QUIZ_JSON_INVALID itself.
                            return@withContext Result.failure(Exception("JSON_INVALID"))
                        }
                    }
                }

                if (allQuestions.size != questionCount) {
                    Log.e(TAG, "QUIZ_QUESTION_COUNT_MISMATCH")
                    Log.e(TAG, "EXPECTED=$questionCount")
                    Log.e(TAG, "ACTUAL=${allQuestions.size}")
                    return@withContext Result.failure(Exception("QUESTION_COUNT_MISMATCH"))
                }
                
                Log.d(TAG, "QUIZ_PARSED_COUNT=${allQuestions.size}")

                val finalQuiz = FinalQuiz(
                    quizTitle = "$skillTitle Final Assessment",
                    totalQuestions = allQuestions.size,
                    passingScore = 70,
                    questions = allQuestions,
                    status = "GENERATED"
                )
                
                Result.success(finalQuiz)

            } catch (e: Exception) {
                Log.e(TAG, "Error during quiz generation", e)
                Result.failure(e)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    /**
     * Public helper to parse and validate a complete Quiz JSON
     */
    fun parseAndValidate(jsonStr: String, expectedCount: Int, skillTitle: String): Result<FinalQuiz> {
        return try {
            val questions = parseQuestions(jsonStr, expectedCount).getOrThrow()
            val finalQuiz = FinalQuiz(
                quizTitle = "$skillTitle Final Assessment",
                totalQuestions = questions.size,
                passingScore = 70,
                questions = questions,
                status = "GENERATED"
            )
            Result.success(finalQuiz)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseQuestions(jsonStr: String, expectedCount: Int): Result<List<QuizQuestion>> {
        Log.d(TAG, "QUIZ_JSON_PARSE_START")
        return try {
            val json = JSONObject(jsonStr)
            
            Log.d(TAG, "QUIZ_VALIDATION_START")
            
            if (!json.has("questions")) {
                Log.e(TAG, "QUIZ_JSON_INVALID: Missing questions array")
                return Result.failure(Exception("JSON missing 'questions' root key."))
            }
            
            val questionsArray = json.getJSONArray("questions")
            
            val parsedQuestions = mutableListOf<QuizQuestion>()
            for (i in 0 until questionsArray.length()) {
                val qObj = questionsArray.getJSONObject(i)
                
                val questionText = qObj.optString("question").trim()
                if (questionText.isEmpty()) {
                    Log.e(TAG, "QUIZ_JSON_INVALID: Question ${i+1} has empty question text")
                    return Result.failure(Exception("Empty question text."))
                }

                val optionsArray = qObj.optJSONArray("options")
                if (optionsArray == null || optionsArray.length() != 4) {
                    Log.e(TAG, "QUIZ_JSON_INVALID: Question ${i+1} has ${optionsArray?.length() ?: 0} options, expected 4")
                    return Result.failure(Exception("Incorrect options count."))
                }
                
                val options = mutableListOf<String>()
                for (j in 0 until 4) {
                    val opt = optionsArray.optString(j).trim()
                    if (opt.isEmpty()) {
                        Log.e(TAG, "QUIZ_JSON_INVALID: Question ${i+1} option ${j+1} is empty")
                        return Result.failure(Exception("Empty option."))
                    }
                    options.add(opt)
                }

                if (!qObj.has("correctAnswer")) {
                    Log.e(TAG, "QUIZ_JSON_INVALID: Question ${i+1} missing correctAnswer")
                    return Result.failure(Exception("Missing correctAnswer."))
                }
                
                val rawAns = qObj.optString("correctAnswer")
                val correctAnswer = rawAns.toIntOrNull()
                
                if (correctAnswer == null || correctAnswer !in 0..3) {
                    Log.e(TAG, "QUIZ_JSON_INVALID: Question ${i+1} correctAnswer out of range")
                    return Result.failure(Exception("Invalid correctAnswer."))
                }

                val explanation = qObj.optString("explanation").trim()
                if (explanation.isEmpty()) {
                    Log.e(TAG, "QUIZ_JSON_INVALID: Question ${i+1} explanation is empty")
                    return Result.failure(Exception("Empty explanation."))
                }

                parsedQuestions.add(QuizQuestion(
                    questionId = UUID.randomUUID().toString(),
                    question = questionText,
                    options = options,
                    correctAnswer = correctAnswer,
                    explanation = explanation,
                    difficulty = "medium",
                    topic = "",
                    dayNumber = 1
                ))
            }

            if (parsedQuestions.isEmpty()) {
                Log.e(TAG, "QUIZ_JSON_INVALID: No valid questions found in JSON")
                return Result.failure(Exception("No valid questions found in JSON."))
            }
            
            if (parsedQuestions.size != expectedCount) {
                // Return success here? No, returning failure per strictly handling Count vs Invalid JSON.
                // Actually the rules say: "Do not report QUESTION_COUNT_MISMATCH when JSON itself is malformed."
                // But this JSON IS well-formed, it just has the wrong count.
                // We'll return success from the parse layer, and the caller layer checks the count to throw MISMATCH.
                Log.d(TAG, "QUIZ_VALIDATION_SUCCESS")
                Log.d(TAG, "QUIZ_FINAL_COUNT=${parsedQuestions.size}")
                return Result.success(parsedQuestions)
            }

            Log.d(TAG, "QUIZ_VALIDATION_SUCCESS")
            Log.d(TAG, "QUIZ_FINAL_COUNT=${parsedQuestions.size}")
            Log.d(TAG, "QUIZ_REVIEW_READY")
            Result.success(parsedQuestions)
        } catch (e: Exception) {
            Log.e(TAG, "QUIZ_JSON_INVALID: JSON Parse Error", e)
            Result.failure(e)
        }
    }

    fun cancel() {
        engine.cancel()
        _isGenerating.value = false
    }

    private fun extractQuizJson(rawOutput: String): String? {
        val trimmed = rawOutput.trim()

        Log.d(TAG, "QUIZ_JSON_EXTRACTION_START")
        Log.d(TAG, "RAW_TRIMMED_LENGTH=${trimmed.length}")
        Log.d(TAG, "TRIMMED_FIRST_CHAR=${trimmed.firstOrNull()}")
        Log.d(TAG, "TRIMMED_LAST_CHAR=${trimmed.lastOrNull()}")

        // FIRST: direct JSON parsing
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                JSONObject(trimmed)
                Log.d(TAG, "QUIZ_JSON_DIRECT_PARSE_SUCCESS")
                Log.d(TAG, "EXTRACTED_JSON_LENGTH=${trimmed.length}")
                Log.d(TAG, "QUIZ_JSON_START")
                Log.d(TAG, trimmed)
                Log.d(TAG, "QUIZ_JSON_END")
                return trimmed
            } catch (e: Exception) {
                Log.e(TAG, "QUIZ_JSON_DIRECT_PARSE_FAILED", e)
            }
        }

        // SECOND: fallback extraction
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')

        if (start >= 0 && end > start) {
            val extracted = trimmed.substring(start, end + 1)
            try {
                JSONObject(extracted)
                Log.d(TAG, "QUIZ_JSON_FALLBACK_EXTRACTION_SUCCESS")
                Log.d(TAG, "EXTRACTED_JSON_LENGTH=${extracted.length}")
                Log.d(TAG, "QUIZ_JSON_START")
                Log.d(TAG, extracted)
                Log.d(TAG, "QUIZ_JSON_END")
                return extracted
            } catch (e: Exception) {
                Log.e(TAG, "QUIZ_JSON_FALLBACK_PARSE_FAILED", e)
            }
        }

        Log.e(TAG, "QUIZ_JSON_EXTRACTION_FAILED")
        return null
    }
}
