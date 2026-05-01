package yamlparser.yaml

import org.slf4j.LoggerFactory

sealed interface YamlNode {
    data class Word(
        val value: kotlin.String,
    ) : YamlNode

    data class String(
        val value: kotlin.String,
    ) : YamlNode

    data class Block(
        val entries: Map<kotlin.String, YamlNode>,
    ) : YamlNode

    data class List(
        val items: kotlin.collections.List<YamlNode>,
    ) : YamlNode
}

private val logger = LoggerFactory.getLogger("yamlparser.yaml.YamlTree")

fun YamlNode.traceTree(indent: String = "") {
    when (this) {
        is YamlNode.Word ->
            logger.trace("$indent@Word \"$value\"")
        is YamlNode.String ->
            logger.trace("$indent@String \"$value\"")
        is YamlNode.Block -> {
            logger.trace("$indent@Block")
            for ((key, child) in entries) {
                logger.trace("$indent@$key:")
                child.traceTree("$indent    ")
            }
        }
        is YamlNode.List -> {
            logger.trace("$indent@List")
            items.forEachIndexed { index, child ->
                logger.trace("$indent@[$index]")
                child.traceTree("$indent    ")
            }
        }
    }
}
