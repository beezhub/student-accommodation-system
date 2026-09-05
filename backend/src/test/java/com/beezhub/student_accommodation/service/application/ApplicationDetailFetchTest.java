package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.repository.ApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GH-130: the application detail endpoints had no transaction, {@code open-in-view} is disabled,
 * and {@code ApplicationMapper} dereferences {@code student.appUser}, {@code preferredBuilding} and
 * each preference's building, all LAZY. Those reads happened after the repository transaction had
 * closed, so both endpoints threw {@code LazyInitializationException}.
 *
 * <p>Only mock-based tests covered them, which is why this shipped: a Mockito repository returns a
 * fully populated object graph, so the mapper never touches a proxy and the test passes no matter
 * what the fetch plan is. These checks pin the two things that actually prevent the failure.
 */
class ApplicationDetailFetchTest {

    /** Everything ApplicationMapper.toResponse dereferences. */
    private static final List<String> REQUIRED_PATHS = List.of(
            "student", "student.appUser", "preferredBuilding",
            "buildingPreferences", "buildingPreferences.building");

    private static Method readMethod(String name, Class<?>... params) throws NoSuchMethodException {
        return ApplicationCoreService.class.getMethod(name, params);
    }

    @ParameterizedTest
    @ValueSource(strings = {"getApplicationById", "getApplicationsByStudentId"})
    void bothDetailReadsRunInsideAReadOnlyTransaction(String methodName) throws NoSuchMethodException {
        Transactional transactional = readMethod(methodName, Long.class).getAnnotation(Transactional.class);

        assertThat(transactional)
                .as(methodName + " must hold the session open while the mapper reads lazy associations")
                .isNotNull();
        assertThat(transactional.readOnly()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"findWithDetailsById", "findWithDetailsByStudent_Id"})
    void theFetchingQueriesLoadEveryAssociationTheMapperTouches(String methodName) throws NoSuchMethodException {
        EntityGraph graph = ApplicationRepository.class.getMethod(methodName, Long.class)
                .getAnnotation(EntityGraph.class);

        assertThat(graph).as(methodName + " must declare an entity graph").isNotNull();
        assertThat(Arrays.asList(graph.attributePaths()))
                .as("a path the mapper reads is missing, so it would trigger a lazy load or an N+1")
                .containsAll(REQUIRED_PATHS);
    }

    @Test
    void theFetchingQueriesReturnTheExpectedShapes() throws NoSuchMethodException {
        assertThat(ApplicationRepository.class.getMethod("findWithDetailsById", Long.class).getReturnType())
                .isEqualTo(java.util.Optional.class);
        assertThat(ApplicationRepository.class.getMethod("findWithDetailsByStudent_Id", Long.class).getReturnType())
                .isEqualTo(List.class);
    }
}
