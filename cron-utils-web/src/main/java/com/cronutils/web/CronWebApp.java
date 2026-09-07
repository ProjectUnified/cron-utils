package com.cronutils.web;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.field.CronField;
import com.cronutils.model.field.CronFieldName;
import com.cronutils.model.field.definition.FieldDefinition;
import com.cronutils.model.field.expression.FieldExpression;
import com.cronutils.parser.CronParser;
import com.cronutils.web.Explainer.ExplainResult;
import org.teavm.jso.JSBody;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;
import org.teavm.jso.dom.html.HTMLOptionElement;
import org.teavm.jso.dom.html.HTMLSelectElement;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Single TeaVM entry point. Builds every DOM node from Java; the shipped
 * {@code index.html} is only a shell (loader + stylesheet link).
 *
 * <p>Page shape: {@code header} intro, {@code main} with the generator
 * (per-field builder, expression preview, cross-type equivalents, expandable
 * examples) and the explainer (type plus expression only; time and zone always
 * come from the browser), then {@code footer}.</p>
 */
public class CronWebApp {

    private static final String[] TYPES = {"CRON4J", "QUARTZ", "UNIX", "SPRING", "SPRING53"};
    private static final String[] LOCALES = {"en_GB", "de", "el", "es", "fr", "id", "it", "ja",
            "ko", "nl", "pl", "pt", "ro", "ru", "sw", "tr", "zh"};

    /** Preset nickname buttons shown above the field grid. */
    private static final String[] PRESETS = {"yearly", "monthly", "weekly", "daily", "hourly"};

    /**
     * One expandable-shelf example: a label plus a type-agnostic schedule
     * ({@link CronFieldName} name to value). Loading builds the expression
     * for whatever cron type the generator currently uses.
     */
    static final class Example {
        final String label;
        final Map<String, String> fields;

        Example(String label, String[][] pairs) {
            this.label = label;
            this.fields = new LinkedHashMap<>();
            for (String[] pair : pairs) {
                this.fields.put(pair[0], pair[1]);
            }
        }
    }

    /** Type-agnostic schedules for the expandable example shelf. */
    static final Example[] EXAMPLES = {
            new Example("Noon every day", new String[][]{
                    {"SECOND", "0"}, {"MINUTE", "0"}, {"HOUR", "12"},
                    {"DAY_OF_MONTH", "*"}, {"MONTH", "*"}, {"DAY_OF_WEEK", "?"}}),
            new Example("Weekdays at 10:15", new String[][]{
                    {"SECOND", "0"}, {"MINUTE", "15"}, {"HOUR", "10"},
                    {"DAY_OF_MONTH", "?"}, {"MONTH", "*"}, {"DAY_OF_WEEK", "MON-FRI"}}),
            new Example("Every 15 minutes", new String[][]{
                    {"SECOND", "0"}, {"MINUTE", "0/15"}, {"HOUR", "*"},
                    {"DAY_OF_MONTH", "*"}, {"MONTH", "*"}, {"DAY_OF_WEEK", "?"}}),
            new Example("First of the month at midnight", new String[][]{
                    {"SECOND", "0"}, {"MINUTE", "0"}, {"HOUR", "0"},
                    {"DAY_OF_MONTH", "1"}, {"MONTH", "*"}, {"DAY_OF_WEEK", "?"}}),
            new Example("Mondays at 9 AM", new String[][]{
                    {"SECOND", "0"}, {"MINUTE", "0"}, {"HOUR", "9"},
                    {"DAY_OF_MONTH", "?"}, {"MONTH", "*"}, {"DAY_OF_WEEK", "MON"}}),
            new Example("Top of every hour", new String[][]{
                    {"SECOND", "0"}, {"MINUTE", "0"}, {"HOUR", "*"},
                    {"DAY_OF_MONTH", "*"}, {"MONTH", "*"}, {"DAY_OF_WEEK", "?"}}),
    };

    private static HTMLDocument doc;

    private static HTMLSelectElement explType;
    private static HTMLInputElement explInput;
    private static HTMLSelectElement explLocale;
    private static HTMLElement explMessage;
    private static HTMLElement explReading;
    private static HTMLElement explSchedule;
    private static HTMLElement explNormalized;
    private static HTMLElement explDescription;
    private static HTMLElement explMeta;
    private static HTMLElement explRuns;
    private static HTMLElement explSection;

