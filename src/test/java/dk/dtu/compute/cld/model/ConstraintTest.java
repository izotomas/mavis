package dk.dtu.compute.cld.model;

import static org.assertj.core.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import dk.dtu.compute.cld.errors.StatementParsingException;
import dk.dtu.compute.cld.services.ConstraintParser;

public class ConstraintTest {
  private final static ConstraintParser PARSER = new ConstraintParser();

  @Test
  public void followingConstraintTriggersTest()
      throws IllegalStateException, StatementParsingException {
    // arrange
    var followingConstraint = "ACTION a IS BLOCKED BY ACTION b IF a.dest IS b.origin";
    var sut = PARSER.Parse(followingConstraint).withRequestingContext(new Action(1, 0, 1, 1))
        .withRestrictingContext(new Action(1, 1, 1, 2)).build();


    // act
    var actual = sut.evaluate();

    // assert
    assertThat(actual).isTrue();
  }

  @Test
  public void followingConstraintNotTriggersTest()
      throws IllegalStateException, StatementParsingException {
    // arrange
    var followingConstraint = "ACTION a IS BLOCKED BY ACTION b IF a.dest IS b.origin";
    var sut = PARSER.Parse(followingConstraint).withRequestingContext(new Action(1, 0, 1, 0))
        .withRestrictingContext(new Action(1, 1, 1, 2)).build();


    // act
    var actual = sut.evaluate();

    // assert
    assertThat(actual).isFalse();
  }


  @ParameterizedTest
  @MethodSource("provideActionsWithVertexOrFollowingConflict")
  public void followingAndVertexConstraintTriggersTest(Action requesting, Action restricting)
      throws IllegalStateException, StatementParsingException {
    // arrange
    var followingAndVertexConstraint =
        "ACTION a IS BLOCKED BY ACTION b IF a.dest IS b.origin OR a.dest IS b.dest";
    var sut = PARSER.Parse(followingAndVertexConstraint).withRequestingContext(requesting)
        .withRestrictingContext(restricting).build();

    // act
    var actual = sut.evaluate();

    // assert
    assertThat(actual).isTrue();
  }

  private static Stream<Arguments> provideActionsWithVertexOrFollowingConflict() {
    return Stream.of(Arguments.of(new Action(1, 0, 1, 1), new Action(1, 1, 1, 2)),
        Arguments.of(new Action(1, 0, 1, 1), new Action(1, 0, 1, 1)));
  }

  @ParameterizedTest
  @MethodSource("provideSwappingConstraintDefinitions")
  public void swappingConstraintTriggersTest(String swappingConstraint)
      throws IllegalStateException, StatementParsingException {
    // arrange
    var sut = PARSER.Parse(swappingConstraint).withRequestingContext(new Action(1, 1, 1, 2))
        .withRestrictingContext(new Action(1, 2, 1, 1)).build();

    // act
    var actual = sut.evaluate();

    // assert
    assertThat(actual).isTrue();
  }

  @ParameterizedTest
  @MethodSource("provideSwappingConstraintDefinitions")
  public void swappingConstraintNotTriggersTest(String swappingConstraint)
      throws IllegalStateException, StatementParsingException {
    // arrange
    var sut = PARSER.Parse(swappingConstraint).withRequestingContext(new Action(1, 0, 2, 0))
        .withRestrictingContext(new Action(1, 2, 1, 1)).build();

    // act
    var actual = sut.evaluate();

    // assert
    assertThat(actual).isFalse();
  }

  private static Stream<Arguments> provideSwappingConstraintDefinitions() {
    return Stream.of(Arguments.of("ACTION a IS BLOCKED BY ACTION b IF a.edge OVERLAPS WITH b.edge"),
        Arguments.of("ACTION a IS BLOCKED BY ACTION b IF a.orig IS b.dest OR a.dest IS b.orig"));
  }
}
