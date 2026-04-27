package yamlparser

import java.io.File
import java.util.LinkedList

import org.slf4j.LoggerFactory

import yamlparser.model.GitlabCiConfig
import yamlparser.schema.GitlabCiMapper
import yamlparser.yaml.YamlParserResult
import yamlparser.yaml.YamlLexer
import yamlparser.yaml.YamlParser
import yamlparser.yaml.YamlToken
import yamlparser.yaml.traceTree

private val logger = LoggerFactory.getLogger("yamlparser.GitlabCi")

fun parseGitlabCi(filePath: String): YamlParserResult<GitlabCiConfig> {
    val file = File(filePath)
    if (!file.exists()) {
        return YamlParserResult.Failure("File not found ${file.name}")
    }

    val fileAsText = file.readText()

    logger.trace("Start parsing YAML tokens")
    val tokens = when (val res = YamlLexer(fileAsText).getTokens()) {
        is YamlParserResult.Success -> res.value
        is YamlParserResult.Failure -> return res
    }

    if (logger.isTraceEnabled) {
        logger.trace("YAML tokens:")
        val tokenIterator = tokens.iterator()
        while (tokenIterator.hasNext()) {
            logger.trace("\t{}", tokenIterator.next())
        }
    }

    logger.trace("Start building YAML AST")
    val rootNode = when (val res = YamlParser(tokens).parse()) {
        is YamlParserResult.Success -> res.value
        is YamlParserResult.Failure -> return res
    }

    if (logger.isTraceEnabled) {
        logger.trace("YAML AST:")
        rootNode.traceTree("    ")
    }

    return GitlabCiMapper().map(rootNode)
}

