package yamlparser.model

import yamlparser.yaml.YamlNode

sealed interface Image {
    data class Name(val value: String) : Image
    data class Object(
        val name: String,
        val attributes: Map<String, YamlNode> = emptyMap(),
    ) : Image
}

data class Job(
    val image: Image,
    val attributes: Map<String, YamlNode> = emptyMap(),
)

data class GitlabCiConfig(
    val stages: List<String>,
    val jobs: Map<String, Job>,
)

