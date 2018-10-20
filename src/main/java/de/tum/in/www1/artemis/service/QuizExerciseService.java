package de.tum.in.www1.artemis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.view.QuizView;
import de.tum.in.www1.artemis.repository.DragAndDropMappingRepository;
import de.tum.in.www1.artemis.repository.QuizExerciseRepository;
import de.tum.in.www1.artemis.repository.QuizSubmissionRepository;
import de.tum.in.www1.artemis.repository.ResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class QuizExerciseService {

    private final Logger log = LoggerFactory.getLogger(QuizExerciseService.class);

    private final QuizExerciseRepository quizExerciseRepository;
    private final DragAndDropMappingRepository dragAndDropMappingRepository;
    private final ParticipationService participationService;
    private final AuthorizationCheckService authCheckService;
    private final ResultRepository resultRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final SimpMessageSendingOperations messagingTemplate;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public QuizExerciseService(UserService userService,
                               QuizExerciseRepository quizExerciseRepository,
                               DragAndDropMappingRepository dragAndDropMappingRepository,
                               ParticipationService participationService,
                               AuthorizationCheckService authCheckService,
                               ResultRepository resultRepository,
                               QuizSubmissionRepository quizSubmissionRepository,
                               SimpMessageSendingOperations messagingTemplate,
                               MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter) {
        this.userService = userService;
        this.quizExerciseRepository = quizExerciseRepository;
        this.dragAndDropMappingRepository = dragAndDropMappingRepository;
        this.participationService = participationService;
        this.authCheckService = authCheckService;
        this.resultRepository = resultRepository;
        this.quizSubmissionRepository = quizSubmissionRepository;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = mappingJackson2HttpMessageConverter.getObjectMapper();
    }

    /**
     * Save the given quizExercise to the database
     * and make sure that objects with references to one another
     * are saved in the correct order to avoid PersistencyExceptions
     *
     * @param quizExercise the quiz exercise to save
     * @return the saved quiz exercise
     */
    @Transactional
    public QuizExercise save(QuizExercise quizExercise) {
        log.debug("Request to save QuizExercise : {}", quizExercise);

        // fix references in all drag and drop questions (step 1/2)
        for (Question question : quizExercise.getQuestions()) {
            if (question instanceof DragAndDropQuestion) {
                DragAndDropQuestion dragAndDropQuestion = (DragAndDropQuestion) question;
                // save references as index to prevent Hibernate Persistence problem
                saveCorrectMappingsInIndices(dragAndDropQuestion);
            }
        }

        // save result
        // Note: save will automatically remove deleted questions from the exercise and deleted answer options from the questions
        //       and delete the now orphaned entries from the database
        QuizExercise result = quizExerciseRepository.save(quizExercise);

        // fix references in all drag and drop questions (step 2/2)
        for (Question question : result.getQuestions()) {
            if (question instanceof DragAndDropQuestion) {
                DragAndDropQuestion dragAndDropQuestion = (DragAndDropQuestion) question;
                // restore references from index after save
                restoreCorrectMappingsFromIndices(dragAndDropQuestion);
            }
        }

        return result;
    }

    /**
     * Save the given quizExercise to the database
     * Note: Use this method if you are sure that there are no new entities
     *
     * @param quizExercise the quiz exercise to save
     * @return the saved quiz exercise
     */
    @Transactional
    public QuizExercise saveWithNoNewEntities(QuizExercise quizExercise) {
        return quizExerciseRepository.save(quizExercise);
    }

    /**
     * Get all quiz exercises.
     *
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public List<QuizExercise> findAll() {
        log.debug("REST request to get all QuizExercises");
        List<QuizExercise> quizExercises = quizExerciseRepository.findAll();
        User user = userService.getUserWithGroupsAndAuthorities();
        Stream<QuizExercise> authorizedExercises = quizExercises.stream().filter(
            exercise -> {
                Course course = exercise.getCourse();
                return authCheckService.isTeachingAssistantInCourse(course, user) ||
                    authCheckService.isInstructorInCourse(course, user) ||
                    authCheckService.isAdmin();
            }
        );
        return authorizedExercises.collect(Collectors.toList());
    }

    /**
     * Get one quiz exercise by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public QuizExercise findOne(Long id) {
        log.debug("Request to get Quiz Exercise : {}", id);
        return quizExerciseRepository.findById(id).get();
    }

    /**
     * Get one quiz exercise by id and eagerly load questions
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public QuizExercise findOneWithQuestions(Long id) {
        log.debug("Request to get Quiz Exercise : {}", id);
        long start = System.currentTimeMillis();
        Optional<QuizExercise> quizExercise = quizExerciseRepository.findById(id);
        log.debug("    loaded quiz after {} ms", System.currentTimeMillis() - start);
        if (quizExercise.isPresent()) {
            quizExercise.get().getQuestions().size();
            log.debug("    loaded questions after {} ms", System.currentTimeMillis() - start);
            return quizExercise.get();
        }
        return null;
    }

    /**
     * Get one quiz exercise by id and eagerly load questions and statistics
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public QuizExercise findOneWithQuestionsAndStatistics(Long id) {
        log.debug("Request to get Quiz Exercise : {}", id);
        long start = System.currentTimeMillis();
        Optional<QuizExercise> optionalQuizExercise = quizExerciseRepository.findById(id);
        if (optionalQuizExercise.isPresent()) {
            QuizExercise quizExercise = optionalQuizExercise.get();
            quizExercise.getQuestions().size();
            log.debug("    loaded questions after {} ms", System.currentTimeMillis() - start);
            quizExercise.getQuizPointStatistic().getPointCounters().size();
            log.debug("    loaded quiz point statistic after {} ms", System.currentTimeMillis() - start);
            for (Question question : quizExercise.getQuestions()) {
                question.getQuestionStatistic().getRatedCorrectCounter();
            }
            log.debug("    loaded question statistics after {} ms", System.currentTimeMillis() - start);
            return quizExercise;
        }
        return null;
    }

    /**
     * Get all quiz exercises for the given course.
     *
     * @param courseId the id of the course
     * @return the entity
     */
    @Transactional(readOnly = true)
    public List<QuizExercise> findByCourseId(Long courseId) {
        log.debug("Request to get all Quiz Exercises in Course : {}", courseId);
        List<QuizExercise> quizExercises = quizExerciseRepository.findByCourseId(courseId);
        User user = userService.getUserWithGroupsAndAuthorities();
        if (quizExercises.size() > 0) {
            Course course = quizExercises.get(0).getCourse();
            if (!authCheckService.isTeachingAssistantInCourse(course, user) &&
                !authCheckService.isInstructorInCourse(course, user) &&
                !authCheckService.isAdmin()) {
                return new LinkedList<>();
            }
        }
        return quizExercises;
    }

    /**
     * Get all quiz exercises that are planned to start in the future
     *
     * @return the list of quiz exercises
     */
    @Transactional(readOnly = true)
    public List<QuizExercise> findAllPlannedToStartInTheFutureWithQuestions() {
        List<QuizExercise> quizExercises = quizExerciseRepository.findByIsPlannedToStartAndReleaseDateIsAfter(true, ZonedDateTime.now());
        for (QuizExercise quizExercise : quizExercises) {
            quizExercise.getQuestions().size();
        }
        return quizExercises;
    }

    /**
     * Delete the quiz exercise by id.
     *
     * @param id the id of the entity
     */
    @Transactional
    public void delete(Long id) {
        log.debug("Request to delete Exercise : {}", id);
        // delete all participations belonging to this quiz
        participationService.deleteAllByExerciseId(id, false, false);

        quizExerciseRepository.deleteById(id);
    }

    /**
     * adjust existing results if an answer or and question was deleted
     * and recalculate the scores
     *
     * @param quizExercise the changed quizExercise.
     */
    @Transactional
    public void adjustResultsOnQuizChanges(QuizExercise quizExercise) {
        //change existing results if an answer or and question was deleted
        for (Result result : resultRepository.findByParticipationExerciseIdOrderByCompletionDateAsc(quizExercise.getId())) {

            Set<SubmittedAnswer> submittedAnswersToDelete = new HashSet<>();
            QuizSubmission quizSubmission = quizSubmissionRepository.findById(result.getSubmission().getId()).get();

            for (SubmittedAnswer submittedAnswer : quizSubmission.getSubmittedAnswers()) {
                // Delete all references to question and question-elements if the question was changed
                submittedAnswer.checkAndDeleteReferences(quizExercise);
                if (!quizExercise.getQuestions().contains(submittedAnswer.getQuestion())) {
                    submittedAnswersToDelete.add(submittedAnswer);
                }
            }
            quizSubmission.getSubmittedAnswers().removeAll(submittedAnswersToDelete);

            //recalculate existing score
            quizSubmission.calculateAndUpdateScores(quizExercise);
            //update Successful-Flag in Result
            result.getParticipation().setExercise(quizExercise);
            result.setSubmission(quizSubmission);
            result.evaluateSubmission();

            // save the updated Result and its Submission
            resultRepository.save(result);
        }
    }

    @Transactional(readOnly = true)
    public void sendQuizExerciseToSubscribedClients(QuizExercise quizExercise) {
        try{
            long start = System.currentTimeMillis();
            Class view = viewForStudentsInQuizExercise(quizExercise);
            byte[] payload = objectMapper.writerWithView(view).writeValueAsBytes(quizExercise);
            messagingTemplate.send("/topic/quizExercise/" + quizExercise.getId(), MessageBuilder.withPayload(payload).build());
            log.info("    sent out quizExercise to all listening clients in {} ms", System.currentTimeMillis() - start);
        } catch (JsonProcessingException e) {
            log.error("Exception occurred while serializing quiz exercise: {}", e);
        }
    }

    /**
     * Check if the current user has at least TA-level permissions for the given exercise
     *
     * @param quizExercise the exercise to check permissions for
     * @return true, if the user has the required permissions, false otherwise
     */
    public boolean userHasTAPermissions(QuizExercise quizExercise) {
        Course course = quizExercise.getCourse();
        User user = userService.getUserWithGroupsAndAuthorities();
        return authCheckService.isTeachingAssistantInCourse(course, user) ||
            authCheckService.isInstructorInCourse(course, user) ||
            authCheckService.isAdmin();
    }

    /**
     * Check if the current user is allowed to see the given exercise
     * @param quizExercise the exercise to check permissions for
     * @return true, if the user has the required permissions, false otherwise
     */
    public boolean userIsAllowedToSeeExercise(QuizExercise quizExercise) {
        return authCheckService.isAllowedToSeeExercise(quizExercise, null);
    }

    /**
     * get the view for students in the given quiz
     * @param quizExercise the quiz to get the view for
     * @return the view depending on the current state of the quiz
     */
    public Class viewForStudentsInQuizExercise(QuizExercise quizExercise) {
        if (!quizExercise.isStarted()) {
            return QuizView.Before.class;
        } else if (quizExercise.isSubmissionAllowed()) {
            return QuizView.During.class;
        } else {
            return QuizView.After.class;
        }
    }

    /**
     * remove dragItem and dropLocation from correct mappings and set dragItemIndex and dropLocationIndex instead
     *
     * @param dragAndDropQuestion the question for which to perform these actions
     */
    private void saveCorrectMappingsInIndices(DragAndDropQuestion dragAndDropQuestion) {
        List<DragAndDropMapping> mappingsToBeRemoved = new ArrayList<>();
        for (DragAndDropMapping mapping : dragAndDropQuestion.getCorrectMappings()) {
            // check for NullPointers
            if (mapping.getDragItem() == null || mapping.getDropLocation() == null) {
                mappingsToBeRemoved.add(mapping);
                continue;
            }

            // drag item index
            DragItem dragItem = mapping.getDragItem();
            boolean dragItemFound = false;
            for (DragItem questionDragItem : dragAndDropQuestion.getDragItems()) {
                if (dragItem.equals(questionDragItem)) {
                    dragItemFound = true;
                    mapping.setDragItemIndex(dragAndDropQuestion.getDragItems().indexOf(questionDragItem));
                    mapping.setDragItem(null);
                    break;
                }
            }

            // replace drop location
            DropLocation dropLocation = mapping.getDropLocation();
            boolean dropLocationFound = false;
            for (DropLocation questionDropLocation : dragAndDropQuestion.getDropLocations()) {
                if (dropLocation.equals(questionDropLocation)) {
                    dropLocationFound = true;
                    mapping.setDropLocationIndex(dragAndDropQuestion.getDropLocations().indexOf(questionDropLocation));
                    mapping.setDropLocation(null);
                    break;
                }
            }

            // if one of them couldn't be found, remove the mapping entirely
            if (!dragItemFound || !dropLocationFound) {
                mappingsToBeRemoved.add(mapping);
            }
        }

        for (DragAndDropMapping mapping : mappingsToBeRemoved) {
            dragAndDropQuestion.removeCorrectMappings(mapping);
        }
    }

    /**
     * restore dragItem and dropLocation for correct mappings using dragItemIndex and dropLocationIndex
     *
     * @param dragAndDropQuestion the question for which to perform these actions
     */
    private void restoreCorrectMappingsFromIndices(DragAndDropQuestion dragAndDropQuestion) {
        for (DragAndDropMapping mapping : dragAndDropQuestion.getCorrectMappings()) {
            // drag item
            mapping.setDragItem(dragAndDropQuestion.getDragItems().get(mapping.getDragItemIndex()));
            // drop location
            mapping.setDropLocation(dragAndDropQuestion.getDropLocations().get(mapping.getDropLocationIndex()));
            // set question
            mapping.setQuestion(dragAndDropQuestion);
            // save mapping
            dragAndDropMappingRepository.save(mapping);
        }
    }


}
