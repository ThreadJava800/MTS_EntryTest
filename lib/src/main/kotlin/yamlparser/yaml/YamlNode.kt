package yamlparser.yaml

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

