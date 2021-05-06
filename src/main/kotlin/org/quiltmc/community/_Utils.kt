package org.quiltmc.community

fun String.chunkByWhitespace(length: Int): List<String> {
    if (length <= 0) {
        error("Length must be greater than 0")
    }

    if (contains("\n")) {
        error("String must be a single line")
    }

    val words = split(" ")
    var currentLine = ""
    val lines: MutableList<String> = mutableListOf()

    for (word in words) {
        if (word.length >= length) {
            val parts = word.chunked(length)

            if (currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = ""
            }

            parts.forEach {
                if (it.length == length) {
                    lines.add(it)
                } else if (it.isNotEmpty()) {
                    currentLine = it
                }
            }
        } else {
            val newLength = currentLine.length + word.length + if (currentLine.isEmpty()) 0 else 1

            if (newLength > length) {
                lines.add(currentLine)
                currentLine = word
            } else {
                currentLine += if (currentLine.isEmpty()) word else " $word"
            }
        }
    }

    if (currentLine.isNotEmpty()) {
        lines.add(currentLine)
    }

    return lines
}