    private static HTMLSelectElement genType;
    private static HTMLElement genFields;
    private static HTMLElement genMessage;
    private static HTMLElement genTokens;
    private static HTMLElement genEquiv;
    private static final List<HTMLInputElement> genInputs = new ArrayList<>();
    private static final List<FieldDefinition> genDefs = new ArrayList<>();
    private static final List<HTMLElement> genMeanings = new ArrayList<>();
    private static final List<HTMLElement> genExampleCodes = new ArrayList<>();
    private static String lastGoodGenerated = "";

    public static void main(String[] args) {
        doc = HTMLDocument.current();

        HTMLElement header = el("header", null);
        header.setAttribute("class", "site");
        HTMLElement h1 = el("h1", "cron-utils-web");
        header.appendChild(h1);
        header.appendChild(el("p", "Build a cron expression field by field, or paste one to learn what it means. All logic runs in your browser."));
        doc.getBody().appendChild(header);

        HTMLElement main = el("main", null);
        main.setAttribute("class", "site-main");
        doc.getBody().appendChild(main);

        main.appendChild(buildGenerator());
        explSection = buildExplainer();
        main.appendChild(explSection);

        buildFooter();
        safeRefreshExplainer();
        safeRebuildGeneratorFields();
    }

    private static void safeRefreshExplainer() {
        try {
            refreshExplainer();
        } catch (Throwable t) {
            explMessage.setTextContent("Explainer failed: " + t);
            explMessage.setAttribute("class", "error");
        }
    }

    private static void safeRebuildGeneratorFields() {
        try {
            rebuildGeneratorFields();
        } catch (Throwable t) {
            genMessage.setTextContent("Generator failed: " + t);
        }
     }

    // ------------------------------------------------------------------
    // Generator section (builder + preview + equivalents + examples)
    // ------------------------------------------------------------------

    private static HTMLElement buildGenerator() {
        HTMLElement section = el("section", null);
        section.setAttribute("id", "generator");
        section.setAttribute("class", "card");
        section.appendChild(el("h2", "Generator"));
        section.appendChild(el("p", "Pick a cron type, fill each field, and watch the expression take shape. Every field shows its allowed range, names and extras, plus a live reading of the value you typed."));

        HTMLElement typeRow = el("div", null);
        typeRow.setAttribute("class", "row");
        genType = select(TYPES, "QUARTZ");
        genType.setAttribute("id", "gen-type");
        HTMLElement typeLabel = el("label", "Cron type ");
        typeLabel.setAttribute("for", "gen-type");
        typeLabel.appendChild(genType);
        typeRow.appendChild(typeLabel);
        section.appendChild(typeRow);

        genFields = el("div", null);
        genFields.setAttribute("class", "field-grid");
        genFields.setAttribute("role", "group");
        genFields.setAttribute("aria-label", "Cron fields");
        section.appendChild(genFields);

        HTMLElement presets = el("div", null);
        presets.setAttribute("class", "presets");
        for (String preset : PRESETS) {
            HTMLElement button = el("button", preset);
            button.setAttribute("type", "button");
            final String name = preset;
            button.addEventListener("click", e -> applyPreset(name));
            presets.appendChild(button);
        }
        section.appendChild(presets);

        genMessage = el("p", "");
        genMessage.setAttribute("id", "gen-message");
        genMessage.setAttribute("class", "error");
        genMessage.setAttribute("role", "status");
        section.appendChild(genMessage);

        HTMLElement preview = el("figure", null);
        preview.setAttribute("class", "preview");
        preview.appendChild(el("figcaption", "Expression"));
        genTokens = el("code", null);
        genTokens.setAttribute("id", "gen-expression");
        preview.appendChild(genTokens);
        HTMLElement explainBtn = el("button", "Explain this expression");
        explainBtn.setAttribute("type", "button");
        explainBtn.setAttribute("class", "ghost");
        explainBtn.addEventListener("click", e -> sendToExplainer());
        preview.appendChild(explainBtn);
        section.appendChild(preview);

        HTMLElement equivTitle = el("h3", "Same schedule in other cron types");
        section.appendChild(equivTitle);
        genEquiv = el("dl", null);
        genEquiv.setAttribute("id", "gen-equivalents");
        genEquiv.setAttribute("class", "equiv");
        section.appendChild(genEquiv);

        section.appendChild(buildExamples());

        genType.addEventListener("change", e -> safeRebuildGeneratorFields());
        return section;
    }

    private static void rebuildGeneratorFields() {
        CronType type = CronType.valueOf(genType.getValue());
        CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(type);
        clearChildren(genFields);
        genInputs.clear();
        genDefs.clear();
        genMeanings.clear();
        for (FieldDefinition field : Generator.orderedFields(definition)) {
            genDefs.add(field);
            String name = field.getFieldName().name();
            String id = "gen-" + name;

            HTMLElement wrap = el("div", null);
            wrap.setAttribute("class", "field");
            wrap.setAttribute("data-field", name);

            HTMLElement label = el("label", humanName(field.getFieldName()) + " ");
            label.setAttribute("for", id);
            HTMLElement badge = el("span", field.isOptional() ? "optional" : "required");
            badge.setAttribute("class", "badge " + (field.isOptional() ? "opt" : "req"));
            label.appendChild(badge);
            wrap.appendChild(label);

            HTMLInputElement input = textInput(defaultFieldText(field));
            input.setAttribute("id", id);
            input.setAttribute("name", id);
            input.setAttribute("autocomplete", "off");
            input.setAttribute("spellcheck", "false");
            input.setAttribute("aria-describedby", "hint-" + name + " meaning-" + name);
            genInputs.add(input);
            wrap.appendChild(input);

            HTMLElement hint = el("small", Generator.hint(field));
            hint.setAttribute("id", "hint-" + name);
            hint.setAttribute("class", "hint");
            wrap.appendChild(hint);

            HTMLElement meaning = el("small", "");
            meaning.setAttribute("id", "meaning-" + name);
            meaning.setAttribute("class", "meaning");
            genMeanings.add(meaning);
            wrap.appendChild(meaning);

            input.addEventListener("input", e -> refreshGenerator());
            genFields.appendChild(wrap);
        }
        refreshGenerator();
        updateExamplePreviews();
    }

    private static String defaultFieldText(FieldDefinition field) {
        CronFieldName name = field.getFieldName();
        if ((name == CronFieldName.DAY_OF_MONTH || name == CronFieldName.DAY_OF_WEEK)
                && field.getConstraints().getSpecialChars().contains(
                        com.cronutils.model.field.value.SpecialChar.QUESTION_MARK)) {
            return "?";
        }
        return "*";
    }

    private static void refreshGenerator() {
        CronType type = CronType.valueOf(genType.getValue());
        Map<String, String> inputs = new LinkedHashMap<>();
        for (int i = 0; i < genDefs.size(); i++) {
            inputs.put(genDefs.get(i).getFieldName().name(), genInputs.get(i).getValue());
        }
        // Live per-field readings first, so a broken field explains itself in place.
        for (int i = 0; i < genDefs.size(); i++) {
            String reading = Generator.meaning(genDefs.get(i), genInputs.get(i).getValue());
            HTMLElement meaning = genMeanings.get(i);
            meaning.setTextContent(reading);
            boolean bad = reading.startsWith("invalid");
            meaning.setAttribute("class", "meaning " + (bad ? "bad" : "ok"));
            genInputs.get(i).setAttribute("aria-invalid", bad ? "true" : "false");
        }
        try {
            lastGoodGenerated = Generator.generate(type, inputs);
            genMessage.setTextContent("");
            renderTokens(type, lastGoodGenerated);
            renderEquivalents(type, lastGoodGenerated);
        } catch (IllegalArgumentException e) {
            genMessage.setTextContent(e.getMessage() == null ? "Invalid field input." : e.getMessage());
            if (!lastGoodGenerated.isEmpty()) {
                renderTokens(type, lastGoodGenerated);
            } else {
                clearChildren(genTokens);
                genTokens.setTextContent("—");
                clearChildren(genEquiv);
            }
        }
    }

