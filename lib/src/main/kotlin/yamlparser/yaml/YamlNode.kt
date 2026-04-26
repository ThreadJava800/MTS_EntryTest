package yamlparser.yaml

import org.slf4j.LoggerFactory

data class SourcePos(
    val line: Int,
    val column: Int,
)

sealed interface YamlNode {
    val pos: SourcePos

    data class Scalar(
        val value: String,
        override val pos: SourcePos,
    ) : YamlNode

    data class Mapping(
        val entries: Map<String, YamlNode>,
        override val pos: SourcePos,
    ) : YamlNode

    data class Sequence(
        val items: List<YamlNode>,
        override val pos: SourcePos,
    ) : YamlNode
}

private val logger = LoggerFactory.getLogger("yamlparser.yaml.YamlNode")

fun YamlNode.traceTree(indent: String = "") {
    when (this) {
        is YamlNode.Scalar ->
            logger.trace("{}{}", indent, "Scalar \"$value\" @ $pos")
        is YamlNode.Mapping -> {
            logger.trace("{}{}", indent, "Mapping @ $pos")
            for ((key, child) in entries) {
                logger.trace("{}{}", indent, "  $key:")
                child.traceTree("$indent    ")
            }
        }
        is YamlNode.Sequence -> {
            logger.trace("{}{}", indent, "Sequence @ $pos")
            items.forEachIndexed { index, child ->
                logger.trace("{}{}", indent, "  [$index]")
                child.traceTree("$indent    ")
            }
        }
    }
}
