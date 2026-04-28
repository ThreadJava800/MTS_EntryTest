import yamlparser.parseGitlabCi
import yamlparser.yaml.YamlParserResult

fun usage() {
    println("Usage: ./gradlew run --args=\".gitlab-ci.yml\"")
}

fun main(args: Array<String>) {
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace")

    if (args.size != 1) {
        usage()
        return
    }

    val result = parseGitlabCi(args[0])
    when (result) {
        is YamlParserResult.Success ->
            println("Successfully parsed GitLab YAML: ${result.value}")
        is YamlParserResult.Failure -> {
            println("Failed to parse GitLab YAML with error: ${result.errorMessage}")
            return
        }
    }
}
