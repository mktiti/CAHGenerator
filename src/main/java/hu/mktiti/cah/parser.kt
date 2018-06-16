package hu.mktiti.cah

import java.io.File
import java.io.IOException
import java.util.logging.Level

object Parser {

    private val log by logger()

    private fun <T> load(filePath: String, parser: (String) -> T, name: String): List<T>? {
        val file = File(filePath)

        if (!file.exists()) {
            log.info { "Skipping loading of $name, file doesn't exists" }
            return null
        }

        return try {
            file.readLines().filter { line ->
                !line.trimStart().startsWith("#") && line.isNotBlank()
            }.map(parser)
        } catch (ioe: IOException) {
            log.log(Level.SEVERE, ioe) { "Error while parsing $name!" }
            null
        }
    }

    fun loadQuestions(file: String): List<QuestionCard>? = load(file, ::parseQuestion, "questions")

    fun loadAnswers(file: String): List<AnswerCard>? = load(file, ::AnswerCard, "answers")

}