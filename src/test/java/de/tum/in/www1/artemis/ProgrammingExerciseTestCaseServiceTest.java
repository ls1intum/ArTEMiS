package de.tum.in.www1.artemis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.repository.ProgrammingExerciseRepository;
import de.tum.in.www1.artemis.repository.ProgrammingExerciseTestCaseRepository;
import de.tum.in.www1.artemis.service.ProgrammingExerciseTestCaseService;
import de.tum.in.www1.artemis.util.DatabaseUtilService;
import de.tum.in.www1.artemis.web.rest.dto.WeightUpdate;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@ActiveProfiles("artemis, bamboo")
public class ProgrammingExerciseTestCaseServiceTest {

    @Autowired
    ProgrammingExerciseTestCaseRepository testCaseRepository;

    @Autowired
    ProgrammingExerciseTestCaseService testCaseService;

    @Autowired
    ProgrammingExerciseRepository programmingExerciseRepository;

    @Autowired
    DatabaseUtilService database;

    private ProgrammingExercise programmingExercise;

    private Result result;

    @Before
    public void reset() {
        database.resetDatabase();
        database.addUsers(0, 1, 0);

        database.addCourseWithOneProgrammingExerciseAndTestCases();

        result = new Result();

        programmingExercise = programmingExerciseRepository.findAll().get(0);
    }

    @Test
    public void shouldSetAllTestCasesToInactiveIfFeedbackListIsEmpty() {
        List<Feedback> feedbacks = new ArrayList<>();
        testCaseService.generateTestCasesFromFeedbacks(feedbacks, programmingExercise);

        Set<ProgrammingExerciseTestCase> testCases = testCaseRepository.findByExerciseId(programmingExercise.getId());
        assertThat(testCases).hasSize(3);

        assertThat(testCases.stream().noneMatch(ProgrammingExerciseTestCase::isActive)).isTrue();
    }

    @Test
    public void shouldUpdateActiveFlagsOfTestCases() {
        List<Feedback> feedbacks = new ArrayList<>();
        feedbacks.add(new Feedback().text("test1"));
        feedbacks.add(new Feedback().text("test2"));
        feedbacks.add(new Feedback().text("test4"));
        feedbacks.add(new Feedback().text("test5"));
        testCaseService.generateTestCasesFromFeedbacks(feedbacks, programmingExercise);

        Set<ProgrammingExerciseTestCase> testCases = testCaseRepository.findByExerciseId(programmingExercise.getId());
        assertThat(testCases).hasSize(5);

        assertThat(testCases.stream().allMatch(testCase -> {
            if (testCase.getTestName().equals("test3")) {
                return !testCase.isActive();
            }
            else {
                return testCase.isActive();
            }
        })).isTrue();
    }

    @Test
    public void shouldGenerateNewTestCases() {
        testCaseRepository.deleteAll();

        List<Feedback> feedbacks = new ArrayList<>();
        feedbacks.add(new Feedback().text("test1"));
        feedbacks.add(new Feedback().text("test2"));
        testCaseService.generateTestCasesFromFeedbacks(feedbacks, programmingExercise);

        Set<ProgrammingExerciseTestCase> testCases = testCaseRepository.findByExerciseId(programmingExercise.getId());
        assertThat(testCases).hasSize(2);

        assertThat(testCases.stream().allMatch(ProgrammingExerciseTestCase::isActive)).isTrue();
    }

    @Test
    public void shouldResetTestWeights() {
        new ArrayList<>(testCaseRepository.findByExerciseId(programmingExercise.getId())).get(0).weight(50);
        testCaseService.resetWeights(programmingExercise.getId());

        Set<ProgrammingExerciseTestCase> testCases = testCaseRepository.findByExerciseId(programmingExercise.getId());
        assertThat(testCases.stream().mapToInt(ProgrammingExerciseTestCase::getWeight).sum()).isEqualTo(testCases.size());
    }

    @Test
    @WithMockUser(value = "tutor1", roles = "TA")
    public void shouldUpdateTestWeight() throws IllegalAccessException {
        ProgrammingExerciseTestCase testCase = testCaseRepository.findAll().get(0);
        Set<WeightUpdate> weightUpdates = new HashSet<>();
        WeightUpdate weightUpdate = new WeightUpdate();
        weightUpdate.setId(testCase.getId());
        weightUpdate.setWeight(400);
        weightUpdates.add(weightUpdate);

        testCaseService.updateWeights(programmingExercise.getId(), weightUpdates);

        assertThat(testCaseRepository.findById(testCase.getId()).get().getWeight()).isEqualTo(400);
    }

    @Test
    public void shouldNotUpdateResultIfNoTestCasesExist() {
        testCaseRepository.deleteAll();

        Long scoreBeforeUpdate = result.getScore();
        testCaseService.updateResultFromTestCases(result, programmingExercise);

        assertThat(result.getScore()).isEqualTo(scoreBeforeUpdate);
    }

    @Test
    public void shouldRecalculateScoreBasedOnTestCasesWeight() {
        List<Feedback> feedbacks = new ArrayList<>();
        feedbacks.add(new Feedback().text("test1").positive(true));
        feedbacks.add(new Feedback().text("test2").positive(true));
        feedbacks.add(new Feedback().text("test3").positive(false));
        result.feedbacks(feedbacks);
        result.successful(false);
        Long scoreBeforeUpdate = result.getScore();

        testCaseService.updateResultFromTestCases(result, programmingExercise);

        Long expectedScore = 25L;

        assertThat(scoreBeforeUpdate).isNotEqualTo(result.getScore());
        assertThat(result.getScore()).isEqualTo(expectedScore);
    }

}
