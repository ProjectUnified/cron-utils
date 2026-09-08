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
import com.cronutils.parser.CronParser;
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
     * Maps {@code expression} (parsed and validated as {@code sourceType}) to
     * an equivalent expression of {@code targetType}, for carrying the
     * schedule over when the user switches cron types. Returns null when the
     * dialects do not map (no direct {@link CronMapper} pair, a failed
     * round-trip) or when the expression is invalid for the source type.
     *
     * @param sourceType dialect {@code expression} is written in; never null
     * @param expression raw expression; may be null or empty (yields null)
     * @param targetType dialect to map to; never null
     * @return mapped expression, or null when no mapping exists; never empty
     */
    public static String carryOver(CronType sourceType, String expression, CronType targetType) {
        if (sourceType == null || targetType == null || expression == null
                || expression.trim().isEmpty()) {
            return null;
        }
        try {
            CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(sourceType);
            Cron cron = new CronParser(definition).parse(expression.trim());
            cron.validate();
            CronMapper mapper = mapperFor(sourceType, targetType);
            if (mapper == null) {
                return null;
            }
            return mapper.map(cron).asString();
        } catch (RuntimeException e) {
            return null;
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
     * Initial builder value for one field: {@code "?"} for day-of-week where
     * supported, {@code "*"} otherwise. Day-of-month always defaults to
     * {@code "*"} so the two day fields never open as {@code "?"} plus
     * {@code "?"} — a valid schedule that matches no day and yields no
     * upcoming runs. Never null.
     *
     * @param definition field definition; never null
     * @return default raw input; never null
     */
    public static String defaultValue(FieldDefinition definition) {
        if (definition.getFieldName() == CronFieldName.DAY_OF_WEEK
                && definition.getConstraints().getSpecialChars().contains(SpecialChar.QUESTION_MARK)) {
            return "?";
        }
        return "*";
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

    /**
     * Clickable starter values for one field: up to four hand-picked,
     * intuitive candidates that actually validate against the field's own
     * constraints (unsupported specials silently drop out). Never empty and
     * never null; every entry parses and validates.
     *
     * @param definition field definition; never null
     * @return 1-4 ready-to-use values; never null
     */
    public static List<String> quickValues(FieldDefinition definition) {
        List<String> candidates = new ArrayList<>();
        switch (definition.getFieldName()) {
            case SECOND:
            case MINUTE:
                Collections.addAll(candidates, "*", "0", "*/15", "5,10");
                break;
            case HOUR:
                Collections.addAll(candidates, "*", "12", "9-17", "*/2");
                break;
            case DAY_OF_MONTH:
                Collections.addAll(candidates, "*", "1", "1-5", "?", "L", "15W");
                break;
            case MONTH:
                Collections.addAll(candidates, "*", "1", "JAN", "1-6");
                break;
            case DAY_OF_WEEK:
                Collections.addAll(candidates, "*", "MON", "MON-FRI", "?", "MON#2");
                break;
            case YEAR:
                Collections.addAll(candidates, "*", "2026", "2026-2030", "*/5");
                break;
            case DAY_OF_YEAR:
                Collections.addAll(candidates, "*", "1", "1-100", "*/30");
                break;
            default:
                Collections.addAll(candidates, "*");
                break;
        }
        List<String> result = new ArrayList<>();
        for (String candidate : candidates) {
            if (result.size() >= 4) {
                break;
            }
            if (!result.contains(candidate) && validValue(definition, candidate)) {
                result.add(candidate);
            }
        }
        if (result.isEmpty()) {
            result.add("*");
        }
        return result;
    }

    /**
     * One teachable syntax row: the shape ({@code pattern}), a concrete
     * value for this exact field ({@code example}) and what it does
     * ({@code explains}). An empty {@code example} means "leave the field
     * empty" (optional fields only).
     */
    public static final class SyntaxItem {
        /** Shape, e.g. {@code "a-b"} or {@code "?"}. Never null. */
        public final String pattern;
        /** Concrete value for this field, e.g. {@code "1-5"}. Never null. */
        public final String example;
        /** Plain-language effect. Never null. */
        public final String explains;

        public SyntaxItem(String pattern, String example, String explains) {
            this.pattern = pattern;
            this.example = example;
            this.explains = explains;
        }
    }

    /**
     * Teachable operator rows tailored to one field: every/single/range/step
     * /list shapes with range-aware examples, then name aliases and the
     * supported specials ({@code ? L W LW # ~}) with concrete values, then
     * the empty-value row for optional fields. Rows whose example does not
     * validate for this field are omitted. Never null.
     *
     * @param definition field definition; never null
     * @return syntax rows; never null
     */
    public static List<SyntaxItem> syntaxGuide(FieldDefinition definition) {
        List<SyntaxItem> guide = new ArrayList<>();
        guide.add(new SyntaxItem("*", "*", "every value in this field"));
        String single = pickSingle(definition);
        guide.add(new SyntaxItem("n", single, "exactly at " + single));
        String range = pickRange(definition);
        if (range != null) {
            int dash = range.indexOf('-');
            String from = dash < 0 ? range : range.substring(0, dash);
            String to = dash < 0 ? range : range.substring(dash + 1);
            guide.add(new SyntaxItem("a-b", range, "every value from " + from + " through " + to));
        }
        String step = pickStep(definition);
        if (step != null) {
            guide.add(new SyntaxItem(step.startsWith("*") ? "*/n" : "a/n", step,
                    "every step through this field (" + step + ")"));
        }
        String list = pickList(definition);
        if (list != null) {
            guide.add(new SyntaxItem("a,b,...", list, "exactly these values (" + list + ")"));
        }
        TreeSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        names.addAll(definition.getConstraints().getStringMappingKeySet());
        if (!names.isEmpty()) {
            String first = names.first();
            guide.add(new SyntaxItem("NAME", first,
                    "names work too (" + String.join(", ", names) + ")"));
        }
        if (definition.getConstraints().getSpecialChars().contains(SpecialChar.QUESTION_MARK)) {
            guide.add(new SyntaxItem("?", "?",
                    "no specific value: use when the other day field is set"));
        }
        if (definition.getConstraints().getSpecialChars().contains(SpecialChar.L)) {
            guide.add(new SyntaxItem("L", "L", "the last value in this field"));
        }
        if (definition.getConstraints().getSpecialChars().contains(SpecialChar.W)) {
            String w = firstValid(definition, "15W", "1W");
            if (w != null) {
                guide.add(new SyntaxItem("nW", w, "the weekday nearest day " + w.substring(0, w.length() - 1)));
            }
        }
        if (definition.getConstraints().getSpecialChars().contains(SpecialChar.LW)) {
            guide.add(new SyntaxItem("LW", "LW", "the last weekday of the month"));
        }
        if (definition.getConstraints().getSpecialChars().contains(SpecialChar.HASH)) {
            String hash = firstValid(definition, "MON#2", "2#1");
            if (hash != null) {
                guide.add(new SyntaxItem("n#m", hash, "the nth weekday, e.g. " + hash));
            }
        }
        if (definition.getConstraints().getSpecialChars().contains(SpecialChar.TILDE)) {
            if (validValue(definition, "~")) {
                guide.add(new SyntaxItem("~", "~", "a random value in this field"));
            }
        }
        if (definition.isOptional()) {
            guide.add(new SyntaxItem("(empty)", "", "leave empty to omit this optional field"));
        }
        return guide;
    }

    private static String pickSingle(FieldDefinition definition) {
        int start = toInt(definition.getConstraints().getStartRange());
        int end = toInt(definition.getConstraints().getEndRange());
        int preferred;
        switch (definition.getFieldName()) {
            case SECOND:
            case MINUTE:
                preferred = 5;
                break;
            case HOUR:
                preferred = 12;
                break;
            case DAY_OF_MONTH:
                preferred = 15;
                break;
            case MONTH:
                preferred = 6;
                break;
            case DAY_OF_WEEK:
                preferred = 3;
                break;
            case YEAR:
                preferred = 2026;
                break;
            case DAY_OF_YEAR:
                preferred = 100;
                break;
            default:
                preferred = start;
                break;
        }
        int value = (preferred >= start && preferred <= end) ? preferred : start;
        String text = String.valueOf(value);
        return validValue(definition, text) ? text : "*";
    }

    private static String pickRange(FieldDefinition definition) {
        String preferred;
        switch (definition.getFieldName()) {
            case SECOND:
            case MINUTE:
                preferred = "0-10";
                break;
            case HOUR:
                preferred = "9-17";
                break;
            case DAY_OF_MONTH:
                preferred = "1-5";
                break;
            case MONTH:
                preferred = "1-6";
                break;
            case DAY_OF_WEEK:
                preferred = "MON-FRI";
                break;
            case YEAR:
                preferred = "2026-2030";
                break;
            case DAY_OF_YEAR:
                preferred = "1-100";
                break;
            default:
                preferred = null;
                break;
        }
        String hit = preferred == null ? null : firstValid(definition, preferred);
        if (hit != null) {
            return hit;
        }
        int start = toInt(definition.getConstraints().getStartRange());
        int end = toInt(definition.getConstraints().getEndRange());
        if (end > start) {
            String generic = start + "-" + Math.min(start + 4, end);
            if (validValue(definition, generic)) {
                return generic;
            }
        }
        return null;
    }

    private static String pickStep(FieldDefinition definition) {
        String preferred;
        switch (definition.getFieldName()) {
            case SECOND:
            case MINUTE:
                preferred = "*/15";
                break;
            case HOUR:
                preferred = "*/2";
                break;
            case DAY_OF_MONTH:
                preferred = "*/5";
                break;
            case MONTH:
                preferred = "*/2";
                break;
            case DAY_OF_WEEK:
                preferred = "*/2";
                break;
            case YEAR:
                preferred = "*/5";
                break;
            case DAY_OF_YEAR:
                preferred = "*/30";
                break;
            default:
                preferred = null;
                break;
        }
        return preferred == null ? null : firstValid(definition, preferred);
    }

    private static String pickList(FieldDefinition definition) {
        String preferred;
        switch (definition.getFieldName()) {
            case SECOND:
            case MINUTE:
                preferred = "5,10";
                break;
            case HOUR:
                preferred = "9,12";
                break;
            case DAY_OF_MONTH:
                preferred = "1,15";
                break;
            case MONTH:
                preferred = "1,6";
                break;
            case DAY_OF_WEEK:
                preferred = "MON,WED";
                break;
            case YEAR:
                preferred = "2026,2027";
                break;
            case DAY_OF_YEAR:
                preferred = "1,100";
                break;
            default:
                preferred = null;
                break;
        }
        String hit = preferred == null ? null : firstValid(definition, preferred);
        if (hit != null) {
            return hit;
        }
        int start = toInt(definition.getConstraints().getStartRange());
        int end = toInt(definition.getConstraints().getEndRange());
        if (end > start) {
            String generic = start + "," + Math.min(start + 1, end);
            if (validValue(definition, generic)) {
                return generic;
            }
        }
        return null;
    }

    private static String firstValid(FieldDefinition definition, String... texts) {
        for (String text : texts) {
            if (validValue(definition, text)) {
                return text;
            }
        }
        return null;
    }

    private static boolean validValue(FieldDefinition definition, String text) {
        try {
            FieldExpression expression = new CronParserField(definition.getFieldName(),
                    definition.getConstraints(), definition.isOptional()).parse(text).getExpression();
            expression.accept(new ValidationFieldExpressionVisitor(definition.getConstraints()));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static int toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(String.valueOf(value));
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
