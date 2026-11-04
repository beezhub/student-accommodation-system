package com.beezhub.student_accommodation.service;

import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GH-450: nginx sets the first limit an upload meets, and with no {@code client_max_body_size} that
 * was its 1 MB default, so every larger upload failed on deployed servers only. The limit lives in
 * the nginx config, Spring's in application.yml and the per-image rule in Java, so these checks pin
 * the agreement. In this package because {@code MAX_IMAGES_PER_BUILDING} is package-private.
 */
class UploadSizeLimitsTest {

    private static final Path REPO_ROOT = Path.of("..");

    /** Default of {@code app.building-images.max-file-size-bytes} in ImageUploadValidator. */
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    private static String read(String path) throws IOException {
        return Files.readString(REPO_ROOT.resolve(path), StandardCharsets.UTF_8);
    }

    /** The {@code location /api/} block of the live server, which comes before the commented-out one. */
    private static String apiLocationBlock() throws IOException {
        String nginx = read("frontend/nginx.conf");
        int start = nginx.indexOf("location /api/ {");
        assertThat(start).as("nginx.conf must proxy /api/").isGreaterThanOrEqualTo(0);
        return nginx.substring(start, nginx.indexOf('}', start));
    }

    private static long nginxApiLimitBytes() throws IOException {
        Matcher limit = Pattern.compile("client_max_body_size\\s+(\\d+)m;").matcher(apiLocationBlock());
        assertThat(limit.find())
                .as("the /api/ location must set client_max_body_size, or nginx refuses bodies over 1 MB")
                .isTrue();
        return Long.parseLong(limit.group(1)) * 1024 * 1024;
    }

    private static DataSize springDefault(String envVar) throws IOException {
        Matcher value = Pattern.compile("\\$\\{" + envVar + ":(\\w+)}")
                .matcher(read("backend/src/main/resources/application.yml"));
        assertThat(value.find()).as(envVar + " in application.yml").isTrue();
        return DataSize.parse(value.group(1));
    }

    @Test
    void nginxLimit_isTheSameAsSpringRequestLimit() throws IOException {
        // Equal, so an oversized request gets nginx's 413 and never reaches Spring, and every
        // request nginx lets through is one Spring accepts too.
        assertThat(nginxApiLimitBytes()).isEqualTo(springDefault("MAX_REQUEST_SIZE").toBytes());
    }

    @Test
    void requestLimit_fitsAFullBatchOfBuildingImages() throws IOException {
        // The image dialog sends every selected file in one request.
        long fullBatch = BuildingImageService.MAX_IMAGES_PER_BUILDING * MAX_IMAGE_BYTES;

        assertThat(springDefault("MAX_REQUEST_SIZE").toBytes())
                .as("a full batch plus its multipart framing has to fit")
                .isGreaterThan(fullBatch);
    }

    @Test
    void perFileLimit_doesNotUndercutTheImageValidator() throws IOException {
        // A lower limit here would replace the validator's clear 422 with a bare 413.
        assertThat(springDefault("MAX_FILE_SIZE").toBytes()).isGreaterThanOrEqualTo(MAX_IMAGE_BYTES);
    }
}
