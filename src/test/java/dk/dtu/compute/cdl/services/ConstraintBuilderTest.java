package dk.dtu.compute.cdl.services;

import static org.assertj.core.api.Assertions.*;
import java.util.stream.Stream;
import org.javatuples.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import dk.dtu.compute.cdl.errors.StatementParsingException;
import dk.dtu.compute.cdl.model.Action;

public class ConstraintBuilderTest {

  final static ConstraintParser PARSER = new ConstraintParser();

  @ParameterizedTest
  @MethodSource("provideValidConstraintDefinitions")
  public void validationTest(String constraintDefinition) throws StatementParsingException {
    // arrange
    var sut = PARSER.Parse(constraintDefinition)
        .withRequestingActionContext(new Action(new Pair<>(1, 1), new Pair<>(1, 1), 1, "NAME"))
        .withBlockingActionContext(new Action(new Pair<>(1, 1), new Pair<>(1, 1), 1, "NAME"));

    // act
    sut.validate();
  }

  @ParameterizedTest
  @MethodSource("provideValidSingleActionArgs")
  public void buildWithSingleActionTest(String constraintDefinition, Action requestingContext,
      boolean expectedEvaluation) throws StatementParsingException {
    // arrange
    var sut =
        PARSER.Parse(constraintDefinition).withRequestingActionContext(requestingContext).build();

    // act
    var actual = sut.evaluate();

    // assert
    assertThat(actual).isEqualTo(expectedEvaluation);
  }

  @ParameterizedTest
  @MethodSource("provideValidDoubleActionArgs")
  public void buildWithDoubleActionTest(String constraintDefinition, Action requesting,
      Action blocking, boolean expectedEvaluation) throws StatementParsingException {
    // arrange
    var sut = PARSER.Parse(constraintDefinition).withRequestingActionContext(requesting)
        .withBlockingActionContext(blocking).build();

    // act
    var actual = sut.evaluate();

    // assert
    assertThat(actual).isEqualTo(expectedEvaluation);
  }


  private static Stream<Arguments> provideValidConstraintDefinitions() {
    return Stream.of(
        Arguments.of("ACTION a IS BLOCKED IF a.name IS 'NoOp' AND a.time IS LESS THAN 10"),
        Arguments.of("ACTION action IS BLOCKED BY ACTION other IF action.dest IS other.origin"));
  }

  private static Stream<Arguments> provideValidSingleActionArgs() {
    return Stream.of(
        Arguments.of("ACTION a IS BLOCKED IF a.name IS 'NoOp' AND a.time IS LESS THAN 10",
            new Action(null, null, 0, "NoOp"), true),
        Arguments.of("ACTION a IS BLOCKED IF a.name IS 'NoOp' AND a.time IS LESS THAN 10",
            new Action(null, null, 11, "NoOp"), false),
        Arguments.of("ACTION a IS BLOCKED IF a.name IS 'NoOp' OR a.time IS LESS THAN 10",
            new Action(null, null, 11, "NoOp"), true),
        Arguments.of("ACTION a IS BLOCKED IF a.name IS NOT 'NoOp' OR a.time IS LESS THAN 10",
            new Action(null, null, 11, "NoOp"), false),
        Arguments.of("ACTION a IS BLOCKED IF a.name IS NOT 'NoOp' AND a.time IS LESS THAN 10",
            new Action(null, null, 10, "Push"), false));
  }

  private static Stream<Arguments> provideValidDoubleActionArgs() {
    return Stream.of(Arguments.of("ACTION a IS BLOCKED BY ACTION b IF a.dest IS b.dest",
        new Action(null, new Pair<>(1, 1), 0, null), new Action(null, new Pair<>(1, 1), 0, null),
        true));
  }
}
