package dk.dtu.compute.cdl.model;

import static org.assertj.core.api.Assertions.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.stream.Stream;
import org.javatuples.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import dk.dtu.compute.cdl.enums.OperandValueType;

public class ExpressionTest {

  private final static Operator IS_STR = new Operator("IS", OperandValueType.String);
  private final static Operator IS_NUM = new Operator("IS", OperandValueType.Number);
  private final static Operator IS_VTX = new Operator("IS", OperandValueType.Vertex);
  private final static Operator IS_LESS = new Operator("IS LESS THAN", OperandValueType.Number);
  private final static Operator IS_MORE = new Operator("IS MORE THAN", OperandValueType.Number);

  private final static Operand A_NAME = new Operand("a.name");
  private final static Operand A_TIME = new Operand("a.time");
  private final static Operand A_DEST = new Operand("a.dest");

  private final static ActionContext CTX_A_TIME_1 =
      new ActionContext(new SimpleEntry<>("a", new Action(null, null, 1, null)));
  private final static ActionContext CTX_A_PUSH =
      new ActionContext(new SimpleEntry<>("a", new Action(null, null, 0, "Push")));
  private final static ActionContext CTX_A_NOOP =
      new ActionContext(new SimpleEntry<>("a", new Action(null, null, 0, "NoOp")));

  @ParameterizedTest
  @MethodSource("provideValidSingleExpressionIsArgs")
  public void singleExpressionIsOperatorTest(Operand op1, Operator operator, Operand op2,
      ActionContext context, boolean expected) {
    // arrange
    var sut = new Expression(op1, operator, op2);

    // act
    var actual = sut.toPredicate().test(context);

    // assert
    assertThat(actual).isEqualTo(expected);
  }

  private static Stream<Arguments> provideValidSingleExpressionIsArgs() {
    return Stream.of(Arguments.of(A_NAME, IS_STR, new Operand("'Push'"), CTX_A_PUSH, true),
        Arguments.of(A_NAME, IS_STR, new Operand("'Push'"), CTX_A_NOOP, false),
        Arguments.of(A_TIME, IS_NUM, new Operand("1"), CTX_A_TIME_1, true),
        Arguments.of(A_TIME, IS_NUM, new Operand("0"), CTX_A_TIME_1, false),
        Arguments.of(A_DEST, IS_VTX, new Operand("b.dest"),
            new ActionContext(new SimpleEntry<>("a", new Action(null, new Pair<>(1, 1), 0, null)),
                new SimpleEntry<>("b", new Action(null, new Pair<>(1, 1), 0, null))),
            true),
        Arguments.of(A_DEST, IS_VTX, new Operand("b.dest"),
            new ActionContext(new SimpleEntry<>("a", new Action(null, new Pair<>(1, 2), 0, null)),
                new SimpleEntry<>("b", new Action(null, new Pair<>(1, 1), 0, null))),
            false));
  }

  @ParameterizedTest
  @MethodSource("provideValidSingleExpressionLessMoreArgs")
  public void singleExpressionLessMoreOperatorTest(Operand op1, Operator operator, Operand op2,
      ActionContext context, boolean expected) {
    // arrange
    var sut = new Expression(op1, operator, op2);

    // act
    var actual = sut.toPredicate().test(context);

    // assert
    assertThat(actual).isEqualTo(expected);
  }


  private static Stream<Arguments> provideValidSingleExpressionLessMoreArgs() {
    return Stream.of(Arguments.of(A_TIME, IS_LESS, new Operand("1"), CTX_A_TIME_1, false),
        Arguments.of(A_TIME, IS_LESS, new Operand("1"), CTX_A_TIME_1, false),
        Arguments.of(A_TIME, IS_LESS, new Operand("2"), CTX_A_TIME_1, true),
        Arguments.of(A_TIME, IS_MORE, new Operand("0"), CTX_A_TIME_1, true),
        Arguments.of(A_TIME, IS_MORE, new Operand("1"), CTX_A_TIME_1, false),
        Arguments.of(A_TIME, IS_MORE, new Operand("2"), CTX_A_TIME_1, false));
  }
}
