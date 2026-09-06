package com.beezhub.student_accommodation.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * The Java version is declared in three places that have no way of checking each other: the Gradle
 * toolchain, the JDK the CI workflow installs, and the Temurin tags the Dockerfile builds and runs
 * on. They had already drifted — a 21 toolchain against a CI job on 17 — which meant CI was
 * compiling with a different compiler from every other build (GH-215).
 *
 * <p>This test reads all three and asserts they name the same feature release, so the next drift
 * fails the build instead of being noticed later.
 *
 * <p>Paths are resolved relative to the Gradle project directory, which is {@code backend/} when
 * the test task runs.
 */
class ToolchainVersionConsistencyTest {

  private static final Path PROJECT_DIR = Path.of("").toAbsolutePath();
  private static final Path REPO_ROOT = PROJECT_DIR.getParent();

  private static final Pattern TOOLCHAIN_VERSION =
      Pattern.compile("JavaLanguageVersion\\.of\\((\\d+)\\)");
  private static final Pattern CI_JAVA_VERSION =
      Pattern.compile("java-version:\\s*'?\"?(\\d+)'?\"?");
  private static final Pattern TEMURIN_TAG = Pattern.compile("FROM\\s+eclipse-temurin:(\\d+)-");

  @Test
  void gradleToolchainCiAndDockerfileAgreeOnTheJavaVersion() throws IOException {
    int toolchainVersion = singleMatch(read(PROJECT_DIR.resolve("build.gradle")), TOOLCHAIN_VERSION);

    List<Integer> ciVersions =
        allMatches(read(REPO_ROOT.resolve(".github/workflows/ci.yml")), CI_JAVA_VERSION);
    assertThat(ciVersions)
        .as("ci.yml should install a JDK for the backend job")
        .isNotEmpty()
        .allSatisfy(
            version ->
                assertThat(version)
                    .as(
                        "ci.yml installs JDK %d but build.gradle targets %d",
                        version, toolchainVersion)
                    .isEqualTo(toolchainVersion));

    List<Integer> dockerVersions = allMatches(read(PROJECT_DIR.resolve("Dockerfile")), TEMURIN_TAG);
    assertThat(dockerVersions)
        .as("Dockerfile should pin a Temurin base image for both stages")
        .hasSizeGreaterThanOrEqualTo(2)
        .allSatisfy(
            version ->
                assertThat(version)
                    .as(
                        "Dockerfile uses eclipse-temurin:%d but build.gradle targets %d",
                        version, toolchainVersion)
                    .isEqualTo(toolchainVersion));
  }

  /**
   * Preview features are version-locked: a class compiled with {@code --enable-preview} on one
   * release refuses to load on any other, so the flag turns a JDK upgrade into a rebuild-everything
   * event. Nothing here uses a preview feature, so the flag stays out.
   */
  @Test
  void theBuildDoesNotEnablePreviewFeatures() throws IOException {
    assertThat(read(PROJECT_DIR.resolve("build.gradle")))
        .as("--enable-preview locks compiled classes to one JDK release (GH-215)")
        .doesNotContain("--enable-preview");
  }

  private static String read(Path path) throws IOException {
    assertThat(path).as("expected to find %s", path).exists();
    return Files.readString(path);
  }

  private static int singleMatch(String content, Pattern pattern) {
    List<Integer> matches = allMatches(content, pattern);
    assertThat(matches).as("expected exactly one match for %s", pattern).hasSize(1);
    return matches.get(0);
  }

  private static List<Integer> allMatches(String content, Pattern pattern) {
    List<Integer> versions = new ArrayList<>();
    Matcher matcher = pattern.matcher(content);
    while (matcher.find()) {
      versions.add(Integer.parseInt(matcher.group(1)));
    }
    return versions;
  }
}
