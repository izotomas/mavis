package dk.dtu.compute.cld.model;

import static org.assertj.core.api.Assertions.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.stream.Stream;
import org.javatuples.Pair;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import dk.dtu.compute.cld.enums.OperandValueType;

public class ExpressionTest {

  private final static Operator IS_STR = new Operator("IS", OperandValueType.String);
  private final static Operator IS_NUM = new Operator("IS", OperandValueType.Number);
  private final static Operator IS_VTX = new Operator("IS", OperandValueType.Vertex);
  private final static Operator IS_LESS = new Operator("IS LESS THAN", OperandValueType.Number);
  private final static Operator IS_MORE = new Operator("IS MORE THAN", OperandValueType.Number);

  private final static Operand A_NAME = new Operand("a.name");
  private final static Operand A_AGENT = new Operand("a.agent");
  private final static Operand A_TIME = new Operand("a.time");
  private final static Operand A_DEST = new Operand("a.dest");
  private final static Operand B_DEST = new Operand("b.dest");

  private final static Operand NUM_0 = new Operand("0");
  private final static Operand NUM_1 = new Operand("1");
  private final static Operand NUM_2 = new Operand("2");
  private final static Operand LITERAL_PUSH = new Operand("'Push'");

  private final static ActionContext CTX_A_AGENT_0 =
      new ActionContext(new SimpleEntry<>("a", new Action(null, null, 0, null, 0)));
  private final static ActionContext CTX_A_TIME_1 =
      new ActionContext(new SimpleEntry<>("a", new Action(null, null, 1, null, 0)));
  private final static ActionContext CTX_A_PUSH =
      new ActionContext(new SimpleEntry<>("a", new Action(null, null, 0, "Push", 0)));
  private final static ActionContext CTX_A_NOOP =
      new ActionContext(new SimpleEntry<>("a", new Action(null, null, 0, "NoOp", 0)));

  private final static Action CTX_A_DEST_1_1 = new Action(null, new Pair<>(1, 1), 0, null, 0);
  private final static Action CTX_A_DEST_1_2 = new Action(null, new Pair<>(1, 2), 0, null, 0);

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
    return Stream.of(Arguments.of(A_AGENT, IS_NUM, NUM_0, CTX_A_AGENT_0, true),
        Arguments.of(A_AGENT, IS_NUM, NUM_1, CTX_A_AGENT_0, false),
        Arguments.of(A_NAME, IS_STR, LITERAL_PUSH, CTX_A_PUSH, true),
        Arguments.of(A_NAME, IS_STR, LITERAL_PUSH, CTX_A_NOOP, false),
        Arguments.of(A_TIME, IS_NUM, NUM_1, CTX_A_TIME_1, true),
        Arguments.of(A_TIME, IS_NUM, NUM_0, CTX_A_TIME_1, false),
        Arguments.of(A_DEST, IS_VTX, B_DEST,
            new ActionContext(new SimpleEntry<>("a", CTX_A_DEST_1_1),
                new SimpleEntry<>("b", CTX_A_DEST_1_1)),
            true),
        Arguments.of(A_DEST, IS_VTX, B_DEST,
            new ActionContext(new SimpleEntry<>("a", CTX_A_DEST_1_2),
                new SimpleEntry<>("b", CTX_A_DEST_1_1)),
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
    return Stream.of(Arguments.of(A_TIME, IS_LESS, NUM_1, CTX_A_TIME_1, false),
        Arguments.of(A_TIME, IS_LESS, NUM_2, CTX_A_TIME_1, true),
        Arguments.of(A_TIME, IS_MORE, NUM_0, CTX_A_TIME_1, true),
        Arguments.of(A_TIME, IS_MORE, NUM_1, CTX_A_TIME_1, false),
        Arguments.of(A_TIME, IS_MORE, NUM_2, CTX_A_TIME_1, false));
  }
}
