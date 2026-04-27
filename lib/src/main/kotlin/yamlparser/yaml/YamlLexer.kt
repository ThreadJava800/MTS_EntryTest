package yamlparser.yaml

import java.util.LinkedList
import org.slf4j.LoggerFactory

import yamlparser.yaml.YamlParserResult

sealed interface YamlToken {
    data class WordToken(val content: CharSequence) : YamlToken
    data class IndentToken(val cnt: Int) : YamlToken

    data object NewLineToken : YamlToken
    data object ColonToken : YamlToken
    data object BulletPointToken : YamlToken
}

private val logger = LoggerFactory.getLogger("yamlparser.yaml.YamlLexer")

class YamlLexer(private val text: String) {
    private var currPos = 0
    private var lineNumber = 1

    private fun isTab(pos: Int): Boolean {
        return text[pos] == '\t'
    }

    private fun extractIndent(): YamlParserResult<YamlToken?> {
        var spaces = 0
        while (currPos + spaces < text.length && text[currPos + spaces] == ' ') {
            spaces++
        }
        if (currPos + spaces < text.length && isTab(currPos + spaces)) {
            return YamlParserResult.Failure("Tabs are not allowed: found tab at line $lineNumber")
        }

        if (spaces > 0) {
            currPos += spaces

            val next = text.getOrNull(currPos)
            if (next == null || next == '\n' || next == '\r') {
                logger.trace("Found end of line after $spaces spaces at line $lineNumber. Omit line")
                return YamlParserResult.Success(null)
            }

            return YamlParserResult.Success(YamlToken.IndentToken(spaces))
        }
        return YamlParserResult.Success(null)
    }

    private fun extractSingleCharToken(): YamlParserResult<YamlToken?> {
        if (isTab(currPos)) {
            return YamlParserResult.Failure("Tabs are not allowed: found tab at line $lineNumber")
        }
        if (text[currPos] == '\r') {
            currPos++
            if (currPos < text.length && text[currPos] == '\n') {
                currPos++
            }
            lineNumber++
            return YamlParserResult.Success(YamlToken.NewLineToken)
        }
        if (text[currPos] == '\n') {
            lineNumber++
            currPos++
            return YamlParserResult.Success(YamlToken.NewLineToken)
        }
        if (text[currPos] == ':') {
            currPos++
            return YamlParserResult.Success(YamlToken.ColonToken)
        }
        if (text[currPos] == '-') {
            currPos++
            return YamlParserResult.Success(YamlToken.BulletPointToken)
        }

        return YamlParserResult.Success(null)
    }

    private fun extractWordToken(): YamlParserResult<YamlToken?> {
        var wordLength = 0
        while (currPos + wordLength < text.length) {
            if (isTab(currPos + wordLength)) {
                return YamlParserResult.Failure("Tabs are not allowed: found tab at line $lineNumber")
            }
            if (!text[currPos + wordLength].isLetterOrDigit() && text[currPos + wordLength] != '_') {
                break
            }
            wordLength++
        }

        if (wordLength > 0) {
            currPos += wordLength
            return YamlParserResult.Success(YamlToken.WordToken(text.subSequence(currPos - wordLength, currPos)))
        }
        return YamlParserResult.Success(null)
    }

    fun getTokens(): YamlParserResult<LinkedList<YamlToken>> {
        val tokens = LinkedList<YamlToken>()

        while (currPos < text.length) {
            val indentToken = when (val res = extractIndent()) {
                is YamlParserResult.Success -> res.value
                is YamlParserResult.Failure -> return res
            }
            if (indentToken != null) {
                tokens.add(indentToken)
                continue
            }

            val singleCharToken = when (val res = extractSingleCharToken()) {
                is YamlParserResult.Success -> res.value
                is YamlParserResult.Failure -> return res
            }
            if (singleCharToken != null) {
                tokens.add(singleCharToken)
                continue
            }

            val wordToken = when (val res = extractWordToken()) {
                is YamlParserResult.Success -> res.value
                is YamlParserResult.Failure -> return res
            }
            if (wordToken != null) {
                tokens.add(wordToken)
                continue
            }

            return YamlParserResult.Failure("Invalid token at line $lineNumber")
        }

        return YamlParserResult.Success(tokens)
    }
}
