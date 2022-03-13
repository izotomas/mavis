package dk.dtu.compute.cdl.model;

import static org.assertj.core.api.Assertions.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import dk.dtu.compute.cdl.enums.OperandValueType;

public class ExpressionTest {

  private final static Operator IS_STR = new Operator("IS", OperandValueType.String);
  private final static Operator IS_NUM = new Operator("IS", OperandValueType.Number);
  private final static Operator IS_LESS = new Operator("IS LESS THAN", OperandValueType.Number);
  private final static Operator IS_MORE = new Operator("IS MORE THAN", OperandValueType.Number);

  private final static Operand A_NAME = new Operand("a.name");
  private final static Operand A_TIME = new Operand("a.time");

  @ParameterizedTest
  @MethodSource("provideValidSingleExpressionArgs")
  public void singleExpressionTest(Operand op1, Operator operator, Operand op2,
      SimpleEntry<String, Action> ctx, boolean expected) {
    // arrange
    var sut = new Expression(op1, operator, op2);
    var context = new ActionContext().mapContext(ctx);

    // act
    var actual = sut.toPredicate().test(context);

    // assert
    assertThat(actual).isEqualTo(expected);
  }

  private static Stream<Arguments> provideValidSingleExpressionArgs() {
    return Stream.of(
        Arguments.of(A_NAME, IS_STR, new Operand("'Push'"),
            new SimpleEntry<>("a", new Action(null, null, 0, "Push")), true),
        Arguments.of(A_NAME, IS_STR, new Operand("'Push'"),
            new SimpleEntry<>("a", new Action(null, null, 0, "NoOp")), false),
        Arguments.of(A_TIME, IS_NUM, new Operand("1"),
            new SimpleEntry<>("a", new Action(null, null, 1, null)), true),
        Arguments.of(A_TIME, IS_LESS, new Operand("1"),
            new SimpleEntry<>("a", new Action(null, null, 0, null)), true),
        Arguments.of(A_TIME, IS_MORE, new Operand("1"),
            new SimpleEntry<>("a", new Action(null, null, 1, null)), false),
        Arguments.of(A_TIME, IS_MORE, new Operand("1"),
            new SimpleEntry<>("a", new Action(null, null, 2, null)), true));
  }
}
