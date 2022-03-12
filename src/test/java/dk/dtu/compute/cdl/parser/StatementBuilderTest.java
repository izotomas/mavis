package dk.dtu.compute.cdl.parser;

import java.util.stream.Stream;
import org.javatuples.Pair;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import dk.dtu.compute.cdl.model.Action;

public class StatementBuilderTest {

  final static StatementParser PARSER = new StatementParser();

  @ParameterizedTest
  @MethodSource("provideValidConstraintDefinitions")
  public void validationTest(String constraintDefinition) throws StatementParsingException {
    // arrange
    var sut = PARSER.Parse(constraintDefinition);
    sut.withRequestingActionContext(new Action(new Pair<>(1, 1), new Pair<>(1, 1), 1, "NAME"));
    sut.withBlockingActionContext(new Action(new Pair<>(1, 1), new Pair<>(1, 1), 1, "NAME"));

    // act
    sut.validate();
  }

  private static Stream<Arguments> provideValidConstraintDefinitions() {
    return Stream.of(
        Arguments.of("ACTION a IS BLOCKED IF a.name IS 'NoOp' AND a.time IS LESS THAN 10"),
        Arguments.of("ACTION action IS BLOCKED BY ACTION other IF action.dest IS other.origin"));
  }
}
