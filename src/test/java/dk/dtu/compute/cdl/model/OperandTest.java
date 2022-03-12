package dk.dtu.compute.cdl.model;

import static org.assertj.core.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import dk.dtu.compute.cdl.enums.OperandType;

public class OperandTest {

  @ParameterizedTest
  @MethodSource("provideArgsForGetTypeTest")
  public void getTypeTest(String valueString, Object expectedVal, OperandType expectedType,
      OperandValueType expectedValueType) {
    // arrange
    var sut = new Operand(valueString);

    // act
    var actualValueType = sut.valueType;
    var actualType = sut.type;
    var actualVal = sut.value;

    // assert
    assertThat(actualValueType).isEqualTo(expectedValueType);
    assertThat(actualVal).isEqualTo(expectedVal);
    assertThat(actualType).isEqualTo(expectedType);
  }

  private static Stream<Arguments> provideArgsForGetTypeTest() {
    return Stream.of(
        Arguments.of("'Some string'", "Some string", OperandType.Literal, OperandValueType.String),
        Arguments.of("b.name", "b.name", OperandType.ActionReference, OperandValueType.String),
        Arguments.of("b.time", "b.time", OperandType.ActionReference, OperandValueType.Number),
        Arguments.of("a.orig", "a.orig", OperandType.ActionReference, OperandValueType.Vertex),
        Arguments.of("14", (Integer) 14, OperandType.Literal, OperandValueType.Number), Arguments
            .of("action.edge", "action.edge", OperandType.ActionReference, OperandValueType.Edge));
  }
}
