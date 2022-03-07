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

  @ParameterizedTest
  @MethodSource("provideStringsForContextMapping")
  public void contextMappingTest(String context, String expectedEntry1, String expectedEntry2)
      throws StatementParsingException {
    // arrange
    var sut = new StatementParser();

    // act
    sut.ParseContext(context);
    var actualEntry1 = sut.builder.contextMap.get("entry1");
    var actualEntry2 = sut.builder.contextMap.get("entry2");

    // assert
    assertThat(actualEntry1).isEqualTo(expectedEntry1);
    assertThat(actualEntry2).isEqualTo(expectedEntry2);
  }

  @ParameterizedTest
  @MethodSource("provideStringsForPredicateParsing")
  public void predicateParsingTest(String predicate, int expectedExpressionCount)
      throws StatementParsingException {
    // arrange
    var sut = new StatementParser();

    // act
    sut.ParsePredicate(predicate);
    var actual = sut.builder.expressionList;

    // assert
    assertThat(actual.size()).isEqualTo(expectedExpressionCount);
  }

  private static Stream<Arguments> provideStringsForPredicateParsing() {
    return Stream.of(Arguments.of("a.name IS 'NoOp' AND a.time IS LESS THAN 10", 2),
        Arguments.of("action.dest IS other.origin", 1), Arguments.of("a.dest IS b.dest", 1),
        Arguments.of("a.name IS NOT 'NoOp' AND NOT a.time IS MORE THAN 5 AND a.time IS MORE THAN 1",
            3));
  }

  private static Stream<Arguments> provideStringsForInitialParsing() {
    return Stream.of(
        Arguments.of("ACTION a IS BLOCKED BY ACTION b IF a.dest IS b.dest",
            "ACTION a IS BLOCKED BY ACTION b", "a.dest IS b.dest"),
        Arguments.of("ACTION a IS BLOCKED IF a.name IS 'NoOp' and a.time IS LESS THAN 10",
            "ACTION a IS BLOCKED", "a.name IS 'NoOp' and a.time IS LESS THAN 10"));
  }

  private static Stream<Arguments> provideStringsForContextMapping() {
    return Stream.of(Arguments.of("ACTION a IS BLOCKED BY ACTION b", "a", "b"),
        Arguments.of("ACTION agent IS BLOCKED BY ACTION other", "agent", "other"),
        Arguments.of("ACTION action IS BLOCKED", "action", null), Arguments.of(
            "ACTION myaction IS BLOCKED BY ACTION someotheraction", "myaction", "someotheraction"));
  }
}
