package com.beezhub.student_accommodation.service.image;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * GH-219: building images were stored and served under whatever content type the uploader
 * declared, with nothing checking the bytes were an image at all.
 */
class ImageContentTypesTest {

  /** Takes ints so signature bytes can be written as literals without a cast on each one. */
  private static byte[] padded(int... prefix) {
    byte[] bytes = new byte[64];
    for (int i = 0; i < prefix.length; i++) {
      bytes[i] = (byte) prefix[i];
    }
    return bytes;
  }

  @Test
  void sniffsPng() {
    assertThat(ImageContentTypes.sniff(padded(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)))
        .isEqualTo("image/png");
  }

  @Test
  void sniffsJpeg() {
    assertThat(ImageContentTypes.sniff(padded(0xFF, 0xD8, 0xFF)))
        .isEqualTo("image/jpeg");
  }

  @Test
  void sniffsGif87aAndGif89a() {
    assertThat(ImageContentTypes.sniff(padded('G', 'I', 'F', '8', '7', 'a')))
        .isEqualTo("image/gif");
    assertThat(ImageContentTypes.sniff(padded('G', 'I', 'F', '8', '9', 'a')))
        .isEqualTo("image/gif");
  }

  /** WebP is a RIFF container: the form type sits at offset 8, past a four-byte chunk length. */
  @Test
  void sniffsWebpPastTheRiffChunkLength() {
    byte[] webp = padded('R', 'I', 'F', 'F', 0x24, 0x10, 0x00, 0x00);
    webp[8] = 'W';
    webp[9] = 'E';
    webp[10] = 'B';
    webp[11] = 'P';

    assertThat(ImageContentTypes.sniff(webp)).isEqualTo("image/webp");
  }

  @Test
  void riffThatIsNotWebpIsNotAnImage() {
    // A WAV file is also RIFF. Matching on RIFF alone would have accepted it.
    byte[] wav = padded('R', 'I', 'F', 'F', 0x24, 0x10, 0x00, 0x00);
    wav[8] = 'W';
    wav[9] = 'A';
    wav[10] = 'V';
    wav[11] = 'E';

    assertThat(ImageContentTypes.sniff(wav)).isNull();
  }

  @Test
  void htmlIsNotAnImage() {
    assertThat(ImageContentTypes.sniff("<html><script>alert(1)</script>".getBytes())).isNull();
  }

  @Test
  void aPdfIsNotABuildingImage() {
    // Accepted for student documents, never for a gallery.
    assertThat(ImageContentTypes.sniff(padded('%', 'P', 'D', 'F', '-'))).isNull();
  }

  @Test
  void emptyAndNullSniffToNothing() {
    assertThat(ImageContentTypes.sniff(null)).isNull();
    assertThat(ImageContentTypes.sniff(new byte[0])).isNull();
  }

  @Test
  void truncatedSignatureIsNotMatched() {
    assertThat(ImageContentTypes.sniff(new byte[] {(byte) 0xFF, (byte) 0xD8})).isNull();
  }

  @ParameterizedTest
  @CsvSource({
    "image/png, image/png",
    "image/jpeg, image/jpeg",
    "image/gif, image/gif",
    "image/webp, image/webp",
    "IMAGE/PNG, image/png",
    "'image/png; charset=binary', image/png",
  })
  void clampKeepsAnAllowedType(String stored, String expected) {
    assertThat(ImageContentTypes.clamp(stored)).isEqualTo(expected);
  }

  /** Not a registered type, but what several clients send and what existing rows carry. */
  @Test
  void clampNormalisesTheNonStandardImageJpg() {
    assertThat(ImageContentTypes.clamp("image/jpg")).isEqualTo("image/jpeg");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "text/html",
        "application/javascript",
        "application/pdf",
        "image/svg+xml",
        "not a media type at all",
        "image/png/../../text/html"
      })
  void clampDowngradesAnythingElse(String stored) {
    assertThat(ImageContentTypes.clamp(stored)).isEqualTo(ImageContentTypes.FALLBACK);
  }

  @Test
  void clampHandlesNull() {
    assertThat(ImageContentTypes.clamp(null)).isEqualTo(ImageContentTypes.FALLBACK);
  }
}
