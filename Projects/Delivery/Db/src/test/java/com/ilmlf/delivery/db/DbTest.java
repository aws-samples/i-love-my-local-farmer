package com.ilmlf.delivery.db;

import io.github.jsonSnapshot.SnapshotMatcher;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import software.amazon.awscdk.core.App;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import software.amazon.awscdk.core.AppProps;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

import static io.github.jsonSnapshot.SnapshotMatcher.expect;
import static org.assertj.core.api.Assertions.assertThat;

public class DbTest {
    private final static ObjectMapper JSON =
        new ObjectMapper()
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    @Test
    public void testStack() {
        App app = new App();

        DbStack stack = new DbStack(app, "test");

        // synthesize the stack to a CloudFormation template
        JsonNode actual =
            JSON.valueToTree(app.synth().getStackArtifact(stack.getArtifactId()).getTemplate());

        expect(actual).toMatchSnapshot();

    }
}
