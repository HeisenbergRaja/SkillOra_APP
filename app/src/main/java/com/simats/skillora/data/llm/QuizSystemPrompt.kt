package com.simats.skillora.data.llm

object QuizSystemPrompt {
    const val INSTRUCTIONS = "You are Skillora's quiz JSON generator. Output ONLY valid JSON. Root key must be \"questions\". Generate exactly the requested number of questions. Each question must have: question, options, correctAnswer, explanation. Each options array must contain exactly 4 strings. correctAnswer must be a zero-based integer 0-3. All fields must be non-empty. No Markdown, no code fences, no extra text. Never output 5 options. Never output correctAnswer 4."

    fun buildResourceAnalysisPrompt(skillTitle: String, questionCount: Int): String {
        return """
==================================================
RESOURCE ANALYSIS PROMPT
==================================================
Skill: $skillTitle
Target: $questionCount Questions

TASK:
Analyze the course resources and roadmap.
Create a compact, self-contained QUIZ GENERATION PROMPT.
Include all key facts, definitions, and concepts needed to generate $questionCount MCQs.
Do NOT generate the quiz yet.
Return ONLY the self-contained prompt text.
==================================================
""".trimIndent()
    }

    fun buildUserPrompt(quizSource: String, batchCount: Int): String {
        return """
Create exactly $batchCount multiple-choice questions from the supplied content.

Requirements:
- Exactly $batchCount questions.
- Exactly 4 options per question.
- correctAnswer is zero-based: 0,1,2,3.
- Include a short explanation.
- Output JSON only using root key "questions".
- No Markdown or extra text.

Content:
$quizSource
""".trimIndent()
    }
}
