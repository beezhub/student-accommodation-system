package com.beezhub.student_accommodation.service.notification;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins agreement between the links emails are built with and the frontend route table. An email
 * link to a path with no route falls through the wildcard to /home, so the student lands on the
 * marketing page with no error anywhere (GH-456).
 */
class EmailLinkRoutesTest {

    private static final Path REPO_ROOT = Path.of("..");
    private static final Path DISPATCH_SOURCE = REPO_ROOT.resolve(
            "backend/src/main/java/com/beezhub/student_accommodation/service/notification/EmailDispatchService.java");
    private static final Path ROUTES_SOURCE = REPO_ROOT.resolve("frontend/src/app/app-routing.module.ts");

    /** Top-level routes sit at four spaces in the table; admin children are indented further. */
    private static final Pattern TOP_LEVEL_ROUTE = Pattern.compile("(?m)^    path: '([^']+)'");
    private static final Pattern LINK_LITERAL = Pattern.compile("baseUrl \\+ \"(/[^\"?]*)");

    @Test
    void emailLinks_everyLink_matchesARouteTheFrontendHas() throws IOException {
        List<Pattern> routes = routePatterns();
        List<String> links = emailLinks();
        assertThat(routes).as("route table parsed").isNotEmpty();
        assertThat(links).as("email links parsed").isNotEmpty();

        assertThat(links)
                .as("a link with no route redirects the student to /home")
                .allSatisfy(link -> assertThat(routes)
                        .anySatisfy(route -> assertThat(route.matcher(link).matches())
                                .as("%s against %s", link, route)
                                .isTrue()));
    }

    @Test
    void routePatterns_unknownAndParameterizedPaths_areToldApart() throws IOException {
        List<Pattern> routes = routePatterns();

        assertThat(routes).noneMatch(route -> route.matcher("/portal/applications").matches());
        assertThat(routes).anyMatch(route -> route.matcher("/applications/42").matches());
    }

    private static List<Pattern> routePatterns() throws IOException {
        Matcher matcher = TOP_LEVEL_ROUTE.matcher(Files.readString(ROUTES_SOURCE, StandardCharsets.UTF_8));
        List<Pattern> routes = new ArrayList<>();
        while (matcher.find()) {
            routes.add(Pattern.compile("/" + matcher.group(1).replaceAll(":[^/]+", "[^/]+")));
        }
        return routes;
    }

    /** A literal ending in a slash has an id appended at runtime, so a stand-in id is added here. */
    private static List<String> emailLinks() throws IOException {
        Matcher matcher = LINK_LITERAL.matcher(Files.readString(DISPATCH_SOURCE, StandardCharsets.UTF_8));
        List<String> links = new ArrayList<>();
        while (matcher.find()) {
            String link = matcher.group(1);
            links.add(link.endsWith("/") ? link + "1" : link);
        }
        return links;
    }
}
