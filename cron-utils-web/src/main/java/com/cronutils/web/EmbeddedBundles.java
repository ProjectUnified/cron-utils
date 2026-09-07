package com.cronutils.web;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;

/**
 * In-memory descriptor bundles for environments where
 * {@code ResourceBundle.getBundle} cannot resolve classpath resources
 * (TeaVM WASM-GC reports "Bundle not found" at runtime even though the
 * lookup compiles). Data comes from {@link EmbeddedBundleData}, vendored
 * verbatim from the descriptor {@code CronUtilsI18N*.properties} files;
 * per-key fallback to the base (English) map mirrors the JVM
 * {@code ResourceBundle} parent chain, including upstream's partial
 * translations.
 */
final class EmbeddedBundles {

    private EmbeddedBundles() {
    }

    /**
     * Bundle for {@code locale}: the language's map with the base map as
     * per-key fallback. Unknown languages fall back to English.
     *
     * @param locale requested locale; null means English
     * @return bundle; never null
     */
    static ResourceBundle forLocale(Locale locale) {
        Locale effective = locale == null ? Locale.UK : locale;
        String language = effective.getLanguage();
        Map<String, String> data = EmbeddedBundleData.forLanguage(language);
        if (data == null) {
            data = EmbeddedBundleData.forLanguage("en");
        }
        if (data == null) {
            data = EmbeddedBundleData.base();
        }
        return new MapBundle(effective, data, EmbeddedBundleData.base());
    }

    private static final class MapBundle extends ResourceBundle {
        private final Locale locale;
        private final Map<String, String> data;
        private final Map<String, String> parent;

        MapBundle(Locale locale, Map<String, String> data, Map<String, String> parent) {
            this.locale = locale;
            this.data = data;
            this.parent = parent;
        }

        @Override
        protected Object handleGetObject(String key) {
            if (data.containsKey(key)) {
                return data.get(key);
            }
            if (parent.containsKey(key)) {
                return parent.get(key);
            }
            throw new MissingResourceException("Can't find resource for bundle, key " + key,
                    MapBundle.class.getName(), key);
        }

        @Override
        public Enumeration<String> getKeys() {
            final List<String> keys = new ArrayList<>(parent.keySet());
            for (String key : data.keySet()) {
                if (!keys.contains(key)) {
                    keys.add(key);
                }
            }
            return new Enumeration<String>() {
                private int index;

                @Override
                public boolean hasMoreElements() {
                    return index < keys.size();
                }

                @Override
                public String nextElement() {
                    if (!hasMoreElements()) {
                        throw new NoSuchElementException();
                    }
                    index++;
                    return keys.get(index - 1);
                }
            };
        }

        @Override
        public Locale getLocale() {
            return locale;
        }
    }
}
