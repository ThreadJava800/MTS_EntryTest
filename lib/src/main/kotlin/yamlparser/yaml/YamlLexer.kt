package yamlparser.yaml

import yamlparser.yaml.YamlParserResult

sealed interface YamlToken {
    data class WordToken(val content: CharSequence) : YamlToken
    data class IndentToken(val cnt: Int) : YamlToken

    data object NewLineToken : YamlToken
    data object ColonToken : YamlToken
    data object BulletPointToken : YamlToken
}

class YamlLexer {
    private fun isTab(text: String, pos: Int): Boolean {
        return text[pos] == '\t'
    }

    // returns error if there are tabs in the text
    // on success, returns the number of leading spaces
    private fun extractIndent(text: String, currPos: Int, lineNumber: Int): YamlParserResult<Pair<YamlToken, Int>> {
        var spaces = 0
        while (currPos + spaces < text.length && text[currPos + spaces] == ' ') {
            spaces++
        }
        if (currPos + spaces < text.length && isTab(text, currPos + spaces)) {
            return YamlParserResult.Failure("Tabs are not allowed: found tab at line $lineNumber")
        }
        return YamlParserResult.Success(YamlToken.IndentToken(spaces) to spaces)
    }

    private fun extractSingleCharToken(text: String, currPos: Int, lineNumber: Int): YamlParserResult<Pair<YamlToken, Int>> {
        if (isTab(text, currPos)) {
            return YamlParserResult.Failure("Tabs are not allowed: found tab at line $lineNumber")
        }
        if (text[currPos] == '\n') {
            return YamlParserResult.Success(YamlToken.NewLineToken to 1)
        }
        if (text[currPos] == ':') {
            return YamlParserResult.Success(YamlToken.ColonToken to 1)
        }
        if (text[currPos] == '-') {
            return YamlParserResult.Success(YamlToken.BulletPointToken to 1)
        }
        return YamlParserResult.Success(YamlToken.ColonToken to 0)
    }

    private fun extractWordToken(text: String, currPos: Int, lineNumber: Int): YamlParserResult<Pair<YamlToken, Int>> {
        var tokenLength = 0
        while (currPos + tokenLength < text.length) {
            if (isTab(text, currPos + tokenLength)) {
                return YamlParserResult.Failure("Tabs are not allowed: found tab at line $lineNumber")
            }
            if (!text[currPos + tokenLength].isLetterOrDigit() && text[currPos + tokenLength] != '_') {
                break
            }
            tokenLength++
        }
        return YamlParserResult.Success(
            YamlToken.WordToken(text.subSequence(currPos, currPos + tokenLength)) to tokenLength
        )
    }

    fun getTokens(text: String): YamlParserResult<List<YamlToken>> {
        val tokens = mutableListOf<YamlToken>()

        var currPos = 0
        var lineNumber = 1

        while (currPos < text.length) {
            val (indentToken, indentTokenLength) = when (val res = extractIndent(text, currPos, lineNumber)) {
                is YamlParserResult.Success -> res.value
                is YamlParserResult.Failure -> return res
            }
            if (indentTokenLength > 0) {
                tokens.add(indentToken)
                currPos += indentTokenLength
                continue
            }
    
            val (singleCharToken, singleCharTokenLength) = when (val res = extractSingleCharToken(text, currPos, lineNumber)) {
                is YamlParserResult.Success -> res.value
                is YamlParserResult.Failure -> return res
            }
            if (singleCharTokenLength > 0) {
                if (singleCharToken is YamlToken.NewLineToken) {
                    lineNumber++
                }
                tokens.add(singleCharToken)
                currPos += singleCharTokenLength
                continue
            }
    
            val (wordToken, wordTokenLength) = when (val res = extractWordToken(text, currPos, lineNumber)) {
                is YamlParserResult.Success -> res.value
                is YamlParserResult.Failure -> return res
            }
            if (wordTokenLength > 0) {
                tokens.add(wordToken)
                currPos += wordTokenLength
                continue
            }

            return YamlParserResult.Failure("Invalid token at line $lineNumber")
        }

        return YamlParserResult.Success(tokens)
    }
}
