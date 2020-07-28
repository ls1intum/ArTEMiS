package de.tum.in.www1.artemis.service;

import static de.tum.in.www1.artemis.web.rest.util.ResponseUtil.forbidden;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.domain.Exercise;
import de.tum.in.www1.artemis.domain.ProgrammingExercise;
import de.tum.in.www1.artemis.domain.Submission;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.exam.Exam;
import de.tum.in.www1.artemis.domain.exam.StudentExam;
import de.tum.in.www1.artemis.domain.participation.StudentParticipation;

@Service
public class ExamSubmissionService {

    private final StudentExamService studentExamService;

    private final ExamService examService;

    private final ParticipationService participationService;

    public ExamSubmissionService(StudentExamService studentExamService, ExamService examService, ParticipationService participationService) {
        this.studentExamService = studentExamService;
        this.examService = examService;
        this.participationService = participationService;
    }

    /**
     * Check if the submission is a exam submission and if so, check that the current user is allowed to submit.
     *
     * @param exercise  the exercise for which a submission should be saved
     * @param user      the user that wants to submit
     * @param <T>       The type of the return type of the requesting route so that the
     *                  response can be returned there
     * @return an Optional with a typed ResponseEntity. If it is empty all checks passed
     */
    public <T> Optional<ResponseEntity<T>> checkSubmissionAllowance(Exercise exercise, User user) {
        if (!isAllowedToSubmit(exercise, user)) {
            // TODO: improve the error message sent to the client
            return Optional.of(forbidden());
        }
        return Optional.empty();
    }

    /**
     * Check if the user is allowed to submit (submission is in time & user's student exam has the exercise).
     *
     * @param exercise  the exercise for which a submission should be saved
     * @param user      the user that wants to submit
     * @return true if it is not an exam of if it is an exam and the submission is in time and the exercise is part of
     *         the user's student exam
     */
    public boolean isAllowedToSubmit(Exercise exercise, User user) {
        if (isExamSubmission(exercise)) {
            // Get the student exam if it was not passed to the function
            Exam exam = exercise.getExerciseGroup().getExam();
            StudentExam studentExam = studentExamService.findOneWithExercisesByUserIdAndExamId(user.getId(), exam.getId());

            // Check that the current user is allowed to submit to this exercise
            if (!studentExam.getExercises().contains(exercise)) {
                return false;
            }

            // if the student exam was already submitted, the user cannot save any more
            if (Boolean.TRUE.equals(studentExam.isSubmitted()) || studentExam.getSubmissionDate() != null) {
                return false;
            }

            // Check that the submission is in time
            return isSubmissionInTime(exercise, studentExam);
        }
        return true;
    }

    /**
     * We want to prevent multiple submissions for text, modeling, file upload and quiz exercises. Therefore we check if
     * a submission for this exercise+student already exists.
     * - If a submission exists, we will always overwrite this submission, even if the id of the received submission
     *   deviates from the one we've got from the database.
     * - If no submission exists (on creation) we allow adding one (implicitly via repository.save()).
     *
     * TODO: we might want to move this to the SubmissionService
     *
     * @param exercise      the exercise for which the submission should be saved
     * @param submission    the submission
     * @param user          the current user
     * @return the submission. If a submission already exists for the exercise we will set the id
     */
    public Submission preventMultipleSubmissions(Exercise exercise, Submission submission, User user) {
        // Return immediately if it is not a exam submissions or if it is a programming exercise
        if (!isExamSubmission(exercise) || exercise.getClass() == ProgrammingExercise.class) {
            return submission;
        }

        List<StudentParticipation> participations = participationService.findByExerciseAndStudentIdWithEagerSubmissions(exercise, user.getId());
        if (!participations.isEmpty()) {
            Set<Submission> submissions = participations.get(0).getSubmissions();
            if (!submissions.isEmpty()) {
                Submission existingSubmission = submissions.iterator().next();
                // Instead of creating a new submission, we want to overwrite the already existing submission. Therefore
                // we set the id of the received submission to the id of the existing submission. When repository.save()
                // is invoked the existing submission will be updated.
                submission.setId(existingSubmission.getId());
            }
        }

        return submission;
    }

    private boolean isExamSubmission(Exercise exercise) {
        return exercise.hasExerciseGroup();
    }

    private boolean isSubmissionInTime(Exercise exercise, StudentExam studentExam) {
        // TODO: we might want to add a grace period here. If so we have to adjust the dueDate checks in the submission
        // services (e.g. in TextSubmissionService::handleTextSubmission())
        // The attributes of the exam (e.g. startDate) are missing. Therefore we need to load it.
        Exam exam = examService.findOne(exercise.getExerciseGroup().getExam().getId());
        ZonedDateTime calculatedEndDate = exam.getEndDate();
        if (studentExam.getWorkingTime() != null && studentExam.getWorkingTime() > 0) {
            calculatedEndDate = exam.getStartDate().plusSeconds(studentExam.getWorkingTime());
        }
        return exam.getStartDate().isBefore(ZonedDateTime.now()) && calculatedEndDate.isAfter(ZonedDateTime.now());
    }
}
