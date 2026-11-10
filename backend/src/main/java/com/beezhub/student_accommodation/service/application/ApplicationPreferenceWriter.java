package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.aspect.LogActivity;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.ApplicationBuildingPreference;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.repository.ApplicationBuildingPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Saves one new application's building preferences, and is the point where its activity-log row is
 * written (GH-495).
 *
 * <p>A separate bean so {@code @LogActivity} goes through the proxy once per application: one
 * submission can create an application at each of several companies, and each gets its own row
 * with its own id. It joins the submission's transaction, so a submission that is rolled back
 * leaves no log row behind.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationPreferenceWriter {

    private final ApplicationBuildingPreferenceRepository buildingPreferenceRepository;

    /**
     * Saves {@code buildings} as the application's preferences in the order given, numbered from 1.
     * An empty list saves nothing, which is how an older client that sends only the single preferred
     * building is recorded.
     */
    @LogActivity(action = "CREATE", entityType = "APPLICATION", description = "Submitted an application",
            entityIdParamIndex = 0)
    public void recordSubmission(Long applicationId, Application application, List<Building> buildings) {
        if (buildings.isEmpty()) {
            return;
        }
        List<ApplicationBuildingPreference> preferences = new ArrayList<>();
        for (int i = 0; i < buildings.size(); i++) {
            preferences.add(ApplicationBuildingPreference.builder()
                    .application(application)
                    .building(buildings.get(i))
                    .preferenceOrder(i + 1)
                    .build());
        }
        buildingPreferenceRepository.saveAll(preferences);
        application.getBuildingPreferences().addAll(preferences);
        log.info("action=applicationPreferencesSaved applicationId={} count={}", applicationId, preferences.size());
    }
}
