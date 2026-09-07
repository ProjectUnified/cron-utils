package com.cronutils.web;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the vendored {@link EmbeddedBundleData} maps against drift from the
 * descriptor {@code CronUtilsI18N*.properties} files: every shipped key must
 * resolve identically, and descriptions built on embedded bundles must match
 * the JVM describer in every locale, including day/month name paths.
 */
class EmbeddedBundlesTest {

    private static final String BUNDLE = "com.cronutils.CronUtilsI18N";

    private static List<Locale> locales() {
        List<Locale> locales = new ArrayList<>();
        locales.add(Locale.UK);
        for (String language : new String[]{"de", "el", "en", "es", "fr", "id", "it",
                "ja", "ko", "nl", "pl", "pt", "ro", "ru", "sw", "tr", "zh"}) {
            locales.add(new Locale(language));
        }
        return locales;
    }

    private static List<Cron> samples() {
        List<Cron> crons = new ArrayList<>();
        crons.add(parse(CronType.QUARTZ, "0 0 12 * * ?"));
        crons.add(parse(CronType.QUARTZ, "0 0 12 ? * 2"));
        crons.add(parse(CronType.QUARTZ, "0 0 12 1 6 ?"));
        crons.add(parse(CronType.UNIX, "0 12 * * *"));
        crons.add(parse(CronType.CRON4J, "0 12 * * *"));
        crons.add(parse(CronType.SPRING, "0 0 12 * * ?"));
        return crons;
    }

    private static Cron parse(CronType type, String expression) {
        return new CronParser(CronDefinitionBuilder.instanceDefinitionFor(type)).parse(expression);
    }

    @Test
    void everyShippedKeyResolvesIdentically() {
        for (Locale locale : locales()) {
            ResourceBundle real = ResourceBundle.getBundle(BUNDLE, locale);
            ResourceBundle embedded = EmbeddedBundles.forLocale(locale);
            List<String> keys = Collections.list(real.getKeys());
            assertTrue(keys.size() > 20, "expected bundle keys for " + locale);
            for (String key : keys) {
                assertEquals(real.getString(key), embedded.getString(key),
                        "key " + key + " in " + locale);
            }
        }
    }

    @Test
    void describeMatchesJvmDescriberInEveryLocale() {
        for (Locale locale : locales()) {
            CronDescriptor real = CronDescriptor.instance(locale);
            CronDescriptor embedded = new CronDescriptor(EmbeddedBundles.forLocale(locale));
            for (Cron cron : samples()) {
                assertEquals(real.describe(cron), embedded.describe(cron),
                        cron.asString() + " in " + locale);
            }
        }
    }
}
