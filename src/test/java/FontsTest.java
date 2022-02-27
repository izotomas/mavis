
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;

import dk.dtu.compute.mavis.Fonts;

public class FontsTest {

  @Test
  public void fontLoadsTest() {
    // act
    var fonts = Fonts.getDejaVuSansMono();

    // assert
    assertThat(fonts).isNotNull();
  }
}
