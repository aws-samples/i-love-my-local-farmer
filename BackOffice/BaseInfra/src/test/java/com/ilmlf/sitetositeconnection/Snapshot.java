package com.ilmlf.sitetositeconnection;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.UncheckedIOException;

public final class Snapshot {
    private static final ObjectMapper objectMapper = buildObjectMapper();
    private static final PrettyPrinter pp = buildDefaultPrettyPrinter();

    /**
     * Workaround for an incompatibility between latest Jackson and json-snapshot libs.
     * <p>
     * Intended to replace {@code io.github.jsonSnapshot.SnapshotMatcher#defaultJsonFunction}
     *
     * @see <a href="https://github.com/json-snapshot/json-snapshot.github.io/issues/27">Issue in json-snapshot project</a>
     */
    public static String asJsonString(Object object) {
        try {
            return objectMapper.writer(pp).writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Unmodified copy of {@code io.github.jsonSnapshot.SnapshotMatcher#buildObjectMapper}
     */
    private static ObjectMapper buildObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        objectMapper.setVisibility(
                objectMapper
                        .getSerializationConfig()
                        .getDefaultVisibilityChecker()
                        .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                        .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                        .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                        .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        return objectMapper;
    }

    /**
     * Modified copy of {@code io.github.jsonSnapshot.SnapshotMatcher#buildDefaultPrettyPrinter}
     */
    private static PrettyPrinter buildDefaultPrettyPrinter() {
        DefaultPrettyPrinter pp =
                new DefaultPrettyPrinter("") {
                    @Override
                    public DefaultPrettyPrinter createInstance() {
                        return this;
                    }

                    @Override
                    public DefaultPrettyPrinter withSeparators(Separators separators) {
                        this._separators = separators;
                        this._objectFieldValueSeparatorWithSpaces =
                                separators.getObjectFieldValueSeparator() + " ";
                        return this;
                    }
                };
        DefaultPrettyPrinter.Indenter lfOnlyIndenter = new DefaultIndenter("  ", "\n");
        pp.indentArraysWith(lfOnlyIndenter);
        pp.indentObjectsWith(lfOnlyIndenter);
        return pp;
    }
}