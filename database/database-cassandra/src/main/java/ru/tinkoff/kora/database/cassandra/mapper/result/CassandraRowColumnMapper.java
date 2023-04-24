package ru.tinkoff.kora.database.cassandra.mapper.result;

import com.datastax.oss.driver.api.core.data.GettableByName;
import ru.tinkoff.kora.common.Mapping;
import ru.tinkoff.kora.database.common.RowMapper;

import javax.annotation.Nullable;

public interface CassandraRowColumnMapper<T> extends Mapping.MappingFunction, RowMapper<T> {
    @Nullable
    T apply(GettableByName row, int index);
}
