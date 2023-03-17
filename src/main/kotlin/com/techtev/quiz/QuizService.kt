package com.techtev.quiz

import arrow.core.*
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull

interface QuizService {
    fun saveQuiz(
        title: String,
        question: String,
        answerIndexes: List<Int>,
        options: List<String>
    ): Either<DomainError, QuizId>

    fun getQuiz(id: Long): Either<PersistenceError, Quiz?>

    fun answerQuiz(id: Long, answerIndexes: List<Int>): Either<DomainError, AnswerResult>
}

fun quizService(
    quizRepository: QuizRepository
): QuizService = object : QuizService {
    override fun saveQuiz(
        title: String,
        question: String,
        answerIndexes: List<Int>,
        options: List<String>
    ): Either<DomainError, QuizId> = either {
        val validatedQuiz = Either.zipOrAccumulate(
            Title.from(title).toEitherNel(),
            Text.from(question).toEitherNel(),
            AnswerIndex.from(answerIndexes, options),
            Option.from(options)
        ) { title: Title, text: Text, answer: List<AnswerIndex>, options: List<Option> ->
            Quiz(title = title, text = text, answer = answer, options = options, userId = null)
        }.mapLeft(::IncorrectFields)
            .bind()
        quizRepository.createNewQuiz(validatedQuiz).bind()
    }

    override fun getQuiz(id: Long): Either<PersistenceError, Quiz?> =
        quizRepository.getQuiz(QuizId(id))

    override fun answerQuiz(id: Long, answerIndexes: List<Int>): Either<DomainError, AnswerResult> =
        either {
            val quiz = ensureNotNull(getQuiz(id).bind()) { AnsweredQuizDoesNotExist(id) }
            AnswerResult(quiz.answer.map { it.value }.toSet() == answerIndexes.toSet())
        }
}