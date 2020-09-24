package de.tum.in.www1.artemis.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.domain.ExampleSubmission;
import de.tum.in.www1.artemis.domain.Exercise;
import de.tum.in.www1.artemis.domain.ProgrammingExercise;
import de.tum.in.www1.artemis.domain.enumeration.TutorParticipationStatus;
import de.tum.in.www1.artemis.domain.participation.TutorParticipation;
import de.tum.in.www1.artemis.repository.ExampleSubmissionRepository;
import de.tum.in.www1.artemis.web.rest.dto.DueDateStat;

/**
 * Service Implementation for managing Tutor-Assessment-Dashboard.
 */
@Service
public class TutorDashboardService {

    private final Logger log = LoggerFactory.getLogger(ExamService.class);

    private final ExerciseService exerciseService;

    private final ProgrammingExerciseService programmingExerciseService;

    private final SubmissionService submissionService;

    private final ResultService resultService;

    private final ExampleSubmissionRepository exampleSubmissionRepository;

    public TutorDashboardService(ExerciseService exerciseService, ProgrammingExerciseService programmingExerciseService, SubmissionService submissionService,
            ResultService resultService, ExampleSubmissionRepository exampleSubmissionRepository) {
        this.exerciseService = exerciseService;
        this.programmingExerciseService = programmingExerciseService;
        this.submissionService = submissionService;
        this.resultService = resultService;
        this.exampleSubmissionRepository = exampleSubmissionRepository;
    }

    /**
     * Prepares the exercises for the tutor dashboard by setting the tutor participations and statistics
     *
     * @param exercises exercises to be prepared for the tutor dashboard
     * @param tutorParticipations participations of the tutors
     * @param examMode flag should be set for exam dashboard
     */
    public void prepareExercisesForTutorDashboard(Set<Exercise> exercises, List<TutorParticipation> tutorParticipations, boolean examMode) {
        for (Exercise exercise : exercises) {

            DueDateStat numberOfSubmissions;
            DueDateStat numberOfAssessments;

            if (exercise instanceof ProgrammingExercise) {
                numberOfSubmissions = new DueDateStat(programmingExerciseService.countSubmissionsByExerciseIdSubmitted(exercise.getId(), examMode), 0L);
                numberOfAssessments = new DueDateStat(programmingExerciseService.countAssessmentsByExerciseIdSubmitted(exercise.getId(), examMode), 0L);
            }
            else {
                numberOfSubmissions = submissionService.countSubmissionsForExercise(exercise.getId(), examMode);
                numberOfAssessments = resultService.countNumberOfFinishedAssessmentsForExercise(exercise.getId(), examMode);
            }

            exercise.setNumberOfSubmissions(numberOfSubmissions);
            exercise.setNumberOfAssessments(numberOfAssessments);

            exerciseService.calculateNrOfOpenComplaints(exercise, examMode);

            List<ExampleSubmission> exampleSubmissions = this.exampleSubmissionRepository.findAllByExerciseId(exercise.getId());
            // Do not provide example submissions without any assessment
            exampleSubmissions.removeIf(exampleSubmission -> exampleSubmission.getSubmission() == null || exampleSubmission.getSubmission().getResult() == null);
            exercise.setExampleSubmissions(new HashSet<>(exampleSubmissions));

            TutorParticipation tutorParticipation = tutorParticipations.stream().filter(participation -> participation.getAssessedExercise().getId().equals(exercise.getId()))
                    .findFirst().orElseGet(() -> {
                        TutorParticipation emptyTutorParticipation = new TutorParticipation();
                        emptyTutorParticipation.setStatus(TutorParticipationStatus.NOT_PARTICIPATED);
                        return emptyTutorParticipation;
                    });
            exercise.setTutorParticipations(Collections.singleton(tutorParticipation));
        }
    }
}
