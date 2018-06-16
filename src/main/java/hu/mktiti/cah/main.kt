package hu.mktiti.cah

import com.natpryce.konfig.*
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import org.apache.pdfbox.pdmodel.common.PDRectangle
import java.io.File
import java.util.logging.Level

class Arguments(parser: ArgParser) {
    val configFile by parser.storing("-c", "--config", help = "locations of the configuration file").default("cah.conf")

    val questionsPath by parser.storing("-q", "--questions", help = "locations of the file containing the questions").default("questions.txt")

    val answersPath by parser.storing("-a", "--answers", help = "locations of the file containing the answers").default("answers.txt")

    val questionsOutput by parser.storing("-w", "--questions-output", help = "generated output of questions (white cards)").default("questionsGenerated.pdf")

    val answersOutput by parser.storing("-b", "--answers-output", help = "generated output of answers (black cards)").default("answersGenerated.pdf")
}

object Main {

    object Config {

        val pick_format by stringType
        val side_margin by doubleType
        val cards_per_row by intType

        val page_size by enumType(mapOf(
                "A0" to PDRectangle.A0,
                "A1" to PDRectangle.A1,
                "A2" to PDRectangle.A2,
                "A3" to PDRectangle.A3,
                "A4" to PDRectangle.A4,
                "A5" to PDRectangle.A5,
                "A6" to PDRectangle.A6,
                "LEGAL" to PDRectangle.LEGAL,
                "LETTER" to PDRectangle.LETTER))

        val font_size by doubleType
        val line_height by doubleType
        val padding_ratio by doubleType

    }

    private val log by logger()

    fun run(args: Array<String>) {
        ArgParser(args).parseInto(::Arguments).run {
            log.info { "Questions: $questionsPath => $questionsOutput" }
            log.info { "Answers: $answersPath => $answersOutput" }

            val config = loadProperties(configFile)
            log.info {
                config.list().joinToString(prefix = "Configuration:\n")
            }

            val generator = Generator(
                    pickStringFormat = config[Config.pick_format],
                    fontSize = config[Config.font_size].toFloat(),
                    cardsPerRow = config[Config.cards_per_row],
                    paddingRatio = config[Config.padding_ratio].toFloat(),
                    pageSize = config[Config.page_size],
                    sideMargin = config[Config.side_margin].toFloat(),
                    textLineHeight = config[Config.line_height].toFloat()
            )

            val questions = Parser.loadQuestions(questionsPath)
            if (questions == null) {
                log.log(Level.INFO, "Skipping the generation of questions")
            } else {
                generator.generateQuestions(questions, questionsOutput)
            }

            val answers = Parser.loadAnswers(answersPath)
            if (answers == null) {
                log.log(Level.INFO, "Skipping the generation of answers")
            } else {
                generator.generateAnswers(answers, answersOutput)
            }
        }
    }

    fun loadProperties(filePath: String): Configuration = ConfigurationProperties.fromOptionalFile(File(filePath)) overriding
                                                          ConfigurationProperties.fromResource("default.conf")

}

fun main(args: Array<String>) {
    Main.run(args)
}