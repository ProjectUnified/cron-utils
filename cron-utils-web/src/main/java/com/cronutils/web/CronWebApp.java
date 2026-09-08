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
 * <p>Page shape: {@code header} intro, {@code main} with the shared cron-type
 * section and the generator (per-field builder, editable expression box with
 * copy, plain reading plus upcoming runs, cross-type equivalents, expandable
 * examples; time and zone always come from the browser), then {@code footer}.</p>
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

    private static HTMLSelectElement cronType;
    private static CronType previousType = CronType.QUARTZ;

    private static HTMLInputElement genExpression;
    private static HTMLElement genCopyNote;
    private static HTMLSelectElement genLocale;
    private static HTMLElement genStatus;
    private static HTMLElement genReading;
    private static HTMLElement genSchedule;
    private static HTMLElement genNormalized;
    private static HTMLElement genDescription;
    private static HTMLElement genRunsMeta;
    private static HTMLElement genRuns;

    private static HTMLElement genFields;
    private static HTMLElement genMessage;
    private static HTMLElement genEquiv;
    private static final List<HTMLInputElement> genInputs = new ArrayList<>();
    private static final List<FieldDefinition> genDefs = new ArrayList<>();
    private static final List<HTMLElement> genMeanings = new ArrayList<>();
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

        main.appendChild(buildTypeBar());
        main.appendChild(buildGenerator());

        buildFooter();
        safeRebuildGeneratorFields();
    }

    private static void safeRebuildGeneratorFields() {
        try {
            rebuildGeneratorFields();
        } catch (Throwable t) {
            genMessage.setTextContent("Generator failed: " + t);
        }
     }

    // ------------------------------------------------------------------
    // Shared cron type (one selector drives the single tool below)
    // ------------------------------------------------------------------

    private static HTMLElement buildTypeBar() {
        HTMLElement section = el("section", null);
        section.setAttribute("id", "cron-type-section");
        section.setAttribute("class", "card typebar");
        section.appendChild(el("h2", "Cron type"));
        section.appendChild(el("p", "This type drives the builder and the reading below. Switching carries your schedule over where the dialects map."));
        HTMLElement row = el("div", null);
        row.setAttribute("class", "row");
        cronType = select(TYPES, "QUARTZ");
        cronType.setAttribute("id", "cron-type");
        HTMLElement typeLabel = el("label", "Cron type ");
        typeLabel.setAttribute("for", "cron-type");
        typeLabel.appendChild(cronType);
        row.appendChild(typeLabel);
        section.appendChild(row);
        cronType.addEventListener("change", e -> onTypeChange());
        return section;
    }

    /** Rebuilds the fields for the newly selected type, carrying the current
     * schedule over where the dialects map and resetting otherwise. */
    private static void onTypeChange() {
        CronType next;
        try {
            next = currentType();
        } catch (RuntimeException e) {
            return;
        }
        CronType prev = previousType;
        previousType = next;
        String current = lastGoodGenerated;
        if (current == null || current.isEmpty()) {
            String box = genExpression.getValue();
            current = box == null ? "" : box.trim();
        }
        safeRebuildGeneratorFields();
        if (current.isEmpty() || prev == next) {
            return;
        }
        String carried = Generator.carryOver(prev, current, next);
        if (carried == null) {
            genMessage.setTextContent("Could not carry the schedule over to "
                    + next.name() + "; fields were reset.");
            return;
        }
        try {
            applyExpression(next, carried);
            genMessage.setTextContent("");
        } catch (IllegalArgumentException e) {
            genMessage.setTextContent(e.getMessage() == null ? "Could not reuse schedule." : e.getMessage());
        }
        refreshGenerator();
    }

    private static CronType currentType() {
        return CronType.valueOf(cronType.getValue());
    }

    // ------------------------------------------------------------------
    // Generator section (builder + expression + reading + equivalents + examples)
    // ------------------------------------------------------------------

    private static HTMLElement buildGenerator() {
        HTMLElement section = el("section", null);
        section.setAttribute("id", "generator");
        section.setAttribute("class", "card");
        section.appendChild(el("h2", "Generator"));
        section.appendChild(el("p", "Fill each field below, or paste an expression straight into the Expression box. Every card shows its allowed values, clickable starters, a syntax shelf with examples, and a live reading of the value you typed."));
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

        section.appendChild(buildExpressionRow());
        section.appendChild(buildReading());

        HTMLElement equivSection = el("section", null);
        equivSection.setAttribute("class", "equiv-block");
        equivSection.appendChild(el("h3", "Same schedule in other cron types"));
        genEquiv = el("dl", null);
        genEquiv.setAttribute("id", "gen-equivalents");
        genEquiv.setAttribute("class", "equiv");
        equivSection.appendChild(genEquiv);
        section.appendChild(equivSection);

        section.appendChild(buildExamples());

        return section;
    }

    /** Editable expression box with copy button and language picker. */
    private static HTMLElement buildExpressionRow() {
        HTMLElement wrap = el("div", null);
        wrap.setAttribute("class", "expr-block");
        wrap.appendChild(el("h3", "Expression"));
        HTMLElement row = el("div", null);
        row.setAttribute("class", "row expr-row");
        genExpression = textInput("");
        genExpression.setAttribute("id", "gen-expression");
        genExpression.setAttribute("name", "gen-expression");
        genExpression.setAttribute("autocomplete", "off");
        genExpression.setAttribute("spellcheck", "false");
        HTMLElement exprLabel = el("label", "Expression ");
        exprLabel.setAttribute("for", "gen-expression");
        exprLabel.appendChild(genExpression);
        row.appendChild(exprLabel);
        genLocale = select(LOCALES, "en_GB");
        genLocale.setAttribute("id", "gen-locale");
        HTMLElement localeLabel = el("label", "Language ");
        localeLabel.setAttribute("for", "gen-locale");
        localeLabel.appendChild(genLocale);
        row.appendChild(localeLabel);
        wrap.appendChild(row);
        HTMLElement copyRow = el("div", null);
        copyRow.setAttribute("class", "copy-row");
        HTMLElement copyBtn = el("button", "Copy expression");
        copyBtn.setAttribute("type", "button");
        copyBtn.setAttribute("id", "gen-copy");
        copyBtn.addEventListener("click", e -> copyExpression());
        copyRow.appendChild(copyBtn);
        genCopyNote = el("small", "");
        genCopyNote.setAttribute("id", "gen-copy-note");
        genCopyNote.setAttribute("class", "meta");
        genCopyNote.setAttribute("role", "status");
        copyRow.appendChild(genCopyNote);
        wrap.appendChild(copyRow);
        genExpression.addEventListener("input", e -> onExpressionInput());
        genLocale.addEventListener("change", e -> refreshReading());
        return wrap;
    }

    /** Plain reading plus upcoming runs for the current expression. */
    private static HTMLElement buildReading() {
        HTMLElement wrap = el("div", null);
        wrap.setAttribute("class", "read-block");
        genStatus = el("p", "");
        genStatus.setAttribute("id", "gen-status");
        genStatus.setAttribute("role", "status");
        wrap.appendChild(genStatus);
        HTMLElement grid = el("div", null);
        grid.setAttribute("class", "read-grid");
        genReading = el("article", null);
        genReading.setAttribute("id", "gen-reading");
        genReading.setAttribute("class", "panel");
        genReading.appendChild(el("h3", "What it means"));
        genDescription = el("p", "");
        genDescription.setAttribute("id", "gen-description");
        genDescription.setAttribute("class", "lede");
        genReading.appendChild(genDescription);
        HTMLElement standard = el("p", "Standard form: ");
        standard.setAttribute("class", "standard");
        genNormalized = el("code", "");
        genNormalized.setAttribute("id", "gen-normalized");
        standard.appendChild(genNormalized);
        genReading.appendChild(standard);
        grid.appendChild(genReading);
        genSchedule = el("article", null);
        genSchedule.setAttribute("id", "gen-schedule");
        genSchedule.setAttribute("class", "panel");
        genSchedule.appendChild(el("h3", "Upcoming runs"));
        genRuns = el("ol", null);
        genRuns.setAttribute("id", "gen-runs");
        genRuns.setAttribute("class", "timeline");
        genSchedule.appendChild(genRuns);
        genRunsMeta = el("p", "");
        genRunsMeta.setAttribute("id", "gen-runs-meta");
        genRunsMeta.setAttribute("class", "meta");
        genSchedule.appendChild(genRunsMeta);
        grid.appendChild(genSchedule);
        wrap.appendChild(grid);
        return wrap;
    }

    private static void rebuildGeneratorFields() {
        CronType type = currentType();
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
            input.setAttribute("aria-describedby", "hint-" + name + " meaning-" + name + " guide-" + name);
            genInputs.add(input);
            wrap.appendChild(input);

            HTMLElement hint = el("small", Generator.hint(field));
            hint.setAttribute("id", "hint-" + name);
            hint.setAttribute("class", "hint");
            wrap.appendChild(hint);

            wrap.appendChild(buildQuickRow(field, input));
            wrap.appendChild(buildFieldGuide(field));

            HTMLElement meaning = el("small", "");
            meaning.setAttribute("id", "meaning-" + name);
            meaning.setAttribute("class", "meaning");
            genMeanings.add(meaning);
            wrap.appendChild(meaning);

            input.addEventListener("input", e -> refreshGenerator());
            genFields.appendChild(wrap);
        }
        refreshGenerator();
    }

    /** Clickable starter values: each chip fills its field and refreshes the generator. */
    private static HTMLElement buildQuickRow(FieldDefinition field, HTMLInputElement input) {
        HTMLElement row = el("div", null);
        row.setAttribute("class", "try");
        row.appendChild(el("span", "Try: "));
        for (String value : Generator.quickValues(field)) {
            final String starter = value;
            HTMLElement chip = el("button", starter);
            chip.setAttribute("type", "button");
            chip.setAttribute("title", "Use " + starter + " in " + humanName(field.getFieldName()));
            chip.addEventListener("click", e -> {
                input.setValue(starter);
                refreshGenerator();
            });
            row.appendChild(chip);
        }
        return row;
    }

    /** Teachable operator shelf for one field: shape, clickable example, plain effect. */
    private static HTMLElement buildFieldGuide(FieldDefinition field) {
        String name = field.getFieldName().name();
        HTMLElement details = el("details", null);
        details.setAttribute("class", "guide");
        details.setAttribute("id", "guide-" + name);
        details.appendChild(el("summary", "How to write " + humanName(field.getFieldName()).toLowerCase() + " values"));
        HTMLElement list = el("ul", null);
        for (Generator.SyntaxItem item : Generator.syntaxGuide(field)) {
            final Generator.SyntaxItem current = item;
            HTMLElement row = el("li", null);
            row.appendChild(el("code", current.pattern));
            row.appendChild(doc.createTextNode(" "));
            if (current.example.isEmpty()) {
                HTMLElement clear = el("button", "clear field");
                clear.setAttribute("type", "button");
                clear.setAttribute("class", "ghost");
                clear.addEventListener("click", e -> {
                    for (int i = 0; i < genDefs.size(); i++) {
                        if (genDefs.get(i).getFieldName() == field.getFieldName()) {
                            genInputs.get(i).setValue("");
                        }
                    }
                    refreshGenerator();
                });
                row.appendChild(clear);
            } else {
                final String example = current.example;
                HTMLElement use = el("button", example);
                use.setAttribute("type", "button");
                use.setAttribute("class", "ghost");
                use.setAttribute("title", "Use " + example);
                use.addEventListener("click", e -> {
                    for (int i = 0; i < genDefs.size(); i++) {
                        if (genDefs.get(i).getFieldName() == field.getFieldName()) {
                            genInputs.get(i).setValue(example);
                        }
                    }
                    refreshGenerator();
                });
                row.appendChild(use);
            }
            row.appendChild(doc.createTextNode(" - " + current.explains));
            list.appendChild(row);
        }
        details.appendChild(list);
        return details;
    }

    private static String defaultFieldText(FieldDefinition field) {
        return Generator.defaultValue(field);
    }

    private static void refreshGenerator() {
        CronType type = currentType();
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
            genExpression.setValue(lastGoodGenerated);
            renderEquivalents(type, lastGoodGenerated);
        } catch (IllegalArgumentException e) {
            genMessage.setTextContent(e.getMessage() == null ? "Invalid field input." : e.getMessage());
            if (lastGoodGenerated.isEmpty()) {
                clearChildren(genEquiv);
            } else {
                renderEquivalents(type, lastGoodGenerated);
            }
        }
        refreshReading();
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
        CronType type = currentType();
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
        details.appendChild(el("p", "Pick one to load it into the generator using the shared cron type above."));
        HTMLElement list = el("ul", null);
        for (Example example : EXAMPLES) {
            final Example current = example;
            HTMLElement item = el("li", null);
            item.appendChild(el("strong", current.label + " "));
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


    private static void loadExample(Example example) {
        CronType type = currentType();
        try {
            applyExpression(type, Generator.buildExample(type, example.fields));
            genMessage.setTextContent("");
        } catch (IllegalArgumentException e) {
            genMessage.setTextContent("Example \"" + example.label + "\" is not supported in "
                    + type.name() + (e.getMessage() == null ? "." : ": " + e.getMessage()));
        }
    }


    // ------------------------------------------------------------------
    // Expression box: paste to fill the fields, copy to take it elsewhere
    // ------------------------------------------------------------------

    private static void onExpressionInput() {
        String text = genExpression.getValue();
        if (text == null || text.trim().isEmpty()) {
            genMessage.setTextContent("");
            refreshReading();
            return;
        }
        try {
            applyExpression(currentType(), text.trim());
            genMessage.setTextContent("");
        } catch (IllegalArgumentException e) {
            genMessage.setTextContent(e.getMessage() == null ? "Cannot use here." : e.getMessage());
        }
        refreshReading();
    }

    private static void refreshReading() {
        CronType type = currentType();
        Locale locale = toLocale(genLocale.getValue());
        ZoneId zone = systemZone();
        ZonedDateTime from = systemNow(zone);

        ExplainResult result = Explainer.explain(type, genExpression.getValue(), locale, zone, from);
        if (result.hint) {
            genStatus.setTextContent("Type or paste a cron expression above.");
            genStatus.setAttribute("class", "hint");
            genStatus.setHidden(false);
            genReading.setHidden(true);
            genSchedule.setHidden(true);
        } else if (!result.ok) {
            genStatus.setTextContent(result.error == null ? "Invalid expression." : result.error);
            genStatus.setAttribute("class", "error");
            genStatus.setHidden(false);
            genReading.setHidden(true);
            genSchedule.setHidden(true);
        } else {
            genStatus.setTextContent("");
            genStatus.setAttribute("class", "");
            genStatus.setHidden(true);
            genReading.setHidden(false);
            genSchedule.setHidden(false);
            genDescription.setTextContent(result.description);
            genNormalized.setTextContent(result.normalized);
            genRunsMeta.setTextContent("Checked from " + Explainer.display(from) + " in " + zone.getId() + " (your system time).");
            clearChildren(genRuns);
            if (result.nextRuns.isEmpty()) {
                genRuns.appendChild(el("li", "No upcoming executions found."));
            } else {
                for (ZonedDateTime run : result.nextRuns) {
                    HTMLElement item = el("li", null);
                    HTMLElement time = el("time", Explainer.display(run));
                    time.setAttribute("datetime", run.toString());
                    item.appendChild(time);
                    genRuns.appendChild(item);
                }
            }
        }
    }

    private static void copyExpression() {
        String text = genExpression.getValue();
        if (text == null || text.trim().isEmpty()) {
            genCopyNote.setTextContent("Nothing to copy yet.");
            return;
        }
        if (jsCopy(text)) {
            genCopyNote.setTextContent("Copied.");
        } else {
            genExpression.focus();
            genExpression.select();
            genCopyNote.setTextContent("Copy failed in this browser: the expression is selected, press Ctrl+C.");
        }
    }

    // NOTE: the clipboard fast path is fire-and-forget: writeText returns a
    // promise we do not await (@JSBody is synchronous), so true there only
    // means the API is present. The execCommand fallback is synchronous and
    // covers insecure contexts (plain http on a non-localhost host, embedding
    // iframes) where navigator.clipboard does not exist.
    @JSBody(params = {"text"},
            script = "try {"
            + " if (navigator.clipboard && navigator.clipboard.writeText)"
            + " { navigator.clipboard.writeText(text); return true; }"
            + " var ta = document.createElement('textarea');"
            + " ta.value = text;"
            + " ta.setAttribute('readonly', '');"
            + " ta.style.position = 'absolute';"
            + " ta.style.left = '-9999px';"
            + " document.body.appendChild(ta);"
            + " ta.select();"
            + " var ok = false;"
            + " try { ok = document.execCommand('copy'); } catch (e) { ok = false; }"
            + " document.body.removeChild(ta);"
            + " return ok;"
            + "} catch (e) { return false; }")
    private static native boolean jsCopy(String text);

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
