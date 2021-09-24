
// import static org.assertj.core.api.Assertions.*;
import org.junit.Test;

import dk.dtu.compute.mavis.Fonts;

public class FontsTest {

  @Test
  public void fontLoadsTest() {
    Fonts.getDejaVuSansMono();
    // assertThat(Fonts.getDejaVuSansMono()).isNotNull();
  }
}
