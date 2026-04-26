package yamlparser

import java.io.File

import yamlparser.model.GitlabCiConfig
import yamlparser.schema.GitlabCiMapper
import yamlparser.yaml.YamlParserResult
import yamlparser.yaml.YamlLexer
import yamlparser.yaml.YamlParser

fun parseGitlabCi(filePath: String): YamlParserResult<GitlabCiConfig> {
    val file = File(filePath)
    if (!file.exists()) {
        return YamlParserResult.Failure("File not found ${file.name}")
    }

    val fileAsText = file.readText()

    val tokens = when (val res = YamlLexer().getTokens(fileAsText)) {
        is YamlParserResult.Success -> res.value
        is YamlParserResult.Failure -> return res
    }
    tokens.forEach { println(it) }

    val rootNode = when (val res = YamlParser().parse(tokens)) {
        is YamlParserResult.Success -> res.value
        is YamlParserResult.Failure -> return res
    }

    return GitlabCiMapper().map(rootNode)
}

