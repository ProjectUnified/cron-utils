package com.cronutils.web;

import com.cronutils.builder.CronBuilder;
import com.cronutils.mapper.CronMapper;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.field.CronFieldName;
import com.cronutils.model.field.definition.FieldDefinition;
import com.cronutils.model.field.expression.FieldExpression;
import com.cronutils.parser.CronParserField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure cron generator and cross-type mapping logic.
 * No DOM access; safe to unit-test on the JVM.
 */
public final class Generator {

    private Generator() {
    }

    /**
     * Builds an expression of {@code type} from per-field user input.
     * Keys are {@link CronFieldName} names; only fields the type defines are read.
     * Optional fields left empty are skipped; required fields left empty fail.
     *
     * @param type        cron dialect; never null
     * @param fieldInputs raw per-field input; never null
     * @return normalized expression string
     * @throws IllegalArgumentException with a field-level message on invalid input
     */
    public static String generate(CronType type, Map<String, String> fieldInputs) {
        CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(type);
        CronBuilder builder = CronBuilder.cron(definition);
        for (FieldDefinition fieldDefinition : orderedFields(definition)) {
            CronFieldName fieldName = fieldDefinition.getFieldName();
            String text = fieldInputs.get(fieldName.name());
            if (text == null || text.trim().isEmpty()) {
                if (fieldDefinition.isOptional()) {
                    continue;
                }
                throw new IllegalArgumentException("Field " + fieldName + " is required.");
            }
            FieldExpression parsed;
            try {
                parsed = new CronParserField(fieldName, fieldDefinition.getConstraints(),
                        fieldDefinition.isOptional()).parse(text.trim()).getExpression();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Field " + fieldName + ": " + e.getMessage(), e);
            }
            switch (fieldName) {
                case SECOND:
                    builder = builder.withSecond(parsed);
                    break;
                case MINUTE:
                    builder = builder.withMinute(parsed);
                    break;
                case HOUR:
                    builder = builder.withHour(parsed);
                    break;
                case DAY_OF_MONTH:
                    builder = builder.withDoM(parsed);
                    break;
                case MONTH:
                    builder = builder.withMonth(parsed);
                    break;
                case DAY_OF_WEEK:
                    builder = builder.withDoW(parsed);
                    break;
                case YEAR:
                    builder = builder.withYear(parsed);
                    break;
                case DAY_OF_YEAR:
                    builder = builder.withDoY(parsed);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported field " + fieldName);
            }
        }
        return builder.instance().asString();
    }

    /**
     * Applies a nickname preset factory ({@code yearly}, {@code monthly},
     * {@code weekly}, {@code daily}, {@code hourly}) for {@code type}.
     *
     * @param type   cron dialect; never null
     * @param preset one of yearly|monthly|weekly|daily|hourly
     * @return normalized expression string
     * @throws IllegalArgumentException on unknown preset names
     */
    public static String preset(CronType type, String preset) {
        CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(type);
        Cron cron;
        if ("yearly".equals(preset)) {
            cron = CronBuilder.yearly(definition);
        } else if ("monthly".equals(preset)) {
            cron = CronBuilder.monthly(definition);
        } else if ("weekly".equals(preset)) {
            cron = CronBuilder.weekly(definition);
        } else if ("daily".equals(preset)) {
            cron = CronBuilder.daily(definition);
        } else if ("hourly".equals(preset)) {
            cron = CronBuilder.hourly(definition);
        } else {
            throw new IllegalArgumentException("Unknown preset " + preset);
        }
        return cron.asString();
    }

    /**
     * Field definitions of {@code definition} in canonical field order.
     * Visibility in the UI derives from this list; never hardcoded per type.
     *
     * @param definition cron definition; never null
     * @return ordered field definitions; never null
     */
    public static List<FieldDefinition> orderedFields(CronDefinition definition) {
        List<FieldDefinition> fields = new ArrayList<>(definition.getFieldDefinitions());
        Collections.sort(fields, FieldDefinition.createFieldDefinitionComparator());
        return fields;
    }

    /**
     * Equivalent expressions of {@code cron} in every {@link CronType}.
     * The source type maps to itself via an identity mapper; types without a
     * direct {@link CronMapper} pair from the source, and failed round-trips,
     * map to {@code "n/a for <TYPE>"}.
     *
     * @param sourceType dialect {@code cron} was parsed with; never null
     * @param cron       valid parsed cron; never null
     * @return type name to expression or {@code n/a} marker, in {@code CronType} order
     */
    public static Map<String, String> equivalents(CronType sourceType, Cron cron) {
        Map<String, String> result = new LinkedHashMap<>();
        for (CronType target : CronType.values()) {
            result.put(target.name(), mapOrNA(sourceType, target, cron));
        }
        return result;
    }

    private static String mapOrNA(CronType sourceType, CronType target, Cron cron) {
        try {
            CronMapper mapper = mapperFor(sourceType, target);
            if (mapper == null) {
                return "n/a for " + target.name();
            }
            return mapper.map(cron).asString();
        } catch (RuntimeException e) {
            return "n/a for " + target.name();
        }
    }

    /**
     * Direct static {@link CronMapper} pair for {@code sourceType} to {@code target},
     * or null where no direct pair exists. Same-type targets map via identity.
     */
    static CronMapper mapperFor(CronType sourceType, CronType target) {
        if (sourceType == target) {
            return CronMapper.sameCron(CronDefinitionBuilder.instanceDefinitionFor(sourceType));
        }
        if (sourceType == CronType.CRON4J && target == CronType.QUARTZ) {
            return CronMapper.fromCron4jToQuartz();
        }
        if (sourceType == CronType.QUARTZ && target == CronType.CRON4J) {
            return CronMapper.fromQuartzToCron4j();
        }
        if (sourceType == CronType.QUARTZ && target == CronType.UNIX) {
            return CronMapper.fromQuartzToUnix();
        }
        if (sourceType == CronType.UNIX && target == CronType.QUARTZ) {
            return CronMapper.fromUnixToQuartz();
        }
        if (sourceType == CronType.QUARTZ && target == CronType.SPRING) {
            return CronMapper.fromQuartzToSpring();
        }
        if (sourceType == CronType.SPRING && target == CronType.QUARTZ) {
            return CronMapper.fromSpringToQuartz();
        }
        return null;
    }

    /**
     * Empty per-field input map for {@code type}: every defined field present
     * with an empty value. Callers fill values before {@link #generate}.
     */
    public static Map<String, String> emptyInputs(CronType type) {
        CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(type);
        Map<String, String> named = new LinkedHashMap<>();
        for (FieldDefinition fieldDefinition : orderedFields(definition)) {
            named.put(fieldDefinition.getFieldName().name(), "");
        }
        return named;
    }
}
