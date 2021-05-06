package de.tum.in.www1.artemis.web.rest;

import static de.tum.in.www1.artemis.web.rest.util.ResponseUtil.*;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import de.tum.in.www1.artemis.domain.ProgrammingExercise;
import de.tum.in.www1.artemis.domain.StaticCodeAnalysisCategory;
import de.tum.in.www1.artemis.repository.ProgrammingExerciseRepository;
import de.tum.in.www1.artemis.service.AuthorizationCheckService;
import de.tum.in.www1.artemis.service.StaticCodeAnalysisService;

/**
 * REST controller for managing static code analysis.
 * Static code analysis categories are created automatically when the programming exercise with static code analysis is
 * created, therefore a POST mapping is missing. A DELETE mapping is also not necessary as those categories can only be
 * deactivated but not deleted.
 */
@RestController
@RequestMapping("/api")
public class StaticCodeAnalysisResource {

    private static final String ENTITY_NAME = "StaticCodeAnalysisCategory";

    private final Logger log = LoggerFactory.getLogger(StaticCodeAnalysisResource.class);

    private final AuthorizationCheckService authCheckService;

    private final ProgrammingExerciseRepository programmingExerciseRepository;

    private final StaticCodeAnalysisService staticCodeAnalysisService;

    public StaticCodeAnalysisResource(AuthorizationCheckService authCheckService, ProgrammingExerciseRepository programmingExerciseRepository,
            StaticCodeAnalysisService staticCodeAnalysisService) {
        this.authCheckService = authCheckService;
        this.programmingExerciseRepository = programmingExerciseRepository;
        this.staticCodeAnalysisService = staticCodeAnalysisService;
    }

    /**
     * Get the static code analysis categories for a given exercise id.
     *
     * @param exerciseId of the the exercise
     * @return the static code analysis categories
     */
    @GetMapping(Endpoints.CATEGORIES)
    @PreAuthorize("hasRole('TA')")
    public ResponseEntity<Set<StaticCodeAnalysisCategory>> getStaticCodeAnalysisCategories(@PathVariable Long exerciseId) {
        log.debug("REST request to get static code analysis categories for programming exercise {}", exerciseId);

        ProgrammingExercise programmingExercise = programmingExerciseRepository.findByIdElseThrow(exerciseId);

        if (!Boolean.TRUE.equals(programmingExercise.isStaticCodeAnalysisEnabled())) {
            return badRequest();
        }

        if (!authCheckService.isAtLeastTeachingAssistantForExercise(programmingExercise)) {
            return forbidden();
        }

        Set<StaticCodeAnalysisCategory> staticCodeAnalysisCategories = staticCodeAnalysisService.findByExerciseId(exerciseId);
        return ResponseEntity.ok(staticCodeAnalysisCategories);
    }

    /**
     * Updates the static code analysis categories of a given programming exercise using the data in the request body.
     *
     * @param exerciseId of the the exercise
     * @param categories used for the update
     * @return the updated static code analysis categories
     */
    @PatchMapping(Endpoints.CATEGORIES)
    @PreAuthorize("hasRole('EDITOR')")
    public ResponseEntity<Set<StaticCodeAnalysisCategory>> updateStaticCodeAnalysisCategories(@PathVariable Long exerciseId,
            @RequestBody Set<StaticCodeAnalysisCategory> categories) {
        log.debug("REST request to update static code analysis categories for programming exercise {}", exerciseId);

        ProgrammingExercise programmingExercise = programmingExerciseRepository.findByIdElseThrow(exerciseId);

        if (!Boolean.TRUE.equals(programmingExercise.isStaticCodeAnalysisEnabled())) {
            return badRequest();
        }

        if (!authCheckService.isAtLeastEditorForExercise(programmingExercise)) {
            return forbidden();
        }

        var optionalError = validateCategories(categories, exerciseId);
        if (optionalError.isPresent()) {
            return optionalError.get();
        }

        Set<StaticCodeAnalysisCategory> staticCodeAnalysisCategories = staticCodeAnalysisService.updateCategories(exerciseId, categories);
        return ResponseEntity.ok(staticCodeAnalysisCategories);
    }

    /**
     * Reset the static code analysis categories of the given exercise to their default configuration.
     *
     * @param exerciseId if of the exercise for which the categories should be reseted
     * @return static code analysis categories with the default configuration
     */
    @PatchMapping(Endpoints.RESET)
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<Set<StaticCodeAnalysisCategory>> resetStaticCodeAnalysisCategories(@PathVariable Long exerciseId) {
        log.debug("REST request to reset static code analysis categories for programming exercise {}", exerciseId);

        ProgrammingExercise programmingExercise = programmingExerciseRepository.findByIdElseThrow(exerciseId);

        if (!Boolean.TRUE.equals(programmingExercise.isStaticCodeAnalysisEnabled())) {
            return badRequest();
        }

        if (!authCheckService.isAtLeastInstructorForExercise(programmingExercise)) {
            return forbidden();
        }

        Set<StaticCodeAnalysisCategory> staticCodeAnalysisCategories = staticCodeAnalysisService.resetCategories(programmingExercise);
        return ResponseEntity.ok(staticCodeAnalysisCategories);
    }

    /**
     * Validates static code analysis categories
     *
     * @param categories to be validated
     * @param exerciseId path variable
     * @return empty optional if no error was found otherwise optional with an error response
     */
    private Optional<ResponseEntity<Set<StaticCodeAnalysisCategory>>> validateCategories(Set<StaticCodeAnalysisCategory> categories, Long exerciseId) {
        for (var category : categories) {
            // Each categories must have an id
            if (category.getId() == null) {
                return Optional.of(badRequest(ENTITY_NAME, "scaCategoryIdError", "Static code analysis category id is missing."));
            }

            // Penalty must not be null or negative
            if (category.getPenalty() == null || category.getPenalty() < 0) {
                return Optional.of(badRequest(ENTITY_NAME + " " + category.getId(), "scaCategoryPenaltyError",
                        "Penalty for static code analysis category " + category.getId() + " must be a non-negative integer."));
            }

            // MaxPenalty must not be smaller than penalty
            if (category.getMaxPenalty() != null && category.getPenalty() > category.getMaxPenalty()) {
                return Optional.of(badRequest(ENTITY_NAME + " " + category.getId(), "scaCategoryMaxPenaltyError",
                        "Max Penalty for static code analysis category " + category.getId() + " must not be smaller than the penalty."));
            }

            // Category state must not be null
            if (category.getState() == null) {
                return Optional.of(badRequest(ENTITY_NAME + " " + category.getId(), "scaCategoryStateError",
                        "Max Penalty for static code analysis category " + category.getId() + " must not be smaller than the penalty."));
            }

            // Exercise id of the request path must match the exerciseId in the request body if present
            if (category.getExercise() != null && !Objects.equals(category.getExercise().getId(), exerciseId)) {
                return Optional.of(conflict(ENTITY_NAME + " " + category.getId(), "scaCategoryExerciseIdError",
                        "Exercise id path variable does not match exercise id of static code analysis category " + category.getId()));
            }
        }
        return Optional.empty();
    }

    public static final class Endpoints {

        private static final String PROGRAMMING_EXERCISE = "/programming-exercise/{exerciseId}";

        public static final String CATEGORIES = PROGRAMMING_EXERCISE + "/static-code-analysis-categories";

        public static final String RESET = PROGRAMMING_EXERCISE + "/static-code-analysis-categories/reset";

        private Endpoints() {
        }
    }
}
