import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import dk.dtu.compute.cdl.Parser;
import dk.dtu.compute.cdl.Cat;

/* #region Import test dependencies */

import sallat.parser.*;
import java.util.function.*;
import java.util.regex.Pattern;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.beanutils.BeanUtils;
import org.javatuples.*;
import java.lang.reflect.InvocationTargetException;

/* #endregion */


public class ParserTest {

  @Test
  public void parserTest() {
    // arrange
    var precdicate = Parser.shouldNotFail();
    var cat = new Cat(true, false, Color.BLUE, "fero");

    // act
    var actual = precdicate.test(cat);

    // assert
    assertThat(actual).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"ACTION a BLOCKED BY ACTION b if a.dest is b.dest"})
  public void vertexConflictTest(String input) {
    // arrange
    // a.dest is b.dest => a.dest == b.dest ==> a.dest.is(b.dest)

    var isOperator = Pattern.compile("a.dest is b.dest");
    var statement = "a.dest is b.dest";
    var statementParser = Pattern.compile("(?<caller>)");



    var a = new Action(new Vertex(new Pair<Integer, Integer>(1, 1)));
    var b = new Action(new Vertex(new Pair<Integer, Integer>(1, 1)));

    var context = new HashMap<String, Action>();
    context.put("a", a);
    context.put("b", b);



    var actual = is(a.dest.coordinates, b.dest.coordinates);

    // assert
    assertThat(actual).isTrue();

    Pattern simpleUrlPattern = Pattern.compile("[^:]+://(?:[.a-z]+/?)+");
    var urlMatcher = simpleUrlPattern.matcher("http://www.microsoft.com/some/other/url/path");

    assertThat(urlMatcher.matches()).isTrue();
  }


  public static boolean is(Pair<Integer, Integer> vertex, Pair<Integer, Integer> other) {
    return ((vertex == null) && (other == null)) || vertex.compareTo(other) == 0;
  }


  public class Action {
    public final Vertex dest;

    public Action(Vertex dest) {
      this.dest = dest;
    }

    public Object get(String key) {
      switch (key) {
        case "dest":
          return this.dest;
        default:
          throw new IllegalArgumentException();
      }
    }
  }

  public class Vertex {
    public final Pair<Integer, Integer> coordinates;

    public Vertex(Pair<Integer, Integer> coordinates) {
      this.coordinates = coordinates;
    }

    public boolean is(Pair<Integer, Integer> other) {
      return coordinates.compareTo(other) == 0;
    }
  }

  @Test
  public void regexTest() {
    String regex = "(?<globalCode>[\\d]{2})-(?<nationalCode>[\\d]{5})-(?<number>[\\d]{6})";
    Pattern pattern = Pattern.compile(regex);
    // Matching the compiled pattern in the String
    var matcher = pattern.matcher("91-08955-224558");
    while (matcher.find()) {
      System.out.println("Global code: " + matcher.group("globalCode"));
      System.out.println("National code: " + matcher.group("nationalCode"));
      System.out.println("Phone number: " + matcher.group("number"));
    }
  }

  @Test
  public void regexTwoTest()
      throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    // arrange
    String regex =
        "(?<caller>[a-zA-Z]+)\\.(?<callerprop>dest)\\s(?<operation>[a-zA-Z]+)\\s(?<arg>[a-zA-Z]+)\\.(?<argprop>[a-zA-Z]+)";
    var input = "a.dest is b.dest";
    var matcher = Pattern.compile(regex).matcher(input);


    matcher.find();

    var caller = matcher.group("caller");
    var callerprop = matcher.group("callerprop");
    var operation = matcher.group("operation");
    var arg = matcher.group("arg");
    var argprop = matcher.group("argprop");


    var a = new Action(new Vertex(new Pair<Integer, Integer>(1, 1)));
    var b = new Action(new Vertex(new Pair<Integer, Integer>(1, 1)));

    var context = new HashMap<String, Action>();
    context.put("a", a);
    context.put("b", b);

    // act
    var actual = context.get(caller);
    var prop = actual.get(callerprop);
    var other = context.get(arg);
    var otherProp = other.get(argprop);

    var actuall = a.dest.is(((Vertex) otherProp).coordinates);
    var m = prop.getClass().getMethods()[0];
    // var m = prop.getClass().getMethod(operation, otherProp.getClass());
    var result = m.invoke(prop, ((Vertex) otherProp).coordinates);

    // assert
    assertThat(actuall).isEqualTo(result);
  }

  @ParameterizedTest
  @ValueSource(strings = {"ACTION a IS BLOCKED BY ACTION b", "ACTION myaction IS BLOCKED"})
  public void contextRegexTest(String input) {
    // arrange
    var regex =
        "^ACTION\\s(?<entry1>[a-z]+)\\sIS\\sBLOCKED(\\s(?<BY>BY)\\sACTION\\s(?<entry2>[a-z]+))?$";

    var pattern = Pattern.compile(regex);

    // act
    var matcher = pattern.matcher(input);
    matcher.find();

    var entry1 = matcher.group("entry1");
    var entry2 = matcher.group("entry2");
    var entry2Required = matcher.group("BY") != null;

    // assert
    assertThat(entry1).isNotBlank();
    assertThat(!entry2Required || entry2 != null).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"ACTION a IS BLOCKED BY ACTION b IF a.dest IS b.dest",
      "ACTION a IS BLOCKED IF a.name IS NOT 'NoOp' AND a.time IS LESS THAN 5"})
  public void splitTest(String statement) {
    var tokens = statement.split("\\sIF\\s");

    assertThat(tokens.length).isEqualTo(2);
  }

  @ParameterizedTest
  @ValueSource(strings = {"ACTION a IS BLOCKED", "ACTION a IS BLOCKED BY ACTION b"})
  public void contextTest(String contextStatement) {
    // arrange
    var regex =
        "^ACTION\\h(?<entry1>[a-z]+)\\hIS\\hBLOCKED(?:\\hBY\\hACTION\\h(?<entry2>[a-z]+))?$";
    var match = Pattern.compile(regex).matcher(contextStatement);

    var result = new ArrayList<String>();
    // act
    if (match.matches()) {
      for (var group : new String[] {"entry1", "entry2"}) {
        var value = match.group(group);
        result.add(value);
      }
    }

    // assert
    assertThat(result.isEmpty()).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      // "a.dest IS b.dest",
      // "a.name IS NOT 'NoOp' AND a.time IS LESS THAN 5",
      // "a.name IS NOT 'NoOp' AND NOT a.time IS MORE THAN 5",
      "a.name IS NOT 'NoOp' a.time IS MORE THAN 5",})
  public void predicateParseTest(String predicate) {
    var regex =
        "(?<operand1>[a-z]+\\.[a-z]+|\\'[\\w]+\\'|[0-9]+)\\s(?<operator>[A-Z]+(?:\\s[A-Z]+)*)"
            + "\\s(?<operand2>\\'\\w*\\'|[0-9]+|[a-z]+\\.[a-z]+)(\\s(?<connector>(?:AND|OR)(?:\\sNOT)?))*";

    var match = Pattern.compile(regex).matcher(predicate);
    var result = new ArrayList<String>();
    var conditionRequired = true;
    var parsingError = false;
    while (match.find()) {
      //
      if (!conditionRequired) {
        parsingError = true;
        break;
      }
      // current predicate
      for (var groupName : new String[] {"operand1", "operator", "operand2"}) {
        var value = match.group(groupName);
        if (value == null) {
          parsingError = true;
          break;
        }
        result.add(value);
      }
      // connector to next predicate
      var connector = match.group("connector");
      if (conditionRequired = connector != null) {
        result.add(connector);
      }
    }

    assertThat(parsingError).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      // "a.dest IS b.dest",
      // "a.name IS NOT 'NoOp' AND a.time IS LESS THAN 5",
      // "a.name IS NOT 'NoOp' AND NOT a.time IS MORE THAN 5",
      "a.name IS NOT 'NoOp' AND a.time IS MORE THAN 5",})
  public void predicateParseNextTest(String predicate) {
    // arrange
    var regex =
        // predicate connector
        "(?:\\h(?<connector>(?:AND|OR)(?:\\hNOT)?)\\h|^)"
            // predicate arguments and operations
            + "(?<operand1>[a-z]+\\.[a-z]+|\\'[\\w().,*]+\\'|[0-9]+)"
            + "\\h(?<operator>[A-Z]+(?:\\h[A-Z]+)*)\\s"
            + "(?<operand2>[a-z]+\\.[a-z]+|\\'[\\w().,*]+\\'|[0-9]+)";

    var match = Pattern.compile(regex).matcher(predicate);
    var result = new ArrayList<String>();

    // act
    var i = 0;
    while (match.find()) {
      // look at connector
      if (i > 0) {
        var connector = match.group("connector");
        if (connector == null) {
          throw new IllegalArgumentException();
        }
        result.add(connector);
      }

      // current predicate
      for (var groupName : new String[] {"operand1", "operator", "operand2"}) {
        var value = match.group(groupName);
        result.add(value);
      }
      i++;
    }

    // assert
    assertThat(result.size() > 1).isTrue();
  }
}
