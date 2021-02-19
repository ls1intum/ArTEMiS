package de.tum.in.www1.artemis.service;

import java.util.Collections;
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
import de.tum.in.www1.artemis.repository.ProgrammingExerciseRepository;
import de.tum.in.www1.artemis.repository.ResultRepository;
import de.tum.in.www1.artemis.repository.SubmissionRepository;
import de.tum.in.www1.artemis.service.util.TimeLogUtil;
import de.tum.in.www1.artemis.web.rest.dto.DueDateStat;

/**
 * Service Implementation for managing Tutor-Assessment-Dashboard.
 */
@Service
public class AssessmentDashboardService {

    private final Logger log = LoggerFactory.getLogger(AssessmentDashboardService.class);

    private final ComplaintService complaintService;

    private final ProgrammingExerciseRepository programmingExerciseRepository;

    private final SubmissionRepository submissionRepository;

    private final ResultRepository resultRepository;

    private final ExampleSubmissionRepository exampleSubmissionRepository;

    public AssessmentDashboardService(ComplaintService complaintService, ProgrammingExerciseRepository programmingExerciseRepository, SubmissionRepository submissionRepository,
            ResultRepository resultRepository, ExampleSubmissionRepository exampleSubmissionRepository) {
        this.complaintService = complaintService;
        this.programmingExerciseRepository = programmingExerciseRepository;
        this.submissionRepository = submissionRepository;
        this.resultRepository = resultRepository;
        this.exampleSubmissionRepository = exampleSubmissionRepository;
    }

    /**
     * Prepares the exercises for the assessment dashboard by setting the tutor participations and statistics
     *
     * @param exercises exercises to be prepared for the assessment dashboard
     * @param tutorParticipations participations of the tutors
     * @param examMode flag should be set for exam dashboard
     */
    public void generateStatisticsForExercisesForAssessmentDashboard(Set<Exercise> exercises, List<TutorParticipation> tutorParticipations, boolean examMode) {
        log.info("generateStatisticsForExercisesForAssessmentDashboard invoked");
        long start = System.nanoTime();
        for (Exercise exercise : exercises) {

            DueDateStat numberOfSubmissions;
            DueDateStat totalNumberOfAssessments;

            if (exercise instanceof ProgrammingExercise) {
                numberOfSubmissions = new DueDateStat(programmingExerciseRepository.countSubmissionsByExerciseIdSubmitted(exercise.getId(), examMode), 0L);
                log.info("StatsTimeLog: number of submitted submissions done in " + TimeLogUtil.formatDurationFrom(start) + " for programming exercise " + exercise.getId());
            }
            else {
                numberOfSubmissions = submissionRepository.countSubmissionsForExercise(exercise.getId(), examMode);
                log.info("StatsTimeLog: number of submitted submissions done in " + TimeLogUtil.formatDurationFrom(start) + " for exercise " + exercise.getId());
            }
            exercise.setNumberOfSubmissions(numberOfSubmissions);

            // set number of correctionrounds. If it isn't an exam the default is 1. If it is an exam we fetch the number of correction rounds which is set for the exam
            int numberOfCorrectionRounds = 1;
            final DueDateStat[] numberOfAssessmentsOfCorrectionRounds;
            if (examMode) {
                numberOfCorrectionRounds = exercise.getExerciseGroup().getExam().getNumberOfCorrectionRoundsInExam();
                numberOfAssessmentsOfCorrectionRounds = resultRepository.countNumberOfFinishedAssessmentsForExamExerciseForCorrectionRound(exercise, numberOfCorrectionRounds);
            }
            else {
                numberOfAssessmentsOfCorrectionRounds = new DueDateStat[1];
                numberOfAssessmentsOfCorrectionRounds[0] = new DueDateStat(resultRepository.countNumberOfFinishedAssessmentsForExercise(exercise.getId()), 0L);
            }
            log.info("StatsTimeLog: number of assessments per correction round in " + TimeLogUtil.formatDurationFrom(start) + " for exercise " + exercise.getId());

            exercise.setNumberOfAssessmentsOfCorrectionRounds(numberOfAssessmentsOfCorrectionRounds);
            exercise.setTotalNumberOfAssessments(numberOfAssessmentsOfCorrectionRounds[0]);

            complaintService.calculateNrOfOpenComplaints(exercise, examMode);
            log.info("StatsTimeLog: number of open complaints done in " + TimeLogUtil.formatDurationFrom(start) + " for exercise " + exercise.getId());
            Set<ExampleSubmission> exampleSubmissions = exampleSubmissionRepository.findAllWithEagerResultByExerciseId(exercise.getId());

            log.info("StatsTimeLog: example submissions done in " + TimeLogUtil.formatDurationFrom(start) + " for exercise " + exercise.getId());

            // Do not provide example submissions without any assessment
            exampleSubmissions.removeIf(exampleSubmission -> exampleSubmission.getSubmission() == null || exampleSubmission.getSubmission().getLatestResult() == null);
            exercise.setExampleSubmissions(exampleSubmissions);

            TutorParticipation tutorParticipation = tutorParticipations.stream().filter(participation -> participation.getAssessedExercise().getId().equals(exercise.getId()))
                    .findFirst().orElseGet(() -> {
                        TutorParticipation emptyTutorParticipation = new TutorParticipation();
                        emptyTutorParticipation.setStatus(TutorParticipationStatus.NOT_PARTICIPATED);
                        return emptyTutorParticipation;
                    });
            exercise.setTutorParticipations(Collections.singleton(tutorParticipation));

            log.info("StatsTimeLog: tutor participations done in " + TimeLogUtil.formatDurationFrom(start) + " for exercise " + exercise.getId());
        }
    }
}
