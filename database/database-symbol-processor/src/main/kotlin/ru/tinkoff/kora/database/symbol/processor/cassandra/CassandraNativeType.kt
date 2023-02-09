package ru.tinkoff.kora.database.symbol.processor.cassandra

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName

interface CassandraNativeType {
    fun type(): TypeName
    fun extract(rs: String, index: CodeBlock): CodeBlock
    fun bind(stmt: String, value: CodeBlock, idx: Int): CodeBlock


    companion object {
        fun of(
            type: TypeName,
            extract: (String, CodeBlock) -> CodeBlock,
            bind: (String, CodeBlock, Int) -> CodeBlock
        ): CassandraNativeType {
            class Impl : CassandraNativeType {
                override fun type() = type

                override fun extract(rs: String, index: CodeBlock) = extract(rs, index)

                override fun bind(stmt: String, value: CodeBlock, idx: Int) = bind(stmt, value, idx)

            }
            return Impl()
        }
    }
}


