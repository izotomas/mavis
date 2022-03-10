package dk.dtu.compute.cdl.validation;

import static org.assertj.core.api.Assertions.*;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class OperatorProviderTest {

  @ParameterizedTest
  @MethodSource("provideArgsForEvaluateTest")
  public void provideArgsForEvaluateTest(String operator, Object arg1, Object arg2,
      boolean expected)
      throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
    // arrange
    var sut = new OperatorProvider();
    // var method = sut.tryGet(operator, arg1, arg2);

    // act
    // var actual = method.invoke(sut, arg1, arg2);
    var actual = sut.evaluate(operator, arg1, arg2);

    // assert
    assertThat(actual).isEqualTo(expected);
  }

  private static Stream<Arguments> provideArgsForEvaluateTest() {
    return Stream.of(Arguments.of("IS", (Integer) 4, (Integer) 5, false));
  }
}
