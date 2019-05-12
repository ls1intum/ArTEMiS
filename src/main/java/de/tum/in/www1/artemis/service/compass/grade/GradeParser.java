package de.tum.in.www1.artemis.service.compass.grade;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import de.tum.in.www1.artemis.service.compass.utils.JSONMapping;

public class GradeParser {

    /**
     * Build a compassGrade out of a json formatted assessment
     *
     * @param assessment a json formatted assessment
     * @return a compassGrade object
     * @throws JsonIOException on unexpected json format
     */
    public static CompassGrade importFromJSON(JsonObject assessment) throws JsonIOException {
        Map<String, String> commentMapping = new HashMap<>();
        Map<String, Double> pointMapping = new HashMap<>();

        double confidence = assessment.get(JSONMapping.assessmentElementConfidence).getAsDouble();
        double coverage = assessment.get(JSONMapping.assessmentElementCoverage).getAsDouble();
        double totalScore = 0;

        JsonArray assessmentArray = assessment.getAsJsonArray(JSONMapping.assessments);

        for (JsonElement o : assessmentArray) {
            JsonObject jsonAssessment = o.getAsJsonObject();

            String jsonElementID = jsonAssessment.get(JSONMapping.assessmentElementID).getAsString();
            String comment = jsonAssessment.has(JSONMapping.assessmentComment) ? jsonAssessment.get(JSONMapping.assessmentComment).getAsString() : "";
            double score = jsonAssessment.get(JSONMapping.assessmentPoints).getAsDouble();

            commentMapping.put(jsonElementID, comment);
            pointMapping.put(jsonElementID, score);
            totalScore += score;
        }

        return new CompassGrade(coverage, confidence, totalScore, commentMapping, pointMapping);
    }
}
