import test.PaperEvalScenarios
import java.io.File

fun main() {
    val oldStats =
        File("./metrics/csv_data")
    oldStats.deleteRecursively()

    PaperEvalScenarios().runAll()
}


