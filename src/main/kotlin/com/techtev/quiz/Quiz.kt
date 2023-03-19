package com.techtev.quiz

import arrow.core.*
import arrow.core.raise.either
import arrow.core.raise.ensure

@JvmInline
value class QuizId(val value: Long)

@JvmInline
value class AnswerResult(val isCorrect: Boolean)

@JvmInline
value class AnswerIndex(val value: Int) {
    companion object {
        private fun from(value: Int, options: List<String>): Either<FieldValidationFailure, AnswerIndex> =
            either {
                ensure(value in options.indices) { AnswerIndexOutOfBounds(value, options.lastIndex) }
                AnswerIndex(value)
            }

        fun from(values: List<Int>, options: List<String>): Either<NonEmptyList<FieldValidationFailure>, List<AnswerIndex>> =
            values.mapOrAccumulate { answer ->
                from(answer, options).bind()
            }
    }
}

@JvmInline
value class Title constructor(val value: String) {
    companion object {
        fun from(value: String): Either<FieldValidationFailure, Title> =
            either {
                ensure(value.isNotEmpty()) { Empty("title") }
                Title(value)
            }
    }
}

@JvmInline
value class Text constructor(val value: String) {
    companion object {
        fun from(value: String): Either<FieldValidationFailure, Text> =
            either {
                ensure(value.isNotEmpty()) { Empty("text") }
                Text(value)
            }
    }
}

@JvmInline
value class Option constructor(val value: String) {
    companion object {
        private fun from(value: String, index: Int): Either<FieldValidationFailure, Option> =
            either {
                ensure(value.isNotEmpty()) { Empty("option #$index") }
                Option(value)
            }

        fun from(values: List<String>): Either<NonEmptyList<FieldValidationFailure>, List<Option>> =
            values.mapOrAccumulate { s: String ->
                from(s, values.indexOf(s)).bind()
            }
    }
}

data class Quiz(
    val id: QuizId = QuizId(-1),
    val answer: List<AnswerIndex>,
    val title: Title,
    val text: Text,
    val options: List<Option>,
    val userId: Long?
)