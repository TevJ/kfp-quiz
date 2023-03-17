package com.techtev.quiz

import kotlinx.serialization.Serializable
import org.http4k.contract.ContractRoute
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.core.*
import org.http4k.format.KotlinxSerialization.auto
import org.http4k.lens.*

@Serializable
data class QuizRequest(
    val title: String,
    val text: String,
    val options: List<String>,
    val answers: List<Int>
)

@Serializable
data class QuizResponse(
    val id: Long,
    val title: String,
    val text: String,
    val options: List<String>,
    val answers: List<Int>
)

@Serializable
data class ErrorResponse(
    val errors: List<String>
)

@Serializable
data class AnswerQuizRequest(
    val answers: List<Int>
)

@Serializable
data class AnswerQuizResponse(
    val isCorrect: Boolean
)

fun quizRoutes(
    quizService: QuizService,
    credentialsKey: RequestContextLens<Credentials>
): List<ContractRoute> =
    listOf(
        createQuizRoute(quizService, credentialsKey),
        getQuizRoute(quizService),
        answerQuizRoute(quizService)
    )


fun createQuizRoute(
    quizService: QuizService,
    credentialsKey: RequestContextLens<Credentials>
): ContractRoute {
    val quizRequestBody = Body.auto<QuizRequest>().toLens()
    val quizResponseBody = Body.auto<QuizResponse>().toLens()
    val quizErrorBody = Body.auto<ErrorResponse>().toLens()
    val quizExampleRequest = quizExampleRequest()
    val quizExampleResponse = quizExampleResponse(quizExampleRequest)

    val spec = "/quiz/create" meta {
        summary = "Upload a new quiz"
        receiving(quizRequestBody to quizExampleRequest)
        returning(Status.OK, quizResponseBody to quizExampleResponse)
    } bindContract Method.POST

    val quizHandler: HttpHandler = { request: Request ->
        val receivedQuiz: QuizRequest = quizRequestBody(request)
        val userCredentials = credentialsKey(request)
        quizService.saveQuiz(
            receivedQuiz.title,
            receivedQuiz.text,
            receivedQuiz.answers,
            receivedQuiz.options,
            userCredentials.user
        )
            .fold(
                { it.toResponse(quizErrorBody) }
            ) { id ->
                Response(Status.OK).with(quizResponseBody of createQuizResponse(id.value, receivedQuiz))
            }
    }
    return spec to quizHandler
}

fun getQuizRoute(quizService: QuizService): ContractRoute {
    val quizResponseBody = Body.auto<QuizResponse>().toLens()
    val quizErrorBody = Body.auto<ErrorResponse>().toLens()

    val spec = "/quiz" / Path.long().of("id", "Id of quiz to get") meta {
        summary = "Gets a quiz"
        returning(Status.OK, quizResponseBody to quizExampleResponse(quizExampleRequest()))
    } bindContract Method.GET

    fun getQuiz(id: Long): HttpHandler = { _ ->
        quizService.getQuiz(id)
            .fold(
                { e -> e.toResponse(quizErrorBody) },
                { quiz ->
                    quiz?.let {
                        Response(Status.OK).with(quizResponseBody of it.toQuizResponse())
                    } ?: Response(Status.NOT_FOUND)
                }
            )
    }
    return spec to ::getQuiz
}

fun answerQuizRoute(quizService: QuizService): ContractRoute {
    val answerQuizRequestBody = Body.auto<AnswerQuizRequest>().toLens()
    val answerQuizResponseBody = Body.auto<AnswerQuizResponse>().toLens()
    val quizErrorBody = Body.auto<ErrorResponse>().toLens()

    val spec = "/quiz" / Path.long().of("id", "Id of quiz to answer") / "answer" meta {
        summary = "Answer a quiz"
        receiving(answerQuizRequestBody to answerQuizExampleRequest())
        returning(Status.OK, answerQuizResponseBody to answerQuizExampleResponse())
    } bindContract Method.POST

    val answerQuiz: (Long, String) -> HttpHandler = { id: Long, _ ->
        { request: Request ->
            val receivedAnswers: AnswerQuizRequest = answerQuizRequestBody(request)
            quizService.answerQuiz(id, receivedAnswers.answers)
                .fold(
                    { it.toResponse(quizErrorBody) },
                    { answerResult ->
                        Response(Status.OK)
                            .with(answerQuizResponseBody of AnswerQuizResponse(answerResult.isCorrect))
                    }
                )
        }
    }
    return spec to answerQuiz
}

private fun Quiz.toQuizResponse(): QuizResponse =
    QuizResponse(
        id = id.value,
        title = title.value,
        text = text.value,
        options = options.map(Option::value),
        answers = answer.map(AnswerIndex::value)
    )

private fun createQuizResponse(id: Long, quizRequest: QuizRequest): QuizResponse =
    QuizResponse(
        id = id,
        title = quizRequest.title,
        text = quizRequest.text,
        options = quizRequest.options,
        answers = quizRequest.answers
    )

private fun quizExampleRequest() = QuizRequest(
    title = "Technology quiz",
    text = "Which of the following programming languages runs on the JVM?",
    options = listOf("Rust", "TypeScript", "Kotlin", "C"),
    answers = listOf(3)
)

private fun quizExampleResponse(quizRequest: QuizRequest) = QuizResponse(
    id = 1,
    title = quizRequest.title,
    text = quizRequest.text,
    options = quizRequest.options,
    answers = quizRequest.answers
)

private fun answerQuizExampleRequest() = AnswerQuizRequest(listOf(2))

private fun answerQuizExampleResponse() = AnswerQuizResponse(true)