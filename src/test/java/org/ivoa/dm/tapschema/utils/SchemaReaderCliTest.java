package org.ivoa.dm.tapschema.utils;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SchemaReaderCliTest {

  @Test
  void parsesNamedOptions() {
    SchemaReader.CliCommand command = new SchemaReader.CliCommand();

    new CommandLine(command)
        .parseArgs(
            "--jdbc-url",
            "jdbc:test",
            "--user",
            "alice",
            "--password",
            "secret",
            "--format",
            "json",
            "--catalog",
            "tap",
            "--schema",
            "public, astro",
            "--schema",
            "extra",
            "--table-type",
            "TABLE, VIEW",
            "--include-system-schemas");

    assertEquals("jdbc:test", command.jdbcUrlOption);
    assertEquals("alice", command.user);
    assertEquals("secret", command.password);
    assertEquals(SchemaReader.OutputFormat.JSON, command.format);
    assertEquals("tap", command.catalog);
    assertEquals(List.of("public, astro", "extra"), command.includeSchemas);
    assertEquals(List.of("TABLE, VIEW"), command.tableTypes);
    assertEquals(true, command.includeSystemSchemas);
  }

  @Test
  void parsesPositionalCompatibilityForm() {
    SchemaReader.CliCommand command = new SchemaReader.CliCommand();

    new CommandLine(command).parseArgs("jdbc:test", "json");

    assertEquals("jdbc:test", command.jdbcUrlPositional);
    assertEquals(SchemaReader.OutputFormat.JSON, command.positionalFormat);
  }

  @Test
  void normalizesCommaSeparatedValues() {
    assertEquals(
        List.of("public", "astro", "extra", "one", "two"),
        SchemaReader.normalizeCsvValues(List.of("public, astro", "extra", "one, two")));
  }
}
