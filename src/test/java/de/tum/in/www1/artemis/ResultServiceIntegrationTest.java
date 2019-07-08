package de.tum.in.www1.artemis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.ProgrammingLanguage;
import de.tum.in.www1.artemis.repository.ProgrammingExerciseRepository;
import de.tum.in.www1.artemis.service.ProgrammingExerciseTestCaseService;
import de.tum.in.www1.artemis.service.ResultService;
import de.tum.in.www1.artemis.service.connectors.BambooService;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@ActiveProfiles("artemis, bamboo")
public class ResultServiceIntegrationTest {

    @MockBean
    BambooService continuousIntegrationServiceMock;

    @Autowired
    ResultService resultService;

    @Autowired
    ProgrammingExerciseTestCaseService programmingExerciseTestCaseService;

    @Autowired
    ProgrammingExerciseRepository programmingExerciseRepository;

    private ProgrammingExercise programmingExercise;

    private Participation participation;

    @Before
    public void reset() {
        ProgrammingExercise programmingExerciseBeforeSave = new ProgrammingExercise().programmingLanguage(ProgrammingLanguage.JAVA);
        programmingExercise = programmingExerciseRepository.save(programmingExerciseBeforeSave);
        participation = new Participation();
        participation.exercise(programmingExercise).setId(1L);
        programmingExercise.setSolutionParticipation(participation);
    }

    @Test
    public void shouldUpdateTestCasesAndResultScoreFromSolutionParticipationResult() throws Exception {
        Result result = new Result();
        List<Feedback> feedbacks = new ArrayList<>();
        feedbacks.add(new Feedback().text("test1").positive(true));
        feedbacks.add(new Feedback().text("test2").positive(true));
        feedbacks.add(new Feedback().text("test4").positive(true));
        result.successful(false).feedbacks(feedbacks).score(20L);

        Set<ProgrammingExerciseTestCase> expectedTestCases = new HashSet<>();
        expectedTestCases.add(new ProgrammingExerciseTestCase().exercise(programmingExercise).testName("test1").active(true).weight(1).id(1L));
        expectedTestCases.add(new ProgrammingExerciseTestCase().exercise(programmingExercise).testName("test2").active(true).weight(1).id(2L));
        expectedTestCases.add(new ProgrammingExerciseTestCase().exercise(programmingExercise).testName("test4").active(true).weight(1).id(3L));

        Object requestDummy = new Object();

        when(continuousIntegrationServiceMock.onBuildCompletedNew(participation, requestDummy)).thenReturn(result);
        resultService.onResultNotifiedNew(participation, requestDummy);

        Set<ProgrammingExerciseTestCase> testCases = programmingExerciseTestCaseService.findByExerciseId(programmingExercise.getId());
        assertThat(testCases).isEqualTo(expectedTestCases);
        assertThat(result.getScore()).isEqualTo(100L);
    }

}
