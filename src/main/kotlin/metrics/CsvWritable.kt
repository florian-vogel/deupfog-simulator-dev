package metrics

import java.io.File

data class ColumnValue(
    val columnName: String, val valueAsString: String
)

interface CsvWritable {
    fun toCsv(): List<ColumnValue>
}

fun <T : CsvWritable> writeCsv(data: List<T>, filePath: String) {
    if (data.isEmpty()) {
        return
    }

    val file = File(filePath)
    file.parentFile.mkdirs()

    val obj1 = data[0]
    val header = obj1.toCsv().joinToString(",") { it.columnName }

    val out = file.outputStream().bufferedWriter()
    out.write(header)
    out.write("\n")

    data.forEach { obj ->
        val objAsCsv = obj.toCsv()
        val accumulatedString = objAsCsv.joinToString(",") { it.valueAsString }
        out.write(accumulatedString)
        out.write("\n")
    }

    out.close()
}
