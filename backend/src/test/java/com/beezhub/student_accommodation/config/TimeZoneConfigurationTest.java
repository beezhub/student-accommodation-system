package com.beezhub.student_accommodation.config;

import com.beezhub.student_accommodation.service.ScheduledJobRunner;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GH-461: the backend ran in UTC and sends times with no offset, so a browser in South Africa read
 * every one of them 2 hours in the past. The zone is set in three files the code cannot see, so
 * these checks pin them.
 */
class TimeZoneConfigurationTest {

    private static final Path REPO_ROOT = Path.of("..");

    private static String read(String path) throws IOException {
        return Files.readString(REPO_ROOT.resolve(path), StandardCharsets.UTF_8);
    }

    @Test
    void compose_defaultsToSouthAfrica() throws IOException {
        assertThat(read("docker-compose.yml")).contains("TZ=${APP_TIMEZONE:-Africa/Johannesburg}");
    }

    @Test
    void dockerfile_defaultsToSouthAfrica() throws IOException {
        assertThat(read("backend/Dockerfile")).contains("ENV TZ=Africa/Johannesburg");
    }

    @Test
    void envExample_showsSouthAfrica() throws IOException {
        // A copy of this file is how a server's .env starts, and a stale UTC there beats the default.
        assertThat(read(".env.example")).contains("APP_TIMEZONE=Africa/Johannesburg");
    }

    @Test
    void timeZone_isOptionalForADeploy() throws IOException {
        // write-env.sh and deploy.yml stop a deploy on ${VAR} and ${VAR:?}, so a required key here
        // would block the next release until someone adds it to every server.
        assertThat(read("docker-compose.yml")).doesNotContain("${APP_TIMEZONE}").doesNotContain("${APP_TIMEZONE:?");
    }

    @Test
    void nightlyJobs_keepTheirRealTime() {
        // A cron with no zone follows the JVM. Moved to South African time, the 03:30 retention
        // sweep would land on the 01:30 UTC database backup, and the 03:00 orphan sweep on RDS's
        // Sunday maintenance window.
        List<Method> cronJobs = Arrays.stream(ScheduledJobRunner.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Scheduled.class))
                .filter(method -> !method.getAnnotation(Scheduled.class).cron().isEmpty())
                .toList();

        assertThat(cronJobs).isNotEmpty();
        assertThat(cronJobs).allSatisfy(method ->
                assertThat(method.getAnnotation(Scheduled.class).zone())
                        .as(method.getName())
                        .isEqualTo("UTC"));
    }
}
