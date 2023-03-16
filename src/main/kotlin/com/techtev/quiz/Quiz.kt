package com.techtev.quiz

import arrow.core.*

@JvmInline
value class QuizId(val value: Long)

@JvmInline
value class AnswerIndex(val value: Int) {
    companion object {
        private fun from(value: Int, options: List<String>): Either<FieldValidationFailure, AnswerIndex> =
            if (value in options.indices) AnswerIndex(value).right() else AnswerIndexOutOfBounds(
                value,
                options.lastIndex
            ).left()
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
            if (value.isNotEmpty()) Title(value).right() else Empty("title").left()
    }
}

@JvmInline
value class Text constructor(val value: String) {
    companion object {
        fun from(value: String): Either<FieldValidationFailure, Text> =
            if (value.isNotEmpty()) Text(value).right() else Empty("text").left()
    }
}

@JvmInline
value class Option constructor(val value: String) {
    companion object {
        private fun from(value: String, index: Int): Either<FieldValidationFailure, Option> =
            if (value.isNotEmpty()) Option(value).right() else Empty("option #$index").left()
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
    val options: List<Option>
)