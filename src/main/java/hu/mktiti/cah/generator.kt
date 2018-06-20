package hu.mktiti.cah

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import java.awt.Color
import java.util.logging.Level
import javax.imageio.ImageIO

class Generator(
        private val pickStringFormat: String,
        private val sideMargin: Float,
        private val cardsPerRow: Int,
        private val pageSize: PDRectangle,
        private val fontSize: Float,
        private val textLineHeight: Float,
        paddingRatio: Float) {

    private val log by logger()

    private val cardSize: Float = (pageSize.width - 2 * sideMargin) / cardsPerRow
    private val cardPadding: Float = cardSize / paddingRatio

    private val iconHeight: Float = cardSize / 8

    private val cardsPerCol: Int = (pageSize.height / cardSize).toInt()
    private val vertMargin: Float = (pageSize.height - cardSize * cardsPerCol) / 2f

    private fun generate(cards: List<Card>, backgroundColor: Color, color: Color, outputFile: String) {
        log.info { "Started generation of $outputFile" }

        val pages = cards.chunked(cardsPerRow).chunked(cardsPerCol)

        val document = PDDocument()

        val icon = LosslessFactory.createFromImage(document, ImageIO.read(javaClass.getResourceAsStream("/icon.png")))
        val iconWidth: Float = iconHeight / icon.height * icon.width

        for (pageCards in pages) {
            val page = PDPage(pageSize)
            document.addPage(page)

            val font = PDType0Font.load(document, javaClass.getResourceAsStream("/Arial Bold.ttf"))
            val lineHeight = calculateLineHeight(font, fontSize)

            val maxHeight = cardSize - (3f * cardPadding) - iconHeight

            with(PDPageContentStream(document, page)) {
                // Background
                setNonStrokingColor(backgroundColor)
                addRect(0f, 0f, pageSize.width, pageSize.height)
                fill()

                setStrokingColor(color)
                setNonStrokingColor(color)

                // vertical borders
                for (i in 0..cardsPerRow) {
                    moveTo(sideMargin + i * cardSize, 0f)
                    lineTo(sideMargin + i * cardSize, pageSize.height)
                    stroke()
                }

                // horizontal borders
                for (i in 0..cardsPerCol) {
                    moveTo(0f, vertMargin + i * cardSize)
                    lineTo(pageSize.width, vertMargin + i * cardSize)
                    stroke()
                }

                // Card content
                pageCards.forEachIndexed { row, rowCards ->
                    rowCards.forEachIndexed { col, card ->
                        val (x, y) = cardPosition(row, col)
                        drawImage(icon, x + cardPadding, y + cardPadding, iconWidth, iconHeight)

                        if (card.numberOfBlanks > 1) {
                            val pickString = pickStringFormat.format(card.numberOfBlanks)
                            val pickStringWidth = font.getStringWidth(pickString) / 1000f * fontSize

                            beginText()
                                setFont(font, fontSize)
                                newLineAtOffset(x + cardSize - cardPadding - pickStringWidth, y + cardPadding)
                                showText(pickString)
                            endText()
                        }

                        beginText()
                            setFont(font, fontSize)
                            newLineAtOffset(x + cardPadding, y + cardSize - cardPadding - lineHeight)

                            val sized = sizeText(card.lines(), font, fontSize, maxHeight)
                            if (sized != null) {
                                setFont(font, sized.second)
                                sized.first.forEach {
                                    showText(it)
                                    newLineAtOffset(0f, -2f * calculateLineHeight(font, sized.second))
                                }
                            } else {
                                log.log(Level.SEVERE) { "Cannot fit text '${card.lines()}' to card!" }
                            }
                        endText()
                    }
                }

                close()
            }
        }

        document.save(outputFile)
        document.close()

        log.info { "Generation of $outputFile done!" }
    }

    fun generateQuestions(questions: List<QuestionCard>, outputFile: String) = generate(questions, Color.BLACK, Color.WHITE, outputFile)

    fun generateAnswers(answers: List<AnswerCard>, outputFile: String) = generate(answers, Color.WHITE, Color.BLACK, outputFile)

    private fun cardPosition(row: Int, col: Int): Pair<Float, Float> = (sideMargin + col * cardSize) to (pageSize.height - vertMargin - (row + 1) * cardSize)

    private fun sizeText(lines: List<String>, font: PDFont, maxFontSize: Float, maxHeight: Float): Pair<List<String>, Float>? {
        for (i in 0 until (2 * maxFontSize).toInt()) {
            val fontSize = maxFontSize - (i / 2f)
            val fontHeight = 2f * calculateLineHeight(font, fontSize)
            val allLines = lines.map {
                wrapText(it, font, fontSize)
            }

            if (!allLines.contains(null)) {
                val flattened = allLines.requireNoNulls().flatMap { it }
                if (flattened.size * fontHeight <= maxHeight) {
                    return flattened to fontSize
                }
            }
        }

        return null
    }

    private fun wrapText(text: String, font: PDFont, fontSize: Float): List<String>? {
        val lines = mutableListOf<String>()
        val words = text.split("\\s+".toRegex())

        for (word in words) {
            if (lines.isEmpty()) {
                if (canFitLine(font, fontSize, word)) {
                    lines.add(word)
                } else {
                    return null
                }
            } else {
                val combined = "${lines.last()} $word"
                when {
                    canFitLine(font, fontSize, combined) -> lines[lines.size - 1] = combined
                    canFitLine(font, fontSize, word) -> lines.add(word)
                    else -> return null
                }
            }
        }

        return lines
    }

    private fun canFitLine(font: PDFont, fontSize: Float, line: String)
        = font.getStringWidth(line) / 1000f * fontSize <= cardSize - 2 * cardPadding

    private fun calculateLineHeight(font: PDFont, fontSize: Float): Float
        = font.fontDescriptor.capHeight / 1000f * fontSize * textLineHeight

}