package dk.dtu.compute.cdl.parser;

import static org.assertj.core.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class StatementParserTest {

  @ParameterizedTest
  @MethodSource("provideStringsForInitialParsing")
  public void initialParsingTest(String constraintDefinition, String expectedContext,
      String expectedPredicate) throws StatementParsingException {
    // arrange
    var sut = new StatementParser();

    // act
    sut.ParseInitial(constraintDefinition);
    var actualContext = sut.contextString;
    var actualPredicate = sut.predicateString;

    // assert
    assertThat(actualContext).isEqualTo(expectedContext);
    assertThat(actualPredicate).isEqualTo(expectedPredicate);
  }


  private static Stream<Arguments> provideStringsForInitialParsing() {
    return Stream.of(
        Arguments.of("AGENT a IS BLOCKED BY AGENT b IF a.dest IS b.dest",
            "AGENT a IS BLOCKED BY AGENT b", "a.dest IS b.dest"),
        Arguments.of("AGENT a IS BLOCKED IF a.name IS 'NoOp' and a.time IS LESS THAN 10",
            "AGENT a IS BLOCKED", "a.name IS 'NoOp' and a.time IS LESS THAN 10"));
  }
}
