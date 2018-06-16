package hu.mktiti.cah

private const val blankDummy = "\$BLANK"

sealed class Card {
    abstract fun lines(): List<String>
    abstract val numberOfBlanks: Int
}

data class QuestionCard(val data: List<String?>) : Card() {
    val numberOfStrings = data.filterNotNull().size
    override val numberOfBlanks = data.size - numberOfStrings

    override fun toString() = data.joinToString(prefix = "", postfix = "", separator = "") { it ?: "_____" }

    override fun lines(): List<String> = toString().split("//")
}

fun parseQuestion(questionString: String): QuestionCard {
    val stringParts = questionString.split(blankDummy)

    val allParts = mutableListOf<String?>()

    for (part in stringParts) {
        allParts.add(part)
        allParts.add(null)
    }

    if (allParts.isNotEmpty()) {
        allParts.removeAt(allParts.size - 1)
    }

    return QuestionCard(allParts)
}

data class AnswerCard(val answer: String) : Card() {
    override val numberOfBlanks: Int = 0

    override fun lines() = answer.split("//")
}