package com.simats.skillora.data

import java.util.UUID

data class QuizQuestion(
    val questionId: String = UUID.randomUUID().toString(),
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctAnswer: Int = 0, // 0=A, 1=B, 2=C, 3=D
    val explanation: String = "",
    val difficulty: String = "medium",
    val topic: String = "",
    val dayNumber: Int = 1
)

data class FinalQuiz(
    val quizId: String = UUID.randomUUID().toString(),
    val quizTitle: String = "Skill Final Assessment",
    val totalQuestions: Int = 0,
    val passingScore: Int = 70,
    val quizVersion: Int = 1,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val questions: List<QuizQuestion> = emptyList(),
    val status: String = "NOT_GENERATED" // NOT_GENERATED, GENERATED, VALIDATED, APPROVED, PUBLISHED
)

data class QuizAttempt(
    val attemptId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val skillId: String = "",
    val quizVersion: Int = 1,
    val score: Int = 0,
    val scorePercentage: Int = 0,
    val correctAnswers: Int = 0,
    val totalQuestions: Int = 0,
    val passed: Boolean = false,
    val attemptedAt: Long = System.currentTimeMillis(),
    val answers: Map<String, Int> = emptyMap() // questionId to selectedOptionIndex
)
