package yamlparser.yaml

import java.util.LinkedList
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("yamlparser.yaml.YamlParser")

class YamlParser(private var tokens: LinkedList<YamlToken>) {
    var line: Int = 1

    private fun peekIndents(): Int {
        var sum = 0
        for (t in tokens) {
            if (t !is YamlToken.IndentToken) break
            sum += t.cnt
        }
        return sum
    }

    private fun readIndents(): Int {
        var indent = 0
        while (tokens.isNotEmpty() && tokens.first() is YamlToken.IndentToken) {
            indent += (tokens.first() as YamlToken.IndentToken).cnt
            tokens.removeFirst()
        }

        logger.trace("Read $indent indents at line $line")
        return indent
    }

    private fun readNewLines(): Int {
        var cnt = 0
        while (tokens.isNotEmpty() && tokens.first() is YamlToken.NewLineToken) {
            cnt++
            tokens.removeFirst()
        }

        line += cnt
        logger.trace("Read $cnt new lines at line $line")
        return cnt
    }

    private fun readWordOrFail(): YamlParserResult<String> {
        if (tokens.isEmpty()) {
            return YamlParserResult.Failure("Unexpected end of input at line $line")
        }

        val tok = tokens.first()
        if (tok !is YamlToken.WordToken) {
            return YamlParserResult.Failure("Expected word, got $tok at line $line")
        }
        tokens.removeFirst()
        logger.trace("Read word \"${tok.content}\" at line $line")
        return YamlParserResult.Success(tok.content.toString())
    }


    private fun readColonOrFail(): YamlParserResult<Unit> {
        if (tokens.isEmpty()) {
            return YamlParserResult.Failure("Unexpected end of input at line $line")
        }

        if (tokens.first() !is YamlToken.ColonToken) {
            return YamlParserResult.Failure("Expected `:`, got ${tokens.first()} at line $line")
        }
        tokens.removeFirst()
        logger.trace("Read colon at line $line")
        return YamlParserResult.Success(Unit)
    }

    private fun parseList(baseIndent: Int): YamlParserResult<YamlNode> {
        logger.trace("Parsing list at line $line with base indent $baseIndent")

        val items = mutableListOf<YamlNode>()
        while (tokens.isNotEmpty()) {            
            val bulletTok = tokens.first()
            if (bulletTok !is YamlToken.BulletPointToken) {
                break
            }

            logger.trace("Read bullet point at line $line")
            tokens.removeFirst()

            readIndents()

            val wordTok = when (val res = readWordOrFail()) {
                is YamlParserResult.Success -> res.value
                is YamlParserResult.Failure -> return res
            }
            items.add(YamlNode.Word(wordTok))

            readNewLines()

            val nextIndentCnt = peekIndents()
            if (nextIndentCnt != baseIndent) {
                break
            }

            readIndents()
        }
        return YamlParserResult.Success(YamlNode.List(items))
    }

    private fun parseNestedBlock(baseIndent: Int): YamlParserResult<YamlNode> {
        logger.trace("Parsing nested block at line $line with base indent $baseIndent")

        val nestedIndent = readIndents()
        if (nestedIndent <= baseIndent) {
            return YamlParserResult.Failure("Expected nested block to be indented deeper than $baseIndent at line $line")
        }

        return when (tokens.first()) {
            is YamlToken.BulletPointToken -> parseList(nestedIndent)
            is YamlToken.WordToken ->
                when (val res = parseBlock()) {
                    is YamlParserResult.Success ->
                        YamlParserResult.Success(
                            YamlNode.Block(linkedMapOf(res.value.first to res.value.second)),
                        )
                    is YamlParserResult.Failure -> res
                }
            else -> YamlParserResult.Failure("Expected `-` or key after indented block start at line $line")
        }
    }

    private fun parseValueAfterColon(baseIndent: Int): YamlParserResult<YamlNode> {
        logger.trace("Parsing value after colon at line $line with base indent $baseIndent")

        readIndents()
        if (tokens.isEmpty()) {
            return YamlParserResult.Failure("Unexpected end of input at line $line")
        }

        return when (val tok = tokens.first()) {
            is YamlToken.WordToken -> {
                when (val res = readWordOrFail()) {
                    is YamlParserResult.Success -> YamlParserResult.Success(YamlNode.Word(res.value))
                    is YamlParserResult.Failure -> return res
                }
            }
            is YamlToken.NewLineToken -> {
                readNewLines()
                parseNestedBlock(baseIndent)
            }
            else -> YamlParserResult.Failure("Expected word or new line, got $tok at line $line")
        }
    }

    private fun parseBlock(): YamlParserResult<Pair<String, YamlNode>> {
        logger.trace("Parsing block at line $line")

        readNewLines()
        val baseIndent = readIndents()
        
        val key = when (val res = readWordOrFail()) {
            is YamlParserResult.Success -> res.value
            is YamlParserResult.Failure -> return res
        }

        when (val res = readColonOrFail()) {
            is YamlParserResult.Success -> Unit
            is YamlParserResult.Failure -> return res
        }

        val value = when (val res = parseValueAfterColon(baseIndent)) {
            is YamlParserResult.Success -> res.value
            is YamlParserResult.Failure -> return res
        }
        return YamlParserResult.Success(key to value)
    }

    fun parse(): YamlParserResult<YamlNode> {
        val nodes = linkedMapOf<String, YamlNode>()
        while (tokens.isNotEmpty()) {
            when (val res = parseBlock()) {
                is YamlParserResult.Success -> nodes[res.value.first] = res.value.second
                is YamlParserResult.Failure -> return res
            }
        }
        return YamlParserResult.Success(YamlNode.Block(nodes))
    }
}
