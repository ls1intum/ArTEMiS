package de.tum.in.www1.artemis.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.config.Constants;
import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.NotificationType;
import de.tum.in.www1.artemis.domain.exam.Exam;
import de.tum.in.www1.artemis.domain.exam.ExerciseGroup;
import de.tum.in.www1.artemis.domain.notification.GroupNotification;
import de.tum.in.www1.artemis.repository.CourseRepository;
import de.tum.in.www1.artemis.repository.ExamRepository;
import de.tum.in.www1.artemis.repository.LearningGoalRepository;
import de.tum.in.www1.artemis.repository.UserRepository;
import de.tum.in.www1.artemis.security.SecurityUtils;
import de.tum.in.www1.artemis.service.exam.ExamService;
import de.tum.in.www1.artemis.service.user.UserService;
import de.tum.in.www1.artemis.web.rest.errors.AccessForbiddenException;

/**
 * Service Implementation for managing Course.
 */
@Service
public class CourseService {

    @Value("${artemis.course-archives-path}")
    private String courseArchivesDirPath;

    private final Logger log = LoggerFactory.getLogger(CourseService.class);

    private final ExerciseService exerciseService;

    private final AuthorizationCheckService authCheckService;

    private final LectureService lectureService;

    private final NotificationService notificationService;

    private final UserService userService;

    private final ExerciseGroupService exerciseGroupService;

    private final CourseExportService courseExportService;

    private final ExamService examService;

    private final ExamRepository examRepository;

    private final GroupNotificationService groupNotificationService;

    private final CourseRepository courseRepository;

    private final UserRepository userRepository;

    private final AuditEventRepository auditEventRepository;

    private final LearningGoalRepository learningGoalRepository;

    public CourseService(CourseRepository courseRepository, ExerciseService exerciseService, AuthorizationCheckService authCheckService, UserRepository userRepository,
            LectureService lectureService, NotificationService notificationService, ExerciseGroupService exerciseGroupService, AuditEventRepository auditEventRepository,
            UserService userService, LearningGoalRepository learningGoalRepository, GroupNotificationService groupNotificationService, ExamService examService,
            ExamRepository examRepository, CourseExportService courseExportService) {
        this.courseRepository = courseRepository;
        this.exerciseService = exerciseService;
        this.authCheckService = authCheckService;
        this.userRepository = userRepository;
        this.lectureService = lectureService;
        this.notificationService = notificationService;
        this.exerciseGroupService = exerciseGroupService;
        this.auditEventRepository = auditEventRepository;
        this.userService = userService;
        this.learningGoalRepository = learningGoalRepository;
        this.groupNotificationService = groupNotificationService;
        this.examService = examService;
        this.examRepository = examRepository;
        this.courseExportService = courseExportService;
    }

    /**
     * Get one course with exercises and lectures (filtered for given user)
     *
     * @param courseId the course to fetch
     * @param user     the user entity
     * @return the course including exercises and lectures for the user
     */
    public Course findOneWithExercisesAndLecturesForUser(Long courseId, User user) {
        Course course = courseRepository.findByIdWithLecturesAndExamsElseThrow(courseId);
        if (!authCheckService.isAtLeastStudentInCourse(course, user)) {
            throw new AccessForbiddenException("You are not allowed to access this resource");
        }
        course.setExercises(exerciseService.findAllForCourse(course, user));
        course.setLectures(lectureService.filterActiveAttachments(course.getLectures(), user));
        if (authCheckService.isOnlyStudentInCourse(course, user)) {
            course.setExams(examService.filterVisibleExams(course.getExams()));
        }
        return course;
    }

    /**
     * Get all courses for the given user
     *
     * @param user the user entity
     * @return the list of all courses for the user
     */
    public List<Course> findAllActiveForUser(User user) {
        return courseRepository.findAllActive(ZonedDateTime.now()).stream().filter(course -> course.getEndDate() == null || course.getEndDate().isAfter(ZonedDateTime.now()))
                .filter(course -> isActiveCourseVisibleForUser(user, course)).collect(Collectors.toList());
    }

    /**
     * Get all courses with exercises and lectures (filtered for given user)
     *
     * @param user the user entity
     * @return the list of all courses including exercises and lectures for the user
     */
    public List<Course> findAllActiveWithExercisesAndLecturesForUser(User user) {
        return courseRepository.findAllActiveWithLecturesAndExams().stream()
                // filter old courses and courses the user should not be able to see
                // skip old courses that have already finished
                .filter(course -> course.getEndDate() == null || course.getEndDate().isAfter(ZonedDateTime.now())).filter(course -> isActiveCourseVisibleForUser(user, course))
                .peek(course -> {
                    course.setExercises(exerciseService.findAllForCourse(course, user));
                    course.setLectures(lectureService.filterActiveAttachments(course.getLectures(), user));
                    if (authCheckService.isOnlyStudentInCourse(course, user)) {
                        course.setExams(examService.filterVisibleExams(course.getExams()));
                    }
                }).collect(Collectors.toList());
    }

    private boolean isActiveCourseVisibleForUser(User user, Course course) {
        // Instructors and TAs see all courses that have not yet finished
        if (authCheckService.isAtLeastTeachingAssistantInCourse(course, user)) {
            return true;
        }
        // Students see all courses that have already started (and not yet finished)
        if (user.getGroups().contains(course.getStudentGroupName())) {
            return course.getStartDate() == null || course.getStartDate().isBefore(ZonedDateTime.now());
        }

        return false;
    }

    /**
     * Deletes all elements associated with the course including:
     * <ul>
     *     <li>The Course</li>
     *     <li>All Exercises including:
     *      submissions, participations, results, repositories and build plans, see {@link ExerciseService#delete}</li>
     *     <li>All Lectures and their Attachments, see {@link LectureService#delete}</li>
     *     <li>All GroupNotifications of the course, see {@link NotificationService#deleteGroupNotification}</li>
     *     <li>All default groups created by Artemis, see {@link UserService#deleteGroup}</li>
     *     <li>All Exams, see {@link ExamService#delete}</li>
     * </ul>
     *
     * @param course the course to be deleted
     */
    public void delete(Course course) {
        log.debug("Request to delete Course : {}", course.getTitle());

        deleteLearningGoalsOfCourse(course);
        deleteExercisesOfCourse(course);
        deleteLecturesOfCourse(course);
        deleteNotificationsOfCourse(course);
        deleteDefaultGroups(course);
        deleteExamsOfCourse(course);
        courseRepository.deleteById(course.getId());
    }

    private void deleteExamsOfCourse(Course course) {
        // delete the Exams
        List<Exam> exams = examRepository.findByCourseId(course.getId());
        for (Exam exam : exams) {
            examService.delete(exam.getId());
        }
    }

    private void deleteDefaultGroups(Course course) {
        // only delete (default) groups which have been created by Artemis before
        if (course.getStudentGroupName().equals(course.getDefaultStudentGroupName())) {
            userService.deleteGroup(course.getStudentGroupName());
        }
        if (course.getTeachingAssistantGroupName().equals(course.getDefaultTeachingAssistantGroupName())) {
            userService.deleteGroup(course.getTeachingAssistantGroupName());
        }
        if (course.getInstructorGroupName().equals(course.getDefaultInstructorGroupName())) {
            userService.deleteGroup(course.getInstructorGroupName());
        }
    }

    private void deleteNotificationsOfCourse(Course course) {
        List<GroupNotification> notifications = notificationService.findAllGroupNotificationsForCourse(course);
        for (GroupNotification notification : notifications) {
            notificationService.deleteGroupNotification(notification);
        }
    }

    private void deleteLecturesOfCourse(Course course) {
        for (Lecture lecture : course.getLectures()) {
            lectureService.delete(lecture);
        }
    }

    private void deleteExercisesOfCourse(Course course) {
        for (Exercise exercise : course.getExercises()) {
            exerciseService.delete(exercise.getId(), true, true);
        }
    }

    private void deleteLearningGoalsOfCourse(Course course) {
        for (LearningGoal learningGoal : course.getLearningGoals()) {
            learningGoalRepository.deleteById(learningGoal.getId());
        }
    }

    /**
     * Given a Course object, it returns the number of users enrolled in the course
     *
     * @param course - the course object we are interested in
     * @return the number of students for that course
     */
    public long countNumberOfStudentsForCourse(Course course) {
        String groupName = course.getStudentGroupName();
        return userRepository.countByGroupsIsContaining(groupName);
    }

    /**
     * If the exercise is part of an exam, retrieve the course through ExerciseGroup -> Exam -> Course.
     * Otherwise the course is already set and the id can be used to retrieve the course from the database.
     *
     * @param exercise the Exercise for which the course is retrieved
     * @return the Course of the Exercise
     */
    public Course retrieveCourseOverExerciseGroupOrCourseId(Exercise exercise) {

        if (exercise.isExamExercise()) {
            ExerciseGroup exerciseGroup = exerciseGroupService.findOneWithExam(exercise.getExerciseGroup().getId());
            exercise.setExerciseGroup(exerciseGroup);
            return exerciseGroup.getExam().getCourse();
        }
        else {
            Course course = courseRepository.findByIdElseThrow(exercise.getCourseViaExerciseGroupOrCourseMember().getId());
            exercise.setCourse(course);
            return course;
        }
    }

    /**
     * Registers a user in a course by adding him to the student group of the course
     *
     * @param user   The user that should get added to the course
     * @param course The course to which the user should get added to
     */
    public void registerUserForCourse(User user, Course course) {
        userService.addUserToGroup(user, course.getStudentGroupName());
        final var auditEvent = new AuditEvent(user.getLogin(), Constants.REGISTER_FOR_COURSE, "course=" + course.getTitle());
        auditEventRepository.add(auditEvent);
        log.info("User " + user.getLogin() + " has successfully registered for course " + course.getTitle());
    }

    /**
     * Fetches Course Management data from repository and returns a list of Course DTOs
     *
     * @param isOnlyActive Whether or not to include courses with a past endDate
     * @return A list of Course DTOs for the course management overview
     */
    public List<Course> getAllCoursesForOverview(Boolean isOnlyActive) {
        var dateTimeNow = isOnlyActive ? ZonedDateTime.now() : null;
        var user = userRepository.getUserWithGroupsAndAuthorities();
        var userGroups = new ArrayList<>(user.getGroups());
        return courseRepository.getAllCoursesForOverview(dateTimeNow, authCheckService.isAdmin(user), userGroups);
    }

    /**
     * Get the active students for this particular course
     *
     * @param courseId the id of the course
     * @return An Integer array containing active students for each index
     */
    public Integer[] getActiveStudents(Long courseId) {
        ZonedDateTime now = ZonedDateTime.now();
        LocalDateTime localStartDate = now.toLocalDateTime().with(DayOfWeek.MONDAY);
        LocalDateTime localEndDate = now.toLocalDateTime().with(DayOfWeek.SUNDAY);
        ZoneId zone = now.getZone();
        ZonedDateTime startDate = localStartDate.atZone(zone).minusWeeks(3).withHour(0).withMinute(0).withSecond(0).withNano(0);
        ZonedDateTime endDate = localEndDate.atZone(zone).withHour(23).withMinute(59).withSecond(59);
        List<Map<String, Object>> outcome = courseRepository.getActiveStudents(courseId, startDate, endDate);
        List<Map<String, Object>> distinctOutcome = removeDuplicatesFromMapList(outcome, startDate);
        return createResultArray(distinctOutcome, endDate);
    }

    /**
     * the List of maps result contains duplicated entries. This method compares the values and returns a List<Map<String, Object>>
     * without duplicated entries
     *
     * @param result the result given by the Repository call
     * @param startDate the startDate of the period
     * @return A List<Map<String, Object>> containing date and amount of active users in this period
     */
    private List<Map<String, Object>> removeDuplicatesFromMapList(List<Map<String, Object>> result, ZonedDateTime startDate) {
        Map<Object, List<String>> users = new HashMap<>();
        for (Map<String, Object> listElement : result) {
            ZonedDateTime date = (ZonedDateTime) listElement.get("day");
            int index = getWeekOfDate(date);
            String username = listElement.get("username").toString();
            List<String> usersInSameSlot = users.get(index);
            // if this index is not yet existing in users
            if (usersInSameSlot == null) {
                usersInSameSlot = new ArrayList<>();
                usersInSameSlot.add(username);
                users.put(index, usersInSameSlot);
            }   // if the value of the map for this index does not contain this username
            else if (!usersInSameSlot.contains(username)) {
                usersInSameSlot.add(username);
            }
        }
        List<Map<String, Object>> returnList = new ArrayList<>();
        users.forEach((k, v) -> {
            int year = (Integer) k < getWeekOfDate(startDate) ? startDate.getYear() + 1 : startDate.getYear();
            ZonedDateTime firstDateOfYear = ZonedDateTime.of(year, 1, 1, 0, 0, 0, 0, startDate.getZone());
            ZonedDateTime start = getWeekOfDate(firstDateOfYear) == 1 ? firstDateOfYear.plusWeeks(((Integer) k) - 1) : firstDateOfYear.plusWeeks((Integer) k);
            Map<String, Object> listElement = new HashMap<>();
            listElement.put("day", start);
            listElement.put("amount", (long) v.size());
            returnList.add(listElement);
        });
        return returnList;
    }

    /**
     * Gets a list of maps, each map describing an entry in the database. The map has the two keys "day" and "amount",
     * which map to the date and the amount of the findings. This Map-List is taken and converted into a Integer array,
     * containing the values for each bar of the graph
     *
     * @param outcome A List<Map<String, Object>>, containing the content which should be refactored into an array
     * @param endDate the endDate
     * @return an array, containing the values for each bar in the graph
     */
    private Integer[] createResultArray(List<Map<String, Object>> outcome, ZonedDateTime endDate) {
        Integer[] result = new Integer[4];
        Arrays.fill(result, 0);
        int week;
        for (Map<String, Object> map : outcome) {
            ZonedDateTime date = (ZonedDateTime) map.get("day");
            int amount = map.get("amount") != null ? ((Long) map.get("amount")).intValue() : 0;
            week = getWeekOfDate(date);
            for (int i = 0; i < result.length; i++) {
                if (week == getWeekOfDate(endDate.minusWeeks(i))) {
                    result[result.length - 1 - i] += amount;
                }
            }
        }
        return result;
    }

    private Integer getWeekOfDate(ZonedDateTime date) {
        LocalDate localDate = date.toLocalDate();
        TemporalField woy = WeekFields.of(DayOfWeek.MONDAY, 4).weekOfWeekBasedYear();
        return localDate.get(woy);
    }

    /**
     * Archives the course by creating a zip file will student submissions for
     * both the course exercises and exams.
     *
     * @param course the course to archive
     */
    @Async
    public void archiveCourse(Course course) {
        SecurityUtils.setAuthorizationObject();

        // Archiving a course is only possible after the course is over
        if (ZonedDateTime.now().isBefore(course.getEndDate())) {
            return;
        }

        // This contains possible errors encountered during the archve process
        ArrayList<String> exportErrors = new ArrayList<>();

        groupNotificationService.notifyInstructorGroupAboutCourseArchiveState(course, NotificationType.COURSE_ARCHIVE_STARTED, exportErrors);

        try {
            // Create course archives directory if it doesn't exist
            Files.createDirectories(Path.of(courseArchivesDirPath));
            log.info("Created the course archives directory at {} because it didn't exist.", courseArchivesDirPath);

            // Export the course to the archives directory.
            var archivedCoursePath = courseExportService.exportCourse(course, courseArchivesDirPath, exportErrors);

            // Attach the path to the archive to the course and save it in the database
            if (archivedCoursePath.isPresent()) {
                course.setCourseArchivePath(archivedCoursePath.get().toString());
                courseRepository.save(course);
            }
            else {
                groupNotificationService.notifyInstructorGroupAboutCourseArchiveState(course, NotificationType.COURSE_ARCHIVE_FAILED, exportErrors);
                return;
            }
        }
        catch (IOException e) {
            var error = "Failed to create course archives directory " + courseArchivesDirPath + ": " + e.getMessage();
            exportErrors.add(error);
            log.info(error);
        }

        groupNotificationService.notifyInstructorGroupAboutCourseArchiveState(course, NotificationType.COURSE_ARCHIVE_FINISHED, exportErrors);
    }

    /**
     * Cleans up a course by cleaning up all exercises from that course. This deletes all student
     * submissions. Note that a course has to be archived first before being cleaned up.
     *
     * @param courseId The id of the course to clean up
     */
    public void cleanupCourse(Long courseId) {
        // Get the course with all exercises
        var course = courseRepository.findWithEagerExercisesAndLecturesById(courseId);
        if (!course.hasCourseArchive()) {
            log.info("Cannot clean up course {} because it hasn't been archived.", courseId);
            return;
        }

        // Clean up exams
        var exams = courseRepository.findByIdWithLecturesAndExamsElseThrow(course.getId()).getExams();
        var examExercises = exams.stream().map(exam -> examRepository.findAllExercisesByExamId(exam.getId())).flatMap(Collection::stream).collect(Collectors.toSet());

        var exercisesToCleanup = Stream.concat(course.getExercises().stream(), examExercises.stream()).collect(Collectors.toSet());
        exercisesToCleanup.forEach(exercise -> {
            if (exercise instanceof ProgrammingExercise) {
                exerciseService.cleanup(exercise.getId(), true);
            }

            // TODO: extend exerciseService.cleanup to clean up all exercise types
        });

        log.info("The course {} has been cleaned up!", courseId);
    }

    public void addUserToGroup(User user, String group) {
        userService.addUserToGroup(user, group);
    }

    public void removeUserFromGroup(User user, String group) {
        userService.removeUserFromGroup(user, group);
    }
}
