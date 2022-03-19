import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import sallat.parser.*;



public class ParserTest {


  @Test
  public void parserTest() {
    // arrange
    var precdicate = ParserTest.shouldNotFail();
    var cat = new Cat(true, false, Color.BLUE, "fero");

    // act
    var actual = precdicate.test(cat);

    // assert
    assertThat(actual).isTrue();
  }

  public class Cat {
    boolean isFluffy;
    boolean isHungry;
    Color color;
    String name;

    public Cat() {}

    public Cat(boolean isFluffy, boolean isHungry, Color color, String name) {
      this.isFluffy = isFluffy;
      this.isHungry = isHungry;
      this.color = color;
      this.name = name;

    }

    boolean isFluffy() {
      return this.isFluffy;
    }

    boolean isHungry() {
      return this.isHungry;
    }

    Color getColor() {
      return this.color;
    }

    boolean equals(String name) {
      return this.name.equals(name);
    }
  }

  public static Predicate<Cat> shouldNotFail() {
    Map<String, Operators> operatorsMap = new HashMap<>();

    operatorsMap.put("not", Operators.NOT);
    operatorsMap.put("and", Operators.AND);
    operatorsMap.put("or", Operators.OR);
    operatorsMap.put("xor", Operators.XOR);

    // define mappings for the elementary predicates

    Map<String, Predicate<Cat>> predicateMap = new HashMap<>();

    predicateMap.put("fluffy", Cat::isFluffy);
    predicateMap.put("black", cat -> cat.getColor() == Color.BLACK);
    predicateMap.put("white", cat -> cat.getColor() == Color.WHITE);
    predicateMap.put("hungry", Cat::isHungry);
    predicateMap.put("tom", cat -> cat.equals("tom"));

    // Build a PredicateParser instance

    PredicateParser<Cat> parser =
        SimplePredicateParser.<Cat>builder().setCasePolicy(CasePolicy.TO_LOWER_CASE)
            .setOperatorMap(operatorsMap).setPredicateMap(predicateMap).build();

    // Use the parser to create predicates

    var predicate = parser.parse("(fluffy or hungry) and not (black or white or tom)");
    return predicate;
  }
}
