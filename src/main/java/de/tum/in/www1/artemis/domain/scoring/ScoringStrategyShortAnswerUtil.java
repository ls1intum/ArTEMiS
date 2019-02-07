package de.tum.in.www1.artemis.domain.scoring;

import de.tum.in.www1.artemis.domain.*;

import java.util.*;

public class ScoringStrategyShortAnswerUtil {

    public static double[] getCorrectAndIncorrectSolutionsrShortAnswerQuestion(ShortAnswerQuestion shortAnswerQuestion, ShortAnswerSubmittedAnswer shortAnswerAnswer) {
        boolean foundCorrectSolution;
        double correctSolutions = 0;
        double incorrectSolutions = 0;
        List<ShortAnswerSolution> notUsedSolutions = shortAnswerQuestion.getSolutions();

        // iterate through each spot and compare its correct solutions with the submitted texts
        for (ShortAnswerSpot spot : shortAnswerQuestion.getSpots()) {
            Set<ShortAnswerSolution> solutionsForSpot = shortAnswerQuestion.getCorrectSolutionForSpot(spot);
            ShortAnswerSubmittedText submittedTextForSpot = shortAnswerAnswer.getSubmittedTextForSpot(spot);
            foundCorrectSolution = false;

            for (ShortAnswerSolution solution : solutionsForSpot) {
                if (submittedTextForSpot.isSubmittedTextCorrect(submittedTextForSpot.getText(), solution.getText())
                    && notUsedSolutions.contains(solution)) {
                    notUsedSolutions.remove(solution);
                    submittedTextForSpot.setIsCorrect(true);
                    correctSolutions++;
                    foundCorrectSolution = true;
                    break;
                }
            }
            if (!foundCorrectSolution) {
                incorrectSolutions++;
            }
        }
        double[] values = {correctSolutions,incorrectSolutions};
        return values;
    }
}
