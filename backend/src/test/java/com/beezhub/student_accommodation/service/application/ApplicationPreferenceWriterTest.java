package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.ApplicationBuildingPreference;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.repository.ApplicationBuildingPreferenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApplicationPreferenceWriterTest {

    @Mock private ApplicationBuildingPreferenceRepository repository;
    @InjectMocks private ApplicationPreferenceWriter writer;

    /** GH-495: each application's preferences are numbered from 1 in the student's order. */
    @Test
    void recordSubmission_numbersThePreferencesFromOneInOrder() {
        Application application = Application.builder().id(5L).build();
        Building first = new Building();
        first.setId(12L);
        Building second = new Building();
        second.setId(10L);

        writer.recordSubmission(5L, application, List.of(first, second));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ApplicationBuildingPreference>> saved = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(ApplicationBuildingPreference::getPreferenceOrder).containsExactly(1, 2);
        assertThat(saved.getValue()).extracting(ApplicationBuildingPreference::getBuilding).containsExactly(first, second);
        assertThat(application.getBuildingPreferences()).hasSize(2);
    }

    @Test
    void recordSubmission_noBuildings_savesNothing() {
        writer.recordSubmission(5L, Application.builder().id(5L).build(), List.of());

        verify(repository, never()).saveAll(any());
    }
}
