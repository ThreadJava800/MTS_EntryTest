package yamlparser.model

sealed interface Image {
    data class Name(val value: String) : Image
    data class Object(val name: String) : Image
}

data class Job(
    val image: Image,
)

data class GitlabCiConfig(
    val stages: List<String>,
    val jobs: Map<String, Job>,
)

