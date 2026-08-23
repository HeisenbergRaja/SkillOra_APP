package com.simats.skillora.data.llm

object LocalQuizConfig {
    /**
     * The target maximum number of prompt tokens to send to the local model.
     * Staying below this ensures fast generation and low memory usage.
     */
    const val MAX_PROMPT_TOKENS = 400

    /**
     * The maximum number of tokens the model is allowed to generate.
     * Prevents unbounded generation. Approx 10 questions = 400-500 tokens.
     */
    const val MAX_QUIZ_OUTPUT_TOKENS = 500

    /**
     * Conservative character limit to fallback on when token counting isn't available.
     * Assuming ~4 characters per token: 400 * 4 = 1600.
     */
    const val SAFE_PROMPT_CHARS = 1600
}
