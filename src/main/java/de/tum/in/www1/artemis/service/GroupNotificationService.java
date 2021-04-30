package de.tum.in.www1.artemis.service;

import static de.tum.in.www1.artemis.domain.notification.GroupNotificationFactory.createNotification;

import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.GroupNotificationType;
import de.tum.in.www1.artemis.domain.enumeration.NotificationType;
import de.tum.in.www1.artemis.domain.exam.Exam;
import de.tum.in.www1.artemis.domain.notification.GroupNotification;
import de.tum.in.www1.artemis.domain.quiz.QuizExercise;
import de.tum.in.www1.artemis.repository.GroupNotificationRepository;
import de.tum.in.www1.artemis.repository.UserRepository;

@Service
public class GroupNotificationService {

    private final GroupNotificationRepository groupNotificationRepository;

    private final SimpMessageSendingOperations messagingTemplate;

    private final UserRepository userRepository;

    public GroupNotificationService(GroupNotificationRepository groupNotificationRepository, SimpMessageSendingOperations messagingTemplate, UserRepository userRepository) {
        this.groupNotificationRepository = groupNotificationRepository;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    /**
     * Notify student groups about an attachment change.
     *
     * @param attachment       that has been changed
     * @param notificationText that should be displayed
     */
    public void notifyStudentGroupAboutAttachmentChange(Attachment attachment, String notificationText) {
        // Do not send a notification before the release date of the attachment.
        if (attachment.getReleaseDate() != null && attachment.getReleaseDate().isAfter(ZonedDateTime.now())) {
            return;
        }
        // Create and send the notification.
        saveAndSend(createNotification(attachment, userRepository.getUser(), GroupNotificationType.STUDENT, NotificationType.ATTACHMENT_CHANGE, notificationText));
    }

    /**
     * Notify students groups about an exercise opened for practice.
     *
     * @param exercise that has been opened for practice
     */
    public void notifyStudentGroupAboutExercisePractice(Exercise exercise) {
        saveAndSend(createNotification(exercise, userRepository.getUser(), GroupNotificationType.STUDENT, NotificationType.EXERCISE_PRACTICE, null));
    }

    /**
     * Notify student groups about a started quiz exercise. The notification is not sent via websocket.
     *
     * @param quizExercise that has been started
     */
    public void notifyStudentGroupAboutQuizExerciseStart(QuizExercise quizExercise) {
        groupNotificationRepository.save(createNotification(quizExercise, null, GroupNotificationType.STUDENT, NotificationType.QUIZ_EXERCISE_STARTED, null));
    }

    /**
     * Notify student groups about an exercise update.
     *
     * @param exercise         that has been updated
     * @param notificationText that should be displayed
     */
    public void notifyStudentGroupAboutExerciseUpdate(Exercise exercise, String notificationText) {
        // Do not send a notification before the release date of the exercise.
        if (exercise.getReleaseDate() != null && exercise.getReleaseDate().isAfter(ZonedDateTime.now())) {
            return;
        }
        // Create and send the notification.
        saveAndSend(createNotification(exercise, userRepository.getUser(), GroupNotificationType.STUDENT, NotificationType.EXERCISE_UPDATED, notificationText));
    }

    /**
     * Notify tutor groups about the creation of an exercise.
     *
     * @param exercise that has been created
     */
    public void notifyTutorGroupAboutExerciseCreated(Exercise exercise) {
        saveAndSend(createNotification(exercise, userRepository.getUser(), GroupNotificationType.TA, NotificationType.EXERCISE_CREATED, null));
    }

    /**
     * Notify editor and instructor groups about an exercise update.
     *
     * @param exercise         that has been updated
     * @param notificationText that should be displayed
     */
    public void notifyEditorAndInstructorGroupAboutExerciseUpdate(Exercise exercise, String notificationText) {
        saveAndSend(createNotification(exercise, null, GroupNotificationType.EDITOR, NotificationType.EXERCISE_UPDATED, notificationText));
        saveAndSend(createNotification(exercise, null, GroupNotificationType.INSTRUCTOR, NotificationType.EXERCISE_UPDATED, notificationText));
    }

    /**
     * Notify tutor, editor and instructor groups about a new question in an exercise.
     *
     * @param studentQuestion that has been posted
     */
    public void notifyTutorAndEditorAndInstructorGroupAboutNewQuestionForExercise(StudentQuestion studentQuestion) {
        saveAndSend(createNotification(studentQuestion, userRepository.getUser(), GroupNotificationType.TA, NotificationType.NEW_QUESTION_FOR_EXERCISE));
        saveAndSend(createNotification(studentQuestion, userRepository.getUser(), GroupNotificationType.EDITOR, NotificationType.NEW_QUESTION_FOR_EXERCISE));
        saveAndSend(createNotification(studentQuestion, userRepository.getUser(), GroupNotificationType.INSTRUCTOR, NotificationType.NEW_QUESTION_FOR_EXERCISE));
    }

    /**
     * Notify editor and instructor groups about duplicate test cases.
     *
     * @param exercise         that has been updated
     * @param notificationText that should be displayed
     */
    public void notifyEditorAndInstructorGroupAboutDuplicateTestCasesForExercise(Exercise exercise, String notificationText) {
        saveAndSend(createNotification(exercise, null, GroupNotificationType.EDITOR, NotificationType.DUPLICATE_TEST_CASE, notificationText));
        saveAndSend(createNotification(exercise, null, GroupNotificationType.INSTRUCTOR, NotificationType.DUPLICATE_TEST_CASE, notificationText));
    }

    /**
     * Notify instructor groups about illegal submissions. In case a student has submitted after the individual end date or exam end date,
     * the submission is not valid and therefore marked as illegal. We notify the instructor about this cheating attempt.
     *
     * @param exercise         that has been affected
     * @param notificationText that should be displayed
     */
    public void notifyInstructorGroupAboutIllegalSubmissionsForExercise(Exercise exercise, String notificationText) {
        saveAndSend(createNotification(exercise, null, GroupNotificationType.INSTRUCTOR, NotificationType.ILLEGAL_SUBMISSION, notificationText));
    }

    /**
     * Notify tutor, editor and instructor groups about a new question in a lecture.
     *
     * @param studentQuestion that has been posted
     */
    public void notifyTutorAndEditorAndInstructorGroupAboutNewQuestionForLecture(StudentQuestion studentQuestion) {
        saveAndSend(createNotification(studentQuestion, userRepository.getUser(), GroupNotificationType.TA, NotificationType.NEW_QUESTION_FOR_LECTURE));
        saveAndSend(createNotification(studentQuestion, userRepository.getUser(), GroupNotificationType.EDITOR, NotificationType.NEW_QUESTION_FOR_LECTURE));
        saveAndSend(createNotification(studentQuestion, userRepository.getUser(), GroupNotificationType.INSTRUCTOR, NotificationType.NEW_QUESTION_FOR_LECTURE));
    }

    /**
     * Notify tutor, editor and instructor groups about a new answer for an exercise.
     *
     * @param studentQuestionAnswer that has been submitted for a question
     */
    public void notifyTutorAndEditorAndInstructorGroupAboutNewAnswerForExercise(StudentQuestionAnswer studentQuestionAnswer) {
        saveAndSend(createNotification(studentQuestionAnswer, userRepository.getUser(), GroupNotificationType.TA, NotificationType.NEW_ANSWER_FOR_EXERCISE));
        saveAndSend(createNotification(studentQuestionAnswer, userRepository.getUser(), GroupNotificationType.EDITOR, NotificationType.NEW_ANSWER_FOR_EXERCISE));
        saveAndSend(createNotification(studentQuestionAnswer, userRepository.getUser(), GroupNotificationType.INSTRUCTOR, NotificationType.NEW_ANSWER_FOR_EXERCISE));
    }

    /**
     * Notify tutor, editor and instructor groups about a new answer for a lecture.
     *
     * @param studentQuestionAnswer that has been submitted for a question
     */
    public void notifyTutorAndEditorAndInstructorGroupAboutNewAnswerForLecture(StudentQuestionAnswer studentQuestionAnswer) {
        saveAndSend(createNotification(studentQuestionAnswer, userRepository.getUser(), GroupNotificationType.TA, NotificationType.NEW_ANSWER_FOR_LECTURE));
        saveAndSend(createNotification(studentQuestionAnswer, userRepository.getUser(), GroupNotificationType.EDITOR, NotificationType.NEW_ANSWER_FOR_LECTURE));
        saveAndSend(createNotification(studentQuestionAnswer, userRepository.getUser(), GroupNotificationType.INSTRUCTOR, NotificationType.NEW_ANSWER_FOR_LECTURE));
    }

    /**
     * Notify tutor and instructor groups about a new answer for a lecture.
     *
     * @param course           The course
     * @param notificationType The state of the archiving process
     * @param archiveErrors    a list of errors that happened during archiving
     */
    public void notifyInstructorGroupAboutCourseArchiveState(Course course, NotificationType notificationType, List<String> archiveErrors) {
        saveAndSend(createNotification(course, null, GroupNotificationType.INSTRUCTOR, notificationType, archiveErrors));
    }

    /**
     * Notify instructor groups about the archive state of the exam.
     *
     * @param exam           The exam
     * @param notificationType The state of the archiving process
     * @param archiveErrors    a list of errors that happened during archiving
     */
    public void notifyInstructorGroupAboutExamArchiveState(Exam exam, NotificationType notificationType, List<String> archiveErrors) {
        saveAndSend(createNotification(exam, null, GroupNotificationType.INSTRUCTOR, notificationType, archiveErrors));
    }

    /**
     * Saves the given notification in database and sends it to the client via websocket.
     *
     * @param notification that should be saved and sent
     */
    private void saveAndSend(GroupNotification notification) {
        groupNotificationRepository.save(notification);
        messagingTemplate.convertAndSend(notification.getTopic(), notification);
    }
}
