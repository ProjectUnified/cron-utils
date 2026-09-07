package com.cronutils.web;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.field.CronFieldName;
import com.cronutils.model.field.definition.FieldDefinition;
import com.cronutils.model.field.expression.FieldExpression;
import com.cronutils.model.field.value.SpecialChar;
import com.cronutils.parser.CronParser;
import com.cronutils.web.Explainer.ExplainResult;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;
import org.teavm.jso.dom.html.HTMLOptionElement;
import org.teavm.jso.dom.html.HTMLSelectElement;

import java.time.LocalDateTime;
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
 */
public class CronWebApp {

    private static HTMLDocument doc;

    private static HTMLSelectElement explType;
    private static HTMLInputElement explInput;
    private static HTMLSelectElement explLocale;
    private static HTMLSelectElement explZone;
    private static HTMLInputElement explFrom;
    private static HTMLElement explMessage;
    private static HTMLElement explNormalized;
    private static HTMLElement explDescription;
    private static HTMLElement explRuns;

    private static HTMLSelectElement genType;
    private static HTMLElement genFields;
    private static HTMLElement genMessage;
    private static HTMLElement genOutput;
    private static final List<HTMLInputElement> genInputs = new ArrayList<>();
    private static final List<String> genFieldNames = new ArrayList<>();
    private static String lastGoodGenerated = "";

    private static HTMLElement mapList;

    public static void main(String[] args) {
        doc = HTMLDocument.current();

        HTMLElement header = el("header", null);
        HTMLElement h1 = el("h1", "cron-utils-web");
        header.appendChild(h1);
        header.appendChild(el("p", "Parse, explain, build and map cron expressions. All logic runs in your browser."));
        doc.getBody().appendChild(header);

        buildExplainer();
        buildGenerator();
        buildMapping();
        buildFooter();

        refreshExplainer();
        rebuildGeneratorFields();
    }

    // ------------------------------------------------------------------
    // Explainer section
    // ------------------------------------------------------------------

    private static void buildExplainer() {
        HTMLElement section = el("section", null);
        section.setAttribute("id", "explainer");
        section.appendChild(el("h2", "Explainer"));

        explType = select(new String[]{"CRON4J", "QUARTZ", "UNIX", "SPRING", "SPRING53"}, "QUARTZ");
        explInput = textInput("0 0 12 * * ?");
        explLocale = select(
                new String[]{"en_GB", "de", "el", "es", "fr", "id", "it", "ja",
                        "ko", "nl", "pl", "pt", "ro", "ru", "sw", "tr", "zh"},
                "en_GB");
        explZone = select(new String[]{"UTC", "+02:00", "-05:00",
                "America/New_York", "Europe/London", "Asia/Tokyo"}, "UTC");
        explFrom = textInput("2026-01-01T00:00");

        section.appendChild(labeled("Type", explType));
        section.appendChild(labeled("Expression", explInput));
        section.appendChild(labeled("Locale", explLocale));
        section.appendChild(labeled("Timezone", explZone));
        section.appendChild(labeled("Reference datetime", explFrom));

        explMessage = el("p", "");
        explMessage.setAttribute("id", "expl-message");
        explNormalized = el("p", "");
        explDescription = el("p", "");
        explRuns = el("ul", null);
        section.appendChild(explMessage);
        section.appendChild(explNormalized);
        section.appendChild(explDescription);
        section.appendChild(explRuns);

        explType.addEventListener("change", e -> refreshExplainer());
        explInput.addEventListener("input", e -> refreshExplainer());
        explLocale.addEventListener("change", e -> refreshExplainer());
        explZone.addEventListener("change", e -> refreshExplainer());
        explFrom.addEventListener("input", e -> refreshExplainer());

        doc.getBody().appendChild(section);
    }

    private static void refreshExplainer() {
        CronType type = CronType.valueOf(explType.getValue());
        Locale locale = toLocale(explLocale.getValue());
        ZoneId zone = toZone(explZone.getValue());
        ZonedDateTime from = toDateTime(explFrom.getValue(), zone);

        ExplainResult result = Explainer.explain(type, explInput.getValue(), locale, zone, from);
        if (result.hint) {
            explMessage.setTextContent("Enter a cron expression above (nicknames like @yearly work too).");
            explMessage.setAttribute("class", "hint");
            explNormalized.setTextContent("");
            explDescription.setTextContent("");
            clearChildren(explRuns);
        } else if (!result.ok) {
            explMessage.setTextContent(result.error == null ? "Invalid expression." : result.error);
            explMessage.setAttribute("class", "error");
            explNormalized.setTextContent("");
            explDescription.setTextContent("");
            clearChildren(explRuns);
        } else {
            explMessage.setTextContent("");
            explMessage.setAttribute("class", "");
            explNormalized.setTextContent("Normalized: " + result.normalized);
            explDescription.setTextContent(result.description);
            clearChildren(explRuns);
            if (from == null) {
                explRuns.appendChild(el("li", "Invalid reference datetime; use YYYY-MM-DDTHH:MM."));
            } else if (result.nextRuns.isEmpty()) {
                explRuns.appendChild(el("li", "No upcoming executions found."));
            } else {
                for (String run : result.nextRuns) {
                    explRuns.appendChild(el("li", run));
                }
            }
        }
        refreshMapping();
    }

    // ------------------------------------------------------------------
    // Generator section
    // ------------------------------------------------------------------

    private static void buildGenerator() {
        HTMLElement section = el("section", null);
        section.setAttribute("id", "generator");
        section.appendChild(el("h2", "Generator"));

        genType = select(new String[]{"CRON4J", "QUARTZ", "UNIX", "SPRING", "SPRING53"}, "QUARTZ");
        section.appendChild(labeled("Type", genType));

        genFields = el("div", null);
        section.appendChild(genFields);

        HTMLElement presets = el("div", null);
        presets.setAttribute("class", "presets");
        for (String preset : new String[]{"yearly", "monthly", "weekly", "daily", "hourly"}) {
            HTMLElement button = el("button", preset);
            button.setAttribute("type", "button");
            final String name = preset;
            button.addEventListener("click", e -> applyPreset(name));
            presets.appendChild(button);
        }
        section.appendChild(presets);

        genMessage = el("p", "");
        genMessage.setAttribute("class", "error");
        genOutput = el("p", "");
        section.appendChild(genMessage);
        section.appendChild(genOutput);

        genType.addEventListener("change", e -> rebuildGeneratorFields());
        doc.getBody().appendChild(section);
    }

    private static void rebuildGeneratorFields() {
        CronType type = CronType.valueOf(genType.getValue());
        CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(type);
        clearChildren(genFields);
        genInputs.clear();
        genFieldNames.clear();
        for (FieldDefinition field : Generator.orderedFields(definition)) {
            HTMLInputElement input = textInput(defaultFieldText(field));
            genInputs.add(input);
            genFieldNames.add(field.getFieldName().name());
            genFields.appendChild(labeled(field.getFieldName().name(), input));
            input.addEventListener("input", e -> refreshGenerator());
        }
        refreshGenerator();
    }

    private static String defaultFieldText(FieldDefinition field) {
        CronFieldName name = field.getFieldName();
        if ((name == CronFieldName.DAY_OF_MONTH || name == CronFieldName.DAY_OF_WEEK)
                && field.getConstraints().getSpecialChars().contains(SpecialChar.QUESTION_MARK)) {
            return "?";
        }
        return "*";
    }

    private static void refreshGenerator() {
        CronType type = CronType.valueOf(genType.getValue());
        Map<String, String> inputs = new LinkedHashMap<>();
        for (int i = 0; i < genFieldNames.size(); i++) {
            inputs.put(genFieldNames.get(i), genInputs.get(i).getValue());
        }
        try {
            lastGoodGenerated = Generator.generate(type, inputs);
            genMessage.setTextContent("");
            genOutput.setTextContent("Expression: " + lastGoodGenerated);
        } catch (IllegalArgumentException e) {
            genMessage.setTextContent(e.getMessage() == null ? "Invalid field input." : e.getMessage());
            if (lastGoodGenerated.isEmpty()) {
                genOutput.setTextContent("");
            } else {
                genOutput.setTextContent("Expression: " + lastGoodGenerated);
            }
        }
    }

    private static void applyPreset(String preset) {
        CronType type = CronType.valueOf(genType.getValue());
        try {
            String expression = Generator.preset(type, preset);
            CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(type);
            Cron cron = new CronParser(definition).parse(expression);
            Map<CronFieldName, com.cronutils.model.field.CronField> fields = cron.retrieveFieldsAsMap();
            for (int i = 0; i < genFieldNames.size(); i++) {
                CronFieldName name = CronFieldName.valueOf(genFieldNames.get(i));
                if (fields.containsKey(name)) {
                    FieldExpression fieldExpression = fields.get(name).getExpression();
                    genInputs.get(i).setValue(fieldExpression.asString());
                }
            }
            refreshGenerator();
            genMessage.setTextContent("");
        } catch (IllegalArgumentException e) {
            genMessage.setTextContent(e.getMessage() == null ? "Preset failed." : e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Mapping section
    // ------------------------------------------------------------------

    private static void buildMapping() {
        HTMLElement section = el("section", null);
        section.setAttribute("id", "mapping");
        section.appendChild(el("h2", "Equivalents"));
        mapList = el("ul", null);
        section.appendChild(mapList);
        section.appendChild(el("p", "Out of v1: timezone conversion and bean-validation demos."));
        doc.getBody().appendChild(section);
    }

    private static void refreshMapping() {
        clearChildren(mapList);
        CronType type;
        try {
            type = CronType.valueOf(explType.getValue());
        } catch (RuntimeException e) {
            return;
        }
        String expression = explInput.getValue();
        if (expression == null || expression.trim().isEmpty()) {
            return;
        }
        Cron cron;
        try {
            CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(type);
            cron = new CronParser(definition).parse(expression);
            cron.validate();
        } catch (RuntimeException e) {
            return;
        }
        for (Map.Entry<String, String> entry : Generator.equivalents(type, cron).entrySet()) {
            mapList.appendChild(el("li", entry.getKey() + ": " + entry.getValue()));
        }
    }

    private static void buildFooter() {
        HTMLElement footer = el("footer", null);
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

    private static ZoneId toZone(String value) {
        if ("UTC".equals(value)) {
            return ZoneId.of("UTC");
        }
        try {
            if (value.startsWith("+") || value.startsWith("-")) {
                return ZoneId.of("UTC" + value);
            }
            return ZoneId.of(value);
        } catch (RuntimeException e) {
            return ZoneId.of("UTC");
        }
    }

    private static ZonedDateTime toDateTime(String value, ZoneId zone) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim()).atZone(zone);
        } catch (RuntimeException e) {
            return null;
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

    private static HTMLElement labeled(String labelText, HTMLElement control) {
        HTMLElement label = el("label", labelText + " ");
        label.appendChild(control);
        return label;
    }

    private static void clearChildren(HTMLElement element) {
        while (element.getFirstChild() != null) {
            element.removeChild(element.getFirstChild());
        }
    }
}
