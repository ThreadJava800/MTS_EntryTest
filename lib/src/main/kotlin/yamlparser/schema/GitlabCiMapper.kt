package yamlparser.schema

import yamlparser.model.Image
import yamlparser.model.Job
import yamlparser.model.GitlabCiConfig
import yamlparser.yaml.YamlParserResult
import yamlparser.yaml.YamlNode

class GitlabCiMapper(private val root: YamlNode) {
    fun map(): YamlParserResult<GitlabCiConfig> {
        val rootBlock = root as? YamlNode.Block
            ?: return YamlParserResult.Failure("Root YAML node must be a mapping (block)")

        val stages = when (val res = extractStages(rootBlock)) {
            is YamlParserResult.Success -> res.value
            is YamlParserResult.Failure -> return res
        }

        val jobs = when (val res = extractJobs(rootBlock)) {
            is YamlParserResult.Success -> res.value
            is YamlParserResult.Failure -> return res
        }

        return YamlParserResult.Success(GitlabCiConfig(stages, jobs))
    }

    private fun extractStages(rootBlock: YamlNode.Block): YamlParserResult<List<String>> {
        val stagesNode: YamlNode.List = rootBlock.entries["stages"] as? YamlNode.List
            ?: return YamlParserResult.Failure("Missing required `stages` list")

        val stages = mutableListOf<String>()
        for ((idx, item) in stagesNode.items.withIndex()) {
            val s = when (item) {
                is YamlNode.Word -> item.value
                is YamlNode.String -> item.value
                else ->
                    return YamlParserResult.Failure("`stages[$idx]`: list item must be a string")
            }
            stages.add(s)
        }

        return YamlParserResult.Success(stages)
    }

    private fun extractJobs(rootBlock: YamlNode.Block): YamlParserResult<Map<String, Job>> {
        val jobs = linkedMapOf<String, Job>()
        for ((jobName, node) in rootBlock.entries) {
            if (jobName == "stages") continue

            val job = when (val res = extractJob(jobName, node)) {
                is YamlParserResult.Success -> res.value
                is YamlParserResult.Failure -> return res
            }
            jobs[jobName] = job
        }
        return YamlParserResult.Success(jobs)
    }

    private fun extractJob(jobName: String, node: YamlNode): YamlParserResult<Job> {
        val jobBlock = node as? YamlNode.Block
            ?: return YamlParserResult.Failure("Job `$jobName` must be a block")

        val imageNode = jobBlock.entries["image"]
            ?: return YamlParserResult.Failure("Job `$jobName` is missing required `image` attribute")

        return when (val res = extractImage(jobName, imageNode)) {
            is YamlParserResult.Success -> YamlParserResult.Success(Job(res.value))
            is YamlParserResult.Failure -> res
        }
    }

    private fun extractImage(jobName: String, imageNode: YamlNode): YamlParserResult<Image> {
        return when (imageNode) {
            is YamlNode.Word -> YamlParserResult.Success(Image.Name(imageNode.value))
            is YamlNode.String -> YamlParserResult.Success(Image.Name(imageNode.value))
            is YamlNode.Block -> {
                val nameNode = imageNode.entries["name"]
                    ?: return YamlParserResult.Failure("Job `$jobName`.image object is missing required `name`")
                val name = when (nameNode) {
                    is YamlNode.Word -> nameNode.value
                    is YamlNode.String -> nameNode.value
                    else -> return YamlParserResult.Failure("Job `$jobName`.image.name must be a string or word")
                }
                YamlParserResult.Success(Image.Object(name))
            }
            else -> YamlParserResult.Failure("Job `$jobName`.image must be a string or an object with `name`")
        }
    }
}

