package dk.dtu.compute.cdl.model;

import static org.assertj.core.api.Assertions.*;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;
import org.javatuples.Pair;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import dk.dtu.compute.cdl.enums.OperandValueType;

public class OperatorTest {

  @ParameterizedTest
  @MethodSource("provideArgsForEvaluateTest")
  public void provideArgsForEvaluateTest(String operator, OperandValueType type, Object arg1,
      Object arg2, boolean expected)
      throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
    // arrange
    var sut = new Operator(operator, type);
    // var context

    // act
    var actual = sut.predicate.test(arg1, arg2);

    // assert
    assertThat(actual).isEqualTo(expected);
  }

  private static Stream<Arguments> provideArgsForEvaluateTest() {
    return Stream.of(
        Arguments.of("OVERLAPS WITH", OperandValueType.Vertex,
            new Pair<>(new Pair<>(1, 1), new Pair<>(1, 2)),
            new Pair<>(new Pair<>(1, 1), new Pair<>(1, 2)), true),
        Arguments.of("IS", OperandValueType.Number, (Integer) 4, (Integer) 5, false),
        Arguments.of("IS", OperandValueType.Number, (Integer) 8, (Integer) 8, true),
        Arguments.of("IS NOT", OperandValueType.Number, (Integer) 9, (Integer) 9, false),
        Arguments.of("IS LESS THAN", OperandValueType.Number, (Integer) 5, (Integer) 6, true),
        Arguments.of("IS NOT LESS THAN", OperandValueType.Number, (Integer) 5, (Integer) 6, false),
        Arguments.of("IS NOT MORE THAN", OperandValueType.Number, (Integer) 10, (Integer) 1, false),
        Arguments.of("IS MORE THAN", OperandValueType.Number, (Integer) 122, (Integer) 0, true),
        Arguments.of("IS", OperandValueType.String, "NoOp", "Push(E,E)", false),
        Arguments.of("IS", OperandValueType.String, "NoOp", "NoOp", true),
        Arguments.of("IS NOT", OperandValueType.String, "NoOp", "NoOp", false),
        Arguments.of("IS NOT", OperandValueType.String, "NoOp", "Move(E)", true));
  }
}
