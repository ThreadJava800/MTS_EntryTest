import com.example.greet.greetingFor

fun main(args: Array<String>) {
    val name = args.firstOrNull() ?: "world"
    println(greetingFor(name))
}
