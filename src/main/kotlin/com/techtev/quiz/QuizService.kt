package com.techtev.quiz

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.toEitherNel

interface QuizService {
    fun saveQuiz(
        title: String,
        question: String,
        answerIndexes: List<Int>,
        options: List<String>,
        userEmail: String
    ): Either<DomainError, QuizId>

    fun getQuiz(id: QuizId): Either<PersistenceError, Quiz?>

    fun answerQuiz(id: QuizId, answerIndexes: List<Int>): Either<DomainError, AnswerResult>
}

fun quizService(
    quizRepository: QuizRepository,
    userRepository: UserRepository
): QuizService = object : QuizService {
    override fun saveQuiz(
        title: String,
        question: String,
        answerIndexes: List<Int>,
        options: List<String>,
        userEmail: String
    ): Either<DomainError, QuizId> = either {
        val user = userRepository.getUserFromEmail(userEmail).bind()
        val validatedQuiz = Either.zipOrAccumulate(
            Title.from(title).toEitherNel(),
            Text.from(question).toEitherNel(),
            AnswerIndex.from(answerIndexes, options),
            Option.from(options)
        ) { title: Title, text: Text, answer: List<AnswerIndex>, options: List<Option> ->
            Quiz(title = title, text = text, answer = answer, options = options, userId = user?.id?.id)
        }.mapLeft(::IncorrectFields)
            .bind()
        quizRepository.createNewQuiz(validatedQuiz).bind()
    }

    override fun getQuiz(id: QuizId): Either<PersistenceError, Quiz?> =
        quizRepository.getQuiz(id)

    override fun answerQuiz(id: QuizId, answerIndexes: List<Int>): Either<DomainError, AnswerResult> =
        either {
            val quiz = ensureNotNull(getQuiz(id).bind()) { AnsweredQuizDoesNotExist(id) }
            AnswerResult(quiz.answer.map { it.value }.toSet() == answerIndexes.toSet())
        }
}