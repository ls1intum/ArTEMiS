package de.tum.in.www1.artemis.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.domain.Course;
import de.tum.in.www1.artemis.domain.Exercise;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.exam.Exam;
import de.tum.in.www1.artemis.domain.leaderboard.tutor.*;
import de.tum.in.www1.artemis.repository.*;
import de.tum.in.www1.artemis.repository.UserRepository;
import de.tum.in.www1.artemis.web.rest.ProgrammingExerciseGradingResource;
import de.tum.in.www1.artemis.web.rest.dto.TutorLeaderboardDTO;

@Service
public class TutorLeaderboardService {

    private final ResultRepository resultRepository;

    private final ComplaintRepository complaintRepository;

    private final UserRepository userRepository;

    private final Logger log = LoggerFactory.getLogger(ProgrammingExerciseGradingResource.class);

    public TutorLeaderboardService(ResultRepository resultRepository, ComplaintRepository complaintRepository, UserRepository userRepository) {
        this.resultRepository = resultRepository;
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
    }

    /**
     * Returns tutor leaderboards for the specified course.
     *
     * @param course course for which leaderboard is created
     * @return list of tutor leaderboard objects
     */
    public List<TutorLeaderboardDTO> getCourseLeaderboard(Course course) {

        List<User> tutors = userRepository.getTutors(course);
        String groupName = course.getTeachingAssistantGroupName();

        long start = System.currentTimeMillis();
        List<TutorLeaderboardAssessments> tutorLeaderboardAssessments = resultRepository.findTutorLeaderboardAssessmentByCourseId(course.getId());
        long end = System.currentTimeMillis();
        log.info("Finished >>resultRepository.findTutorLeaderboardAssessmentByCourseId<< call for course " + course.getId() + " in " + (end - start) + "ms");

        start = System.currentTimeMillis();
        List<TutorLeaderboardComplaints> tutorLeaderboardComplaints = complaintRepository.findTutorLeaderboardComplaintsByCourseId(groupName, course.getId());
        end = System.currentTimeMillis();
        log.info("Finished >>complaintRepository.findTutorLeaderboardComplaintsByCourseId<< call for course " + course.getId() + " in " + (end - start) + "ms");

        start = System.currentTimeMillis();
        List<TutorLeaderboardMoreFeedbackRequests> tutorLeaderboardMoreFeedbackRequests = complaintRepository.findTutorLeaderboardMoreFeedbackRequestsByCourseId(groupName,
                course.getId());
        end = System.currentTimeMillis();
        log.info("Finished >>complaintRepository.findTutorLeaderboardMoreFeedbackRequestsByCourseId<< call for course " + course.getId() + " in " + (end - start) + "ms");

        start = System.currentTimeMillis();
        List<TutorLeaderboardComplaintResponses> tutorLeaderboardComplaintResponses = complaintRepository.findTutorLeaderboardComplaintResponsesByCourseId(groupName,
                course.getId());
        end = System.currentTimeMillis();
        log.info("Finished >>complaintRepository.findTutorLeaderboardComplaintResponsesByCourseId<< call for course " + course.getId() + " in " + (end - start) + "ms");

        start = System.currentTimeMillis();
        List<TutorLeaderboardAnsweredMoreFeedbackRequests> tutorLeaderboardAnsweredMoreFeedbackRequests = complaintRepository
                .findTutorLeaderboardAnsweredMoreFeedbackRequestsByCourseId(groupName, course.getId());
        end = System.currentTimeMillis();
        log.info("Finished >>complaintRepository.findTutorLeaderboardAnsweredMoreFeedbackRequestsByCourseId<< call for course " + course.getId() + " in " + (end - start) + "ms");

        return aggregateTutorLeaderboardData(tutors, tutorLeaderboardAssessments, tutorLeaderboardComplaints, tutorLeaderboardMoreFeedbackRequests,
                tutorLeaderboardComplaintResponses, tutorLeaderboardAnsweredMoreFeedbackRequests, false);
    }

    /**
     * Returns tutor leaderboards for the specified course.
     *
     * @param course - course for which leaderboard is fetched
     * @param exam   - the exam for which the leaderboard will be fetched
     * @return list of tutor leaderboard objects
     */
    public List<TutorLeaderboardDTO> getExamLeaderboard(Course course, Exam exam) {

        List<User> tutors = userRepository.getTutors(course);
        String groupName = course.getTeachingAssistantGroupName();

        long start = System.currentTimeMillis();
        List<TutorLeaderboardAssessments> tutorLeaderboardAssessments = resultRepository.findTutorLeaderboardAssessmentByExamId(exam.getId());
        long end = System.currentTimeMillis();
        log.info("Finished >>resultRepository.findTutorLeaderboardAssessmentByExamId<< call for exercise " + exam.getId() + " in " + (end - start) + "ms");
        start = System.currentTimeMillis();
        List<TutorLeaderboardComplaints> tutorLeaderboardComplaints = complaintRepository.findTutorLeaderboardComplaintsByExamId(groupName, exam.getId());
        end = System.currentTimeMillis();
        log.info("Finished >>complaintRepository.findTutorLeaderboardComplaintsByExamId<< call for exercise " + exam.getId() + " in " + (end - start) + "ms");
        start = System.currentTimeMillis();
        List<TutorLeaderboardComplaintResponses> tutorLeaderboardComplaintResponses = complaintRepository.findTutorLeaderboardComplaintResponsesByExamId(groupName, exam.getId());
        end = System.currentTimeMillis();
        log.info("Finished >>complaintRepository.findTutorLeaderboardComplaintResponsesByExamId<< call for exercise " + exam.getId() + " in " + (end - start) + "ms");

        return aggregateTutorLeaderboardData(tutors, tutorLeaderboardAssessments, tutorLeaderboardComplaints, new ArrayList<>(), tutorLeaderboardComplaintResponses,
                new ArrayList<>(), true);
    }

    /**
     * Returns tutor leaderboards for the specified exercise.
     *
     * @param exercise exercise for which leaderboard is created
     * @return list of tutor leaderboard objects
     */
    public List<TutorLeaderboardDTO> getExerciseLeaderboard(Exercise exercise) {

        List<User> tutors = userRepository.getTutors(exercise.getCourseViaExerciseGroupOrCourseMember());
        String groupName = exercise.getCourseViaExerciseGroupOrCourseMember().getTeachingAssistantGroupName();
        long start = System.currentTimeMillis();
        List<TutorLeaderboardAssessments> tutorLeaderboardAssessments = resultRepository.findTutorLeaderboardAssessmentByExerciseId(exercise.getId());
        long end = System.currentTimeMillis();
        log.info("Finished >>resultRepository.findTutorLeaderboardAssessmentByExerciseId<< call for exercise " + exercise.getId() + " in " + (end - start) + "ms");
        start = System.currentTimeMillis();
        List<TutorLeaderboardComplaints> tutorLeaderboardComplaints = complaintRepository.findTutorLeaderboardComplaintsByExerciseId(groupName, exercise.getId());
        end = System.currentTimeMillis();
        log.info("Finished >>complaintRepository.findTutorLeaderboardComplaintsByExerciseId<< call for exercise " + exercise.getId() + " in " + (end - start) + "ms");
        start = System.currentTimeMillis();
        List<TutorLeaderboardMoreFeedbackRequests> tutorLeaderboardMoreFeedbackRequests = complaintRepository.findTutorLeaderboardMoreFeedbackRequestsByExerciseId(groupName,
                exercise.getId());
        end = System.currentTimeMillis();
        log.info("Finished >>complaintRepository.findTutorLeaderboardMoreFeedbackRequestsByExerciseId<< call for exercise " + exercise.getId() + " in " + (end - start) + "ms");
        start = System.currentTimeMillis();
        List<TutorLeaderboardComplaintResponses> tutorLeaderboardComplaintResponses = complaintRepository.findTutorLeaderboardComplaintResponsesByExerciseId(groupName,
                exercise.getId());
        end = System.currentTimeMillis();
        log.info("Finished >>complaintRepository.findTutorLeaderboardComplaintResponsesByExerciseId<< call for exercise " + exercise.getId() + " in " + (end - start) + "ms");
        start = System.currentTimeMillis();
        List<TutorLeaderboardAnsweredMoreFeedbackRequests> tutorLeaderboardAnsweredMoreFeedbackRequests = complaintRepository
                .findTutorLeaderboardAnsweredMoreFeedbackRequestsByExerciseId(groupName, exercise.getId());
        end = System.currentTimeMillis();
        log.info("Finished >>complaintRepository.findTutorLeaderboardAnsweredMoreFeedbackRequestsByExerciseId<< call for exercise " + exercise.getId() + " in " + (end - start)
                + "ms");

        return aggregateTutorLeaderboardData(tutors, tutorLeaderboardAssessments, tutorLeaderboardComplaints, tutorLeaderboardMoreFeedbackRequests,
                tutorLeaderboardComplaintResponses, tutorLeaderboardAnsweredMoreFeedbackRequests, exercise.isExamExercise());
    }

    @NotNull
    private List<TutorLeaderboardDTO> aggregateTutorLeaderboardData(List<User> tutors, List<TutorLeaderboardAssessments> tutorLeaderboardAssessments,
            List<TutorLeaderboardComplaints> tutorLeaderboardComplaints, List<TutorLeaderboardMoreFeedbackRequests> tutorLeaderboardMoreFeedbackRequests,
            List<TutorLeaderboardComplaintResponses> tutorLeaderboardComplaintResponses,
            List<TutorLeaderboardAnsweredMoreFeedbackRequests> tutorLeaderboardAnsweredMoreFeedbackRequests, boolean isExam) {

        Map<Long, TutorLeaderboardAssessments> tutorLeaderboardAssessmentsMap = tutorLeaderboardAssessments.stream()
                .collect(Collectors.toMap(TutorLeaderboardAssessments::getKey, t -> t));
        Map<Long, TutorLeaderboardComplaints> tutorLeaderboardComplaintsMap = tutorLeaderboardComplaints.stream()
                .collect(Collectors.toMap(TutorLeaderboardComplaints::getKey, t -> t));
        Map<Long, TutorLeaderboardMoreFeedbackRequests> tutorLeaderboardMoreFeedbackRequestsMap = tutorLeaderboardMoreFeedbackRequests.stream()
                .collect(Collectors.toMap(TutorLeaderboardMoreFeedbackRequests::getKey, t -> t));
        Map<Long, TutorLeaderboardComplaintResponses> tutorLeaderboardComplaintResponsesMap = tutorLeaderboardComplaintResponses.stream()
                .collect(Collectors.toMap(TutorLeaderboardComplaintResponses::getKey, t -> t));
        Map<Long, TutorLeaderboardAnsweredMoreFeedbackRequests> tutorLeaderboardAnsweredMoreFeedbackRequestsMap = tutorLeaderboardAnsweredMoreFeedbackRequests.stream()
                .collect(Collectors.toMap(TutorLeaderboardAnsweredMoreFeedbackRequests::getKey, t -> t));

        List<TutorLeaderboardDTO> tutorLeaderBoardEntries = new ArrayList<>();
        for (User tutor : tutors) {

            long numberOfAssessments = 0L;
            long numberOfAcceptedComplaints = 0L;
            long numberOfTutorComplaints = 0L;
            long numberOfNotAnsweredMoreFeedbackRequests = 0L;
            long numberOfComplaintResponses = 0L;
            long numberOfAnsweredMoreFeedbackRequests = 0L;
            long numberOfTutorMoreFeedbackRequests = 0L;
            double points = 0.0;

            var assessment = tutorLeaderboardAssessmentsMap.getOrDefault(tutor.getId(), new TutorLeaderboardAssessments());
            numberOfAssessments += assessment.getAssessments();
            points += assessment.getPoints();

            var complaints = tutorLeaderboardComplaintsMap.getOrDefault(tutor.getId(), new TutorLeaderboardComplaints());
            numberOfTutorComplaints += complaints.getAllComplaints();
            numberOfAcceptedComplaints += complaints.getAcceptedComplaints();
            // accepted complaints count 2x negatively
            points -= 2 * complaints.getPoints();

            var complaintResponses = tutorLeaderboardComplaintResponsesMap.getOrDefault(tutor.getId(), new TutorLeaderboardComplaintResponses());
            numberOfComplaintResponses += complaintResponses.getComplaintResponses();
            // resolved complaints count 2x
            points += 2 * complaintResponses.getPoints();

            if (!isExam) {
                var moreFeedbackRequests = tutorLeaderboardMoreFeedbackRequestsMap.getOrDefault(tutor.getId(), new TutorLeaderboardMoreFeedbackRequests());
                numberOfNotAnsweredMoreFeedbackRequests += moreFeedbackRequests.getNotAnsweredRequests();
                numberOfTutorMoreFeedbackRequests += moreFeedbackRequests.getAllRequests();
                // not answered requests count only 1x negatively
                points -= moreFeedbackRequests.getPoints();

                var answeredFeedbackRequests = tutorLeaderboardAnsweredMoreFeedbackRequestsMap.getOrDefault(tutor.getId(), new TutorLeaderboardAnsweredMoreFeedbackRequests());
                numberOfAnsweredMoreFeedbackRequests += answeredFeedbackRequests.getAnsweredRequests();
                // answered requests doesn't count, because it only means that the tutor repaired the negative points
            }
            tutorLeaderBoardEntries.add(new TutorLeaderboardDTO(tutor.getId(), tutor.getName(), numberOfAssessments, numberOfAcceptedComplaints, numberOfTutorComplaints,
                    numberOfNotAnsweredMoreFeedbackRequests, numberOfComplaintResponses, numberOfAnsweredMoreFeedbackRequests, numberOfTutorMoreFeedbackRequests, points));
        }
        return tutorLeaderBoardEntries;
    }
}
