package com.simats.skillora.data.llm

import android.util.Log

object QuizPromptCompressor {
    private const val TAG = "QuizPromptCompressor"

    /**
     * Cleans and compacts the prompt.
     * Removes markdown fences, greetings, and excessive whitespace.
     * Truncates aggressively if it still exceeds SAFE_PROMPT_CHARS.
     */
    fun compress(input: String): String {
        var compressed = input

        // 1. Remove Markdown formatting and code fences
        compressed = compressed.replace(Regex("```json", RegexOption.IGNORE_CASE), "")
        compressed = compressed.replace(Regex("```.*", RegexOption.IGNORE_CASE), "")
        compressed = compressed.replace("```", "")
        compressed = compressed.replace(Regex("\\*\\*"), "") // bold

        // 2. Remove common conversational fluff
        val fluff = listOf(
            "Here is the quiz specification:",
            "Here is the quiz specification you requested:",
            "Sure, here is the compact format:",
            "Certainly!",
            "Here is the compact quiz specification for Skillora's local Qwen model:",
            "Here is the compact quiz specification:"
        )
        for (phrase in fluff) {
            compressed = compressed.replace(phrase, "", ignoreCase = true)
        }

        // 3. Condense whitespace
        compressed = compressed.replace(Regex("\n{3,}"), "\n\n").trim()

        Log.d(TAG, "QUIZ_PROMPT_ORIGINAL_TOKENS=~${input.length / 4}")
        Log.d(TAG, "QUIZ_PROMPT_COMPRESSED_TOKENS=~${compressed.length / 4}")

        // 4. Truncate if still too large, while trying to preserve structure
        if (compressed.length > LocalQuizConfig.SAFE_PROMPT_CHARS) {
            Log.w(TAG, "Prompt still too large (${compressed.length} chars). Truncating aggressively.")
            
            // We keep the beginning (where topics/concepts usually are) and end, removing the middle.
            val keepStart = LocalQuizConfig.SAFE_PROMPT_CHARS / 2
            val keepEnd = LocalQuizConfig.SAFE_PROMPT_CHARS / 2
            
            if (compressed.length > keepStart + keepEnd) {
                 compressed = compressed.substring(0, keepStart) + "\n...[TRUNCATED]...\n" + compressed.substring(compressed.length - keepEnd)
            }
        }

        Log.d(TAG, "QUIZ_PROMPT_FINAL_TOKENS=~${compressed.length / 4}")
        
        return compressed
    }
}