    /** Expression preview: one tinted token per field, titled with the field name. */
    private static void renderTokens(CronType type, String expression) {
        clearChildren(genTokens);
        String[] parts = expression.split(" ");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                genTokens.appendChild(doc.createTextNode(" "));
            }
            HTMLElement token = el("span", parts[i]);
            String field = i < genDefs.size() ? genDefs.get(i).getFieldName().name() : "extra";
            token.setAttribute("class", "tok tok-" + field.toLowerCase().replace('_', '-'));
            token.setAttribute("title", field);
            genTokens.appendChild(token);
        }
    }

    /** Cross-type equivalents for the last good generated expression. */
    private static void renderEquivalents(CronType type, String expression) {
        clearChildren(genEquiv);
        Cron cron;
        try {
            CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(type);
            cron = new CronParser(definition).parse(expression);
            cron.validate();
        } catch (RuntimeException e) {
            return;
        }
        for (Map.Entry<String, String> entry : Generator.equivalents(type, cron).entrySet()) {
            HTMLElement row = el("div", null);
            if (entry.getKey().equals(type.name())) {
                row.setAttribute("class", "same");
            } else if (entry.getValue().startsWith("not available")) {
                row.setAttribute("class", "na");
            }
            HTMLElement term = el("dt", entry.getKey());
            HTMLElement detail = el("dd", null);
            detail.appendChild(el("code", entry.getValue()));
            row.appendChild(term);
            row.appendChild(detail);
            genEquiv.appendChild(row);
        }
    }

    private static void applyPreset(String preset) {
        CronType type = CronType.valueOf(genType.getValue());
        try {
            applyExpression(type, Generator.preset(type, preset));
            genMessage.setTextContent("");
        } catch (IllegalArgumentException e) {
            genMessage.setTextContent(e.getMessage() == null ? "Preset failed." : e.getMessage());
        }
    }

    /** Fills the field grid from a full expression of the current type. */
    private static void applyExpression(CronType type, String expression) {
        CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(type);
        Cron cron = new CronParser(definition).parse(expression);
        Map<CronFieldName, CronField> fields = cron.retrieveFieldsAsMap();
        for (int i = 0; i < genDefs.size(); i++) {
            CronFieldName name = genDefs.get(i).getFieldName();
            if (fields.containsKey(name)) {
                FieldExpression fieldExpression = fields.get(name).getExpression();
                genInputs.get(i).setValue(fieldExpression.asString());
            }
        }
        refreshGenerator();
    }

    private static HTMLElement buildExamples() {
        HTMLElement details = el("details", null);
        details.setAttribute("class", "examples");
        details.appendChild(el("summary", "Examples"));
        details.appendChild(el("p", "Pick one to load it into the generator using the cron type selected above."));
        HTMLElement list = el("ul", null);
        genExampleCodes.clear();
        for (Example example : EXAMPLES) {
            final Example current = example;
            HTMLElement item = el("li", null);
            item.appendChild(el("strong", current.label + " "));
            HTMLElement preview = el("code", "");
            genExampleCodes.add(preview);
            item.appendChild(preview);
            item.appendChild(doc.createTextNode(" "));
            HTMLElement load = el("button", "Use this");
            load.setAttribute("type", "button");
            load.setAttribute("class", "ghost");
            load.addEventListener("click", e -> loadExample(current));
            item.appendChild(load);
            list.appendChild(item);
        }
        details.appendChild(list);
        return details;
    }

    /** Refreshes each example's built expression for the current generator type. */
    private static void updateExamplePreviews() {
        CronType type;
        try {
            type = CronType.valueOf(genType.getValue());
        } catch (RuntimeException e) {
            return;
        }
        for (int i = 0; i < EXAMPLES.length && i < genExampleCodes.size(); i++) {
            try {
                genExampleCodes.get(i).setTextContent(Generator.buildExample(type, EXAMPLES[i].fields));
            } catch (IllegalArgumentException e) {
                genExampleCodes.get(i).setTextContent("Not available in " + type.name());
            }
        }
    }

    private static void loadExample(Example example) {
        CronType type = CronType.valueOf(genType.getValue());
        try {
            applyExpression(type, Generator.buildExample(type, example.fields));
            genMessage.setTextContent("");
        } catch (IllegalArgumentException e) {
            genMessage.setTextContent("Example \"" + example.label + "\" is not supported in "
                    + type.name() + (e.getMessage() == null ? "." : ": " + e.getMessage()));
        }
    }

    private static void sendToExplainer() {
        if (lastGoodGenerated.isEmpty()) {
            return;
        }
        explType.setValue(genType.getValue());
        explInput.setValue(lastGoodGenerated);
        safeRefreshExplainer();
    }

    // ------------------------------------------------------------------
    // Explainer section (type + expression; time and zone are the browser's)
    // ------------------------------------------------------------------

    private static HTMLElement buildExplainer() {
        HTMLElement section = el("section", null);
        section.setAttribute("id", "explainer");
        section.setAttribute("class", "card");
        section.appendChild(el("h2", "Explainer"));
        section.appendChild(el("p", "Paste any expression and get a plain reading plus the next times it fires, counted from right now in your own timezone."));

        HTMLElement controls = el("div", null);
        controls.setAttribute("class", "row");

        explType = select(TYPES, "QUARTZ");
        explType.setAttribute("id", "expl-type");
        HTMLElement typeLabel = el("label", "Cron type ");
        typeLabel.setAttribute("for", "expl-type");
        typeLabel.appendChild(explType);
        controls.appendChild(typeLabel);

        explInput = textInput("0 0 12 * * ?");
        explInput.setAttribute("id", "expl-expression");
        explInput.setAttribute("name", "expl-expression");
        explInput.setAttribute("autocomplete", "off");
        explInput.setAttribute("spellcheck", "false");
        HTMLElement exprLabel = el("label", "Expression ");
        exprLabel.setAttribute("for", "expl-expression");
        exprLabel.appendChild(explInput);
        controls.appendChild(exprLabel);

        explLocale = select(LOCALES, "en_GB");
        explLocale.setAttribute("id", "expl-locale");
        HTMLElement localeLabel = el("label", "Language ");
        localeLabel.setAttribute("for", "expl-locale");
        localeLabel.appendChild(explLocale);
        controls.appendChild(localeLabel);

        section.appendChild(controls);

        explMessage = el("p", "");
        explMessage.setAttribute("id", "expl-message");
        explMessage.setAttribute("role", "status");
        section.appendChild(explMessage);

        HTMLElement grid = el("div", null);
        grid.setAttribute("class", "expl-grid");

        explReading = el("article", null);
        explReading.setAttribute("id", "expl-reading");
        explReading.setAttribute("class", "panel");
        explReading.appendChild(el("h3", "What it means"));
        explDescription = el("p", "");
        explDescription.setAttribute("id", "expl-description");
        explDescription.setAttribute("class", "lede");
        explReading.appendChild(explDescription);
        HTMLElement standard = el("p", "Standard form: ");
        standard.setAttribute("class", "standard");
        explNormalized = el("code", "");
        explNormalized.setAttribute("id", "expl-normalized");
        standard.appendChild(explNormalized);
        explReading.appendChild(standard);
        grid.appendChild(explReading);

        explSchedule = el("article", null);
        explSchedule.setAttribute("id", "expl-schedule");
        explSchedule.setAttribute("class", "panel");
        explSchedule.appendChild(el("h3", "Upcoming runs"));
        explRuns = el("ol", null);
        explRuns.setAttribute("id", "expl-runs");
        explRuns.setAttribute("class", "timeline");
        explSchedule.appendChild(explRuns);
        explMeta = el("p", "");
        explMeta.setAttribute("id", "expl-meta");
        explMeta.setAttribute("class", "meta");
        explSchedule.appendChild(explMeta);
        grid.appendChild(explSchedule);
        section.appendChild(grid);

        HTMLElement editBtn = el("button", "Edit in generator");
        editBtn.setAttribute("type", "button");
        editBtn.setAttribute("class", "ghost");
        editBtn.addEventListener("click", e -> sendToGenerator());
        section.appendChild(editBtn);

        explType.addEventListener("change", e -> safeRefreshExplainer());
        explInput.addEventListener("input", e -> safeRefreshExplainer());
        explLocale.addEventListener("change", e -> safeRefreshExplainer());

        return section;
    }

    private static void refreshExplainer() {
        CronType type = CronType.valueOf(explType.getValue());
        Locale locale = toLocale(explLocale.getValue());
        ZoneId zone = systemZone();
        ZonedDateTime from = systemNow(zone);

        ExplainResult result = Explainer.explain(type, explInput.getValue(), locale, zone, from);
        if (result.hint) {
            explMessage.setTextContent("Enter a cron expression above (nicknames like @yearly work too).");
            explMessage.setAttribute("class", "hint");
            explMessage.setHidden(false);
            explReading.setHidden(true);
            explSchedule.setHidden(true);
        } else if (!result.ok) {
            explMessage.setTextContent(result.error == null ? "Invalid expression." : result.error);
            explMessage.setAttribute("class", "error");
            explMessage.setHidden(false);
            explReading.setHidden(true);
            explSchedule.setHidden(true);
        } else {
            explMessage.setTextContent("");
            explMessage.setAttribute("class", "");
            explMessage.setHidden(true);
            explReading.setHidden(false);
            explSchedule.setHidden(false);
            explDescription.setTextContent(result.description);
            explNormalized.setTextContent(result.normalized);
            explMeta.setTextContent("Checked from " + Explainer.display(from) + " in " + zone.getId() + " (your system time).");
            clearChildren(explRuns);
            if (result.nextRuns.isEmpty()) {
                explRuns.appendChild(el("li", "No upcoming executions found."));
            } else {
                for (ZonedDateTime run : result.nextRuns) {
                    HTMLElement item = el("li", null);
                    HTMLElement time = el("time", Explainer.display(run));
                    time.setAttribute("datetime", run.toString());
                    item.appendChild(time);
                    explRuns.appendChild(item);
                }
            }
        }
    }

    private static void sendToGenerator() {
        String expression = explInput.getValue();
        if (expression == null || expression.trim().isEmpty()) {
            return;
        }
        genType.setValue(explType.getValue());
        rebuildGeneratorFields();
        try {
            applyExpression(CronType.valueOf(genType.getValue()), expression.trim());
            genMessage.setTextContent("");
        } catch (IllegalArgumentException e) {
            genMessage.setTextContent(e.getMessage() == null ? "Cannot edit here." : e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Browser clock: system zone name + current millis via JS interop
    // ------------------------------------------------------------------

    @JSBody(params = {}, script = "return (typeof Intl !== \"undefined\" && Intl.DateTimeFormat)"
            + " ? (Intl.DateTimeFormat().resolvedOptions().timeZone || \"UTC\") : \"UTC\";")
    private static native String jsTimeZone();

    // NOTE: returns double, not long. An i64 return value traps the WASM-GC
    // build at runtime; f64 crosses the JS boundary cleanly and Date.now()
    // stays exactly representable well past 2^53 millis.
    @JSBody(params = {}, script = "return Date.now();")
    private static native double jsNowMillis();

    private static ZoneId systemZone() {
        try {
            String name = jsTimeZone();
            if (name != null && !name.trim().isEmpty()) {
                return ZoneId.of(name.trim());
            }
        } catch (RuntimeException e) {
            // fall through to UTC
        }
        return ZoneId.of("UTC");
    }

    private static ZonedDateTime systemNow(ZoneId zone) {
        long millis;
        try {
            millis = (long) jsNowMillis();
        } catch (RuntimeException e) {
            millis = System.currentTimeMillis();
        }
        return Instant.ofEpochMilli(millis).atZone(zone);
    }

    private static void buildFooter() {
        HTMLElement footer = el("footer", null);
        footer.setAttribute("class", "site");
        HTMLElement link = el("a", "cron-utils on GitHub");
        link.setAttribute("href", "https://github.com/ProjectUnified/cron-utils");
        footer.appendChild(link);
        doc.getBody().appendChild(footer);
    }

    // ------------------------------------------------------------------
    // Parsing helpers
    // ------------------------------------------------------------------

    private static Locale toLocale(String value) {
        if ("en_GB".equals(value)) {
            return Locale.UK;
        }
        try {
            return new Locale(value);
        } catch (RuntimeException e) {
            return Locale.UK;
        }
    }

    private static String humanName(CronFieldName name) {
        switch (name) {
            case SECOND:
                return "Second";
            case MINUTE:
                return "Minute";
            case HOUR:
                return "Hour";
            case DAY_OF_MONTH:
                return "Day of month";
            case MONTH:
                return "Month";
            case DAY_OF_WEEK:
                return "Day of week";
            case YEAR:
                return "Year";
            case DAY_OF_YEAR:
                return "Day of year";
            default:
                return name.name();
        }
    }

    // ------------------------------------------------------------------
    // DOM helpers
    // ------------------------------------------------------------------

    private static HTMLElement el(String tag, String text) {
        HTMLElement element = doc.createElement(tag);
        if (text != null) {
            element.setTextContent(text);
        }
        return element;
    }

    private static HTMLInputElement textInput(String value) {
        HTMLInputElement input = (HTMLInputElement) doc.createElement("input");
        input.setAttribute("type", "text");
        input.setValue(value);
        return input;
    }

    private static HTMLSelectElement select(String[] values, String selected) {
        HTMLSelectElement element = (HTMLSelectElement) doc.createElement("select");
        for (String value : values) {
            HTMLOptionElement option = (HTMLOptionElement) doc.createElement("option");
            option.setValue(value);
            option.setText(value);
            element.getOptions().add(option);
        }
        element.setValue(selected);
        return element;
    }

    private static void clearChildren(HTMLElement element) {
        while (element.getFirstChild() != null) {
            element.removeChild(element.getFirstChild());
        }
    }
}
