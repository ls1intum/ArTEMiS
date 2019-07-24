package de.tum.in.www1.artemis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import de.tum.in.www1.artemis.domain.Exercise;
import de.tum.in.www1.artemis.domain.TextExercise;
import de.tum.in.www1.artemis.domain.TextSubmission;
import de.tum.in.www1.artemis.domain.enumeration.Language;
import de.tum.in.www1.artemis.repository.ExerciseRepository;
import de.tum.in.www1.artemis.util.DatabaseUtilService;
import de.tum.in.www1.artemis.util.ModelFactory;
import de.tum.in.www1.artemis.util.RequestUtilService;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@ActiveProfiles("artemis")
public class TextSubmissionIntegrationTest {

    @Autowired
    ExerciseRepository exerciseRepo;

    @Autowired
    RequestUtilService request;

    @Autowired
    DatabaseUtilService database;

    private TextExercise textExercise;

    private TextSubmission textSubmission;

    @Before
    public void initTestCase() throws Exception {
        database.resetDatabase();
        database.addUsers(1, 1, 0);
        database.addCourseWithOneTextExerciseDueDateReached();
        textExercise = (TextExercise) exerciseRepo.findAll().get(0);
        textSubmission = ModelFactory.generateTextSubmission("example text", Language.ENGLISH, true);
    }

    @Test
    @WithMockUser(value = "tutor1", roles = "TA")
    public void getAllTextSubmissions_studentHidden() throws Exception {
        textSubmission = database.addTextSubmission(textExercise, textSubmission, "student1");

        List<TextSubmission> textSubmissions = request.getList("/api/exercises/" + textExercise.getId() + "/text-submissions", HttpStatus.OK, TextSubmission.class);

        assertThat(textSubmissions.size()).as("one text submission was found").isEqualTo(1);
        assertThat(textSubmissions.get(0).getId()).as("correct text submission was found").isEqualTo(textSubmission.getId());
        assertThat(textSubmissions.get(0).getParticipation().getStudent()).as("student of participation is hidden").isNull();
    }

    @Test
    @WithMockUser(value = "tutor1", roles = "TA")
    public void getTextSubmissionWithoutAssessment_studentHidden() throws Exception {
        textSubmission = database.addTextSubmission(textExercise, textSubmission, "student1");

        TextSubmission textSubmissionWithoutAssessment = request.get("/api/exercises/" + textExercise.getId() + "/text-submission-without-assessment", HttpStatus.OK,
                TextSubmission.class);

        assertThat(textSubmissionWithoutAssessment).as("text submission without assessment was found").isNotNull();
        assertThat(textSubmissionWithoutAssessment.getId()).as("correct text submission was found").isEqualTo(textSubmission.getId());
        assertThat(textSubmissionWithoutAssessment.getParticipation().getStudent()).as("student of participation is hidden").isNull();
    }

    @Test
    @WithMockUser(username = "student1")
    public void getResultsForCurrentStudent_assessorHiddenForStudent() throws Exception {
        textSubmission = ModelFactory.generateTextSubmission("Some text", Language.ENGLISH, true);
        database.addTextSubmissionWithResultAndAssessor(textExercise, textSubmission, "student1", "tutor1");

        Exercise returnedExercise = request.get("/api/exercises/" + textExercise.getId() + "/results", HttpStatus.OK, Exercise.class);

        assertThat(returnedExercise.getParticipations().iterator().next().getResults().iterator().next().getAssessor()).as("assessor is null").isNull();
    }

}
