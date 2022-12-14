package ru.tinkoff.kora.json.common;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Month;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;

import static java.time.temporal.ChronoField.*;

final class DateTimeFormatters {

    private DateTimeFormatters() {}

    static final Month[] MONTHS = Month.values();
    static final DayOfWeek[] DAY_OF_WEEKS = DayOfWeek.values();

    /**
     * uuuu
     */
    public static final DateTimeFormatter ISO_YEAR = new DateTimeFormatterBuilder()
        .appendValue(YEAR, 4, 4, SignStyle.EXCEEDS_PAD)
        .toFormatter()
        .withResolverStyle(ResolverStyle.STRICT)
        .withChronology(IsoChronology.INSTANCE);

    /**
     * uuuu-MM
     */
    public static final DateTimeFormatter ISO_YEAR_MONTH = new DateTimeFormatterBuilder()
        .append(ISO_YEAR)
        .appendLiteral('-')
        .appendValue(MONTH_OF_YEAR, 2)
        .toFormatter()
        .withResolverStyle(ResolverStyle.STRICT)
        .withChronology(IsoChronology.INSTANCE);

    /**
     * MM-dd
     */
    public static final DateTimeFormatter ISO_MONTH_DAY = new DateTimeFormatterBuilder()
        .appendValue(MONTH_OF_YEAR, 2)
        .appendLiteral('-')
        .appendValue(DAY_OF_MONTH, 2)
        .toFormatter()
        .withResolverStyle(ResolverStyle.STRICT)
        .withChronology(IsoChronology.INSTANCE);

    /**
     * @see DateTimeFormatter#ISO_INSTANT
     */
    public static final DateTimeFormatter ISO_INSTANT = DateTimeFormatter.ISO_INSTANT;

    /**
     * uuuu-MM-dd
     */
    public static final DateTimeFormatter ISO_LOCAL_DATE = new DateTimeFormatterBuilder()
        .append(ISO_YEAR_MONTH)
        .appendLiteral('-')
        .appendValue(DAY_OF_MONTH, 2)
        .toFormatter()
        .withResolverStyle(ResolverStyle.STRICT)
        .withChronology(IsoChronology.INSTANCE);

    /**
     * HH:mm:ss[.SSS]
     */
    static final DateTimeFormatter ISO_LOCAL_TIME = new DateTimeFormatterBuilder()
        .appendValue(HOUR_OF_DAY, 2)
        .appendLiteral(':')
        .appendValue(MINUTE_OF_HOUR, 2)
        .optionalStart()
        .appendLiteral(':')
        .appendValue(SECOND_OF_MINUTE, 2)
        .optionalStart()
        .appendFraction(NANO_OF_SECOND, 3, 3, true)
        .optionalEnd()
        .toFormatter()
        .withResolverStyle(ResolverStyle.STRICT);

    /**
     * uuuu-MM-dd'T'HH:mm:ss[.SSS]
     */
    static final DateTimeFormatter ISO_LOCAL_DATE_TIME = new DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ISO_LOCAL_DATE)
        .appendLiteral('T')
        .append(ISO_LOCAL_TIME)
        .toFormatter()
        .withResolverStyle(ResolverStyle.STRICT)
        .withChronology(IsoChronology.INSTANCE);

    /**
     * HH:mm:ss[.SSS]XXX
     */
    static final DateTimeFormatter ISO_OFFSET_TIME = new DateTimeFormatterBuilder()
        .append(ISO_LOCAL_TIME)
        .appendOffsetId()
        .toFormatter()
        .withResolverStyle(ResolverStyle.STRICT);

    /**
     * uuuu-MM-dd'T'HH:mm:ss[.SSS]XXX
     */
    static final DateTimeFormatter ISO_OFFSET_DATE_TIME = new DateTimeFormatterBuilder()
        .append(ISO_LOCAL_DATE_TIME)
        .appendOffsetId()
        .toFormatter()
        .withResolverStyle(ResolverStyle.STRICT)
        .withChronology(IsoChronology.INSTANCE);

    /**
     * uuuu-MM-dd'T'HH:mm:ss[.SSS]XXX['['VV']']
     */
    static final DateTimeFormatter ISO_ZONED_DATE_TIME = new DateTimeFormatterBuilder()
        .append(ISO_OFFSET_DATE_TIME)
        .optionalStart()
        .appendLiteral('[')
        .parseCaseSensitive()
        .appendZoneRegionId()
        .appendLiteral(']')
        .optionalEnd()
        .toFormatter()
        .withResolverStyle(ResolverStyle.STRICT)
        .withChronology(IsoChronology.INSTANCE);

    /**
     * ISO8601 for {@link java.util.Date} and {@link java.sql.Timestamp} and {@link java.sql.Date}
     */
    private static final String ISO_DATE_FORMATTER = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    /**
     * ISO8601 for {@link java.sql.Time}
     */
    private static final String ISO_TIME_FORMATTER = "HH:mm:ss.SSSXXX";

    static final SimpleDateFormat ISO_DATE_OLD = new SimpleDateFormat(ISO_DATE_FORMATTER);

    static final SimpleDateFormat ISO_TIME_OLD = new SimpleDateFormat(ISO_TIME_FORMATTER);
}
