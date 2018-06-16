# CAHGenerator
Cards Against Humanity cards generator written in Kotlin.

### Works thanks to:
+ [Apache PDFBox](https://pdfbox.apache.org/)
+ [Konfig configuration library](https://github.com/npryce/konfig)
+ [Kotlin-argparser](https://github.com/xenomachina/kotlin-argparser)
+ [Kotlin](https://kotlinlang.org/)

### Build (self contained runnable jar file):
`mvn clean install`

### Execution:
`java -jar CahGenerator.jar {arguments}`

### Arguments:
+ `-c`, `--config` - Config file location, if not present defaults will be used, defualt: cah.conf
+ `-q`, `--questions` - Questions file, if not present question generation will be skipped, default: questions.txt
+ `-a`, `--answers` - Answers file, if not present answer generation will be skipped, default: answers.txt
+ `-b`, `--questions-output` - Generated questions pdf (Black cards), default: questionsGenerated.pdf
+ `-w`, `--answers-output` - Generated answers pdf (White cards), default: answersGenerated.pdf

### Config file possible properties:
+ `pick_format` - 'PICK' string format ,%s is replaced by number (`pick_format = PICK %s` => PICK 2), default: PICK %s
+ `side_margin` - Page left/right margin in points, default: 10
+ `cards_per_row` - Number of cards in a row (card size is calculated based on this), default: 4
+ `page_size` - Page size, accepted: A0-A6, LEGAL, LETTER, default: A4
+ `font_size` - Font size, default: 12
+ `line_height` - Line height, default: 1
+ `padding_ratio` - Card padding (all sides) in points, default: 15

### Card definitions:
Simple text file with one line per card. '//' is replaced by newline. In questions '$BLANK' is replaced by underline and number of cards to pick is base on this.

The sample (hungarian) question and answer definitions are copied from https://github.com/emberiseg-ellenes-kartyak/emberiseg-ellenes-kartyak.github.io
