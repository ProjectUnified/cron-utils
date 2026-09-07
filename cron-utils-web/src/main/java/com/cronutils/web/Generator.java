package com.cronutils.web;

import com.cronutils.builder.CronBuilder;
import com.cronutils.mapper.CronMapper;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.field.CronFieldName;
import com.cronutils.model.field.constraint.FieldConstraints;
import com.cronutils.model.field.definition.FieldDefinition;
import com.cronutils.model.field.expression.Always;
import com.cronutils.model.field.expression.And;
import com.cronutils.model.field.expression.Between;
import com.cronutils.model.field.expression.Every;
import com.cronutils.model.field.expression.FieldExpression;
import com.cronutils.model.field.expression.On;
import com.cronutils.model.field.expression.visitor.ValidationFieldExpressionVisitor;
import com.cronutils.model.field.expression.QuestionMark;
import com.cronutils.model.field.expression.RandomExpression;
import com.cronutils.model.field.value.SpecialChar;
import com.cronutils.parser.CronParserField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

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
     * map to {@code "not available"}.
     *
     * @param sourceType dialect {@code cron} was parsed with; never null
     * @param cron       valid parsed cron; never null
     * @return type name to expression or a {@code "not available"} marker, in {@code CronType} order
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
                return "not available";
            }
            return mapper.map(cron).asString();
        } catch (RuntimeException e) {
            return "not available";
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

    /**
     * Builds an expression of {@code type} from a type-agnostic schedule.
     * Keys are {@link CronFieldName} names; only fields the type defines are
     * read. Fields the schedule omits fall back to {@code ?} (day fields where
     * supported), {@code *}, or omission for optional fields. A {@code ?} the
     * type does not support degrades to {@code *}.
     *
     * @param type cron dialect; never null
     * @param universal field name to value; never null
     * @return normalized expression string
     * @throws IllegalArgumentException when the schedule has no valid form
     * in {@code type} (e.g. dialect-specific specials like {@code L})
     */
    public static String buildExample(CronType type, Map<String, String> universal) {
        return generate(type, exampleInputs(type, universal));
    }

    /**
     * Per-field inputs of {@code type} for a type-agnostic schedule.
     *
     * @param type cron dialect; never null
     * @param universal field name to value; never null
     * @return field name to raw input, in canonical field order; never null
     */
    public static Map<String, String> exampleInputs(CronType type, Map<String, String> universal) {
        CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(type);
        Map<String, String> inputs = new LinkedHashMap<>();
        for (FieldDefinition fieldDefinition : orderedFields(definition)) {
            String name = fieldDefinition.getFieldName().name();
            String value = universal.get(name);
            if (value == null) {
                if (fieldDefinition.isOptional()) {
                    value = "";
                } else if (isQuestionMarkField(fieldDefinition)) {
                    value = "?";
                } else {
                    value = "*";
                }
            } else if ("?".equals(value.trim())
                    && !fieldDefinition.getConstraints().getSpecialChars().contains(SpecialChar.QUESTION_MARK)) {
                value = "*";
            }
            inputs.put(name, value);
        }
        return inputs;
    }

    private static boolean isQuestionMarkField(FieldDefinition fieldDefinition) {
        CronFieldName name = fieldDefinition.getFieldName();
        return (name == CronFieldName.DAY_OF_MONTH || name == CronFieldName.DAY_OF_WEEK)
                && fieldDefinition.getConstraints().getSpecialChars().contains(SpecialChar.QUESTION_MARK);
    }

    /**
     * Short allowed-values hint for one field, e.g.
     * {@code "0-59 · * , - / · L W"}. Range first, then the always-available
     * {@code * , - /} operators, then any extra specials ({@code ? # L W LW ~}),
     * then any name aliases ({@code JAN-DEC}), then {@code optional} if skippable.
     *
     * @param definition field definition; never null
     * @return hint text; never null
     */
    public static String hint(FieldDefinition definition) {
        FieldConstraints constraints = definition.getConstraints();
        StringBuilder out = new StringBuilder();
        out.append(constraints.getStartRange()).append('-').append(constraints.getEndRange());
        out.append(" · * , - /");
        List<String> specials = new ArrayList<>();
        for (SpecialChar special : new TreeSet<>(constraints.getSpecialChars())) {
            String symbol = specialSymbol(special);
            if (symbol != null) {
                specials.add(symbol);
            }
        }
        if (!specials.isEmpty()) {
            out.append(" · ").append(String.join(" ", specials));
        }
        TreeSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        names.addAll(constraints.getStringMappingKeySet());
        if (!names.isEmpty()) {
            out.append(" · ").append(String.join(" ", names));
        }
        if (definition.isOptional()) {
            out.append(" · optional");
        }
        return out.toString();
    }

    /**
     * One-line meaning of the current raw input for one field, e.g.
     * {@code "every value"}, {@code "at 12"}, {@code "range 1-5"}.
     * Invalid input yields {@code "invalid: <reason>"}; empty yields
     * {@code "omitted"} for optional fields and {@code "required"} otherwise.
     *
     * @param definition field definition; never null
     * @param input raw field text; may be null
     * @return meaning text; never null
     */
    public static String meaning(FieldDefinition definition, String input) {
        if (input == null || input.trim().isEmpty()) {
            return definition.isOptional() ? "omitted" : "required";
        }
        String text = input.trim();
        FieldExpression expression;
        try {
            expression = new CronParserField(definition.getFieldName(), definition.getConstraints(),
                    definition.isOptional()).parse(text).getExpression();
            expression.accept(new ValidationFieldExpressionVisitor(definition.getConstraints()));
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            return "invalid" + (message == null ? "" : ": " + message);
        }
        if (expression instanceof Always) {
            return "every value";
        }
        if (expression instanceof QuestionMark) {
            return "no specific value";
        }
        if (expression instanceof Between) {
            return "range " + expression.asString();
        }
        if (expression instanceof Every) {
            return "step " + expression.asString();
        }
        if (expression instanceof On) {
            return "at " + expression.asString();
        }
        if (expression instanceof And) {
            return "list " + expression.asString();
        }
        if (expression instanceof RandomExpression) {
            return "random " + expression.asString();
        }
        return expression.asString();
    }

    private static String specialSymbol(SpecialChar special) {
        switch (special) {
            case QUESTION_MARK:
                return "?";
            case HASH:
                return "#";
            case L:
                return "L";
            case W:
                return "W";
            case LW:
                return "LW";
            case TILDE:
                return "~";
            default:
                return null;
        }
    }
}
