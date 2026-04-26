package yamlparser.yaml

sealed interface YamlParserResult<out T> {
    data class Success<T>(val value: T) : YamlParserResult<T>
    data class Failure(val errorMessage: String) : YamlParserResult<Nothing>
}

