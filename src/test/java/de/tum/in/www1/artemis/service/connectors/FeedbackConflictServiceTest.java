package de.tum.in.www1.artemis.service.connectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import de.tum.in.www1.artemis.domain.enumeration.FeedbackConflictType;
import de.tum.in.www1.artemis.exception.NetworkingError;
import de.tum.in.www1.artemis.service.dto.FeedbackConflictResponseDTO;
import de.tum.in.www1.artemis.service.dto.TextFeedbackConflictRequestDTO;

public class FeedbackConflictServiceTest {

    private static final String TEXT_ASSESSMENT_CONFLICT_ENDPOINT = "http://localhost:8001/feedback_consistency";

    /**
     * Creates two submissions with feedback and send them to remote Athene service to check feedback consistency.
     * Checks if the consistency is found correctly.
     * @throws NetworkingError if the request isn't successful
     */
    @Test
    public void checkFeedbackConsistency() throws NetworkingError {
        final TextAssessmentConflictService textAssessmentConflictService = new TextAssessmentConflictService();
        ReflectionTestUtils.setField(textAssessmentConflictService, "API_ENDPOINT", TEXT_ASSESSMENT_CONFLICT_ENDPOINT);

        final List<TextFeedbackConflictRequestDTO> textFeedbackConflictRequestDTOS = new ArrayList<>();

        String firstSubmissionText = "My answer text block for the question.";
        String firstFeedbackText = "Correct answer.";
        final TextFeedbackConflictRequestDTO firstRequestObject = new TextFeedbackConflictRequestDTO("1", firstSubmissionText, 1L, 1L, firstFeedbackText, 1.0);
        textFeedbackConflictRequestDTOS.add(firstRequestObject);

        textAssessmentConflictService.checkFeedbackConsistencies(textFeedbackConflictRequestDTOS, -1L, 0);
        textFeedbackConflictRequestDTOS.clear();

        String secondSubmissionText = "My answer text block for the question.";
        String secondFeedbackText = "Correct answer.";
        final TextFeedbackConflictRequestDTO secondRequestObject = new TextFeedbackConflictRequestDTO("2", secondSubmissionText, 1L, 2L, secondFeedbackText, 2.0);
        textFeedbackConflictRequestDTOS.add(secondRequestObject);

        List<FeedbackConflictResponseDTO> feedbackConflicts = textAssessmentConflictService.checkFeedbackConsistencies(textFeedbackConflictRequestDTOS, -1L, 0);
        assertThat(feedbackConflicts, is(not(empty())));
        assertThat(feedbackConflicts, hasItem(
                either(hasProperty("firstFeedbackId", is(firstRequestObject.getFeedbackId()))).or(hasProperty("secondFeedbackId", is(firstRequestObject.getFeedbackId())))));
        assertThat(feedbackConflicts, hasItem(
                either(hasProperty("firstFeedbackId", is(secondRequestObject.getFeedbackId()))).or(hasProperty("secondFeedbackId", is(secondRequestObject.getFeedbackId())))));
        assertThat(feedbackConflicts, hasItem(hasProperty("type", is(FeedbackConflictType.INCONSISTENT_SCORE))));
    }

    @BeforeAll
    public static void runClassOnlyIfTextAssessmentConflictServiceIsAvailable() {
        assumeTrue(isTextAssessmentConflictServiceAvailable());
    }

    private static boolean isTextAssessmentConflictServiceAvailable() {
        try {
            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(TEXT_ASSESSMENT_CONFLICT_ENDPOINT).openConnection();
            httpURLConnection.setRequestMethod("HEAD");
            httpURLConnection.setConnectTimeout(1000);
            final int responseCode = httpURLConnection.getResponseCode();

            return responseCode == 405;
        }
        catch (IOException e) {
            return false;
        }
    }
}
