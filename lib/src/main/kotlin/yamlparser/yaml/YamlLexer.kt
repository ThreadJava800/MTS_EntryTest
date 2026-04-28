package yamlparser.yaml

import java.util.LinkedList

sealed interface YamlToken {
    data class WordToken(val content: CharSequence) : YamlToken
    data class IndentToken(val cnt: Int) : YamlToken
    data class NewLineToken(val cnt: Int) : YamlToken

    data object ColonToken : YamlToken
    data object BulletPointToken : YamlToken
}

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
            if (currPos >= text.length) {
                return YamlParserResult.Success(null)
            }
            return YamlParserResult.Success(YamlToken.IndentToken(spaces))
        }
        return YamlParserResult.Success(null)
    }

    private fun skipComment(): YamlParserResult<Unit> {
        if (currPos >= text.length) return YamlParserResult.Success(Unit)
        if (isTab(currPos)) {
            return YamlParserResult.Failure("Tabs are not allowed: found tab at line $lineNumber")
        }
        if (text[currPos] != '#') return YamlParserResult.Success(Unit)

        currPos++
        while (currPos < text.length) {
            val ch = text[currPos]
            if (ch == '\n' || ch == '\r') break
            if (isTab(currPos)) {
                return YamlParserResult.Failure("Tabs are not allowed: found tab at line $lineNumber")
            }
            currPos++
        }
        return YamlParserResult.Success(Unit)
    }

    private fun normalizeTokens(tokens: LinkedList<YamlToken>): LinkedList<YamlToken> {
        val input = LinkedList(tokens)
        val out = LinkedList<YamlToken>()

        var pendingNewLines = 0
        while (input.isNotEmpty()) {
            val tok = input.removeFirst()

            if (tok is YamlToken.IndentToken) {
                if (input.firstOrNull() is YamlToken.NewLineToken) continue
            }

            if (tok is YamlToken.NewLineToken) {
                pendingNewLines += tok.cnt
                continue
            }

            if (pendingNewLines > 0) {
                out.add(YamlToken.NewLineToken(pendingNewLines))
                pendingNewLines = 0
            }
            out.add(tok)
        }

        if (pendingNewLines > 0) {
            out.add(YamlToken.NewLineToken(pendingNewLines))
        }

        return out
    }

    private fun extractSingleCharToken(): YamlParserResult<YamlToken?> {
        if (currPos >= text.length) return YamlParserResult.Success(null)
        if (isTab(currPos)) {
            return YamlParserResult.Failure("Tabs are not allowed: found tab at line $lineNumber")
        }
        if (text[currPos] == '\r' || text[currPos] == '\n') {
            if (text[currPos] == '\r') {
                currPos++
                if (currPos < text.length && text[currPos] == '\n') {
                    currPos++
                }
            } else {
                currPos++
            }
            lineNumber++
            return YamlParserResult.Success(YamlToken.NewLineToken(1))
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
            when (val res = skipComment()) {
                is YamlParserResult.Success -> Unit
                is YamlParserResult.Failure -> return res
            }

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

        return YamlParserResult.Success(normalizeTokens(tokens))
    }
}
