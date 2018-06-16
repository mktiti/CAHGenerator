package hu.mktiti.cah

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import java.awt.Color
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
            val lineHeight = font.fontDescriptor.capHeight / 1000f * fontSize * textLineHeight

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

                            card.lines().flatMap {
                                wrapText(it, font, fontSize)
                            }.forEach { line ->
                                showText(line)
                                newLineAtOffset(0f, -2 * lineHeight)
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

    private fun wrapText(text: String, font: PDFont, fontSize: Float): List<String> {
        val lines = mutableListOf<String>()
        val words = text.split("\\s+".toRegex())

        for (word in words) {
            if (lines.isEmpty()) {
                lines.add(word)
            } else {
                val combined = "${lines.last()} $word"
                if (font.getStringWidth(combined) / 1000 * fontSize <= cardSize - 2 * cardPadding) {
                    lines[lines.size - 1] = combined
                } else {
                    lines.add(word)
                }
            }
        }

        return lines
    }

}