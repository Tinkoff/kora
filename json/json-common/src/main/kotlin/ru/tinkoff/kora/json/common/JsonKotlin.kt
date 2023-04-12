package ru.tinkoff.kora.json.common

object JsonKotlin {
    fun <T> writerForNullable(writer: JsonWriter<T>): JsonWriter<T?> {
        return JsonWriter<T?> { generator, o -> writer.write(generator, o) }
    }

    fun <T> readerForNullable(reader: JsonReader<T>): JsonReader<T?> {
        return JsonReader<T?> { parser -> reader.read(parser) }
    }
}
