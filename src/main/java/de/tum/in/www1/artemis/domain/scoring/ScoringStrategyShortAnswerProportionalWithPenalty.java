package de.tum.in.www1.artemis.domain.scoring;

import de.tum.in.www1.artemis.domain.*;

import java.util.Set;

/**
 * Proportional with Penalty means that
 * every correct mapping increases the score by x and
 * every incorrect mapping decreases the score by x
 * where x = maxScore / numberOfDropLocationsThatShouldHaveAMapping
 * if the result is negative, a score of 0 is given instead
 */
public class ScoringStrategyShortAnswerProportionalWithPenalty implements ScoringStrategy{
    @Override
    public double calculateScore(Question question, SubmittedAnswer submittedAnswer) {
        //check if the question is invalid: if true: -> return with full points
        if (question.isInvalid()) {
            return question.getScore();
        }

        if (submittedAnswer instanceof ShortAnswerSubmittedAnswer && question instanceof ShortAnswerQuestion) {
            ShortAnswerSubmittedAnswer saAnswer = (ShortAnswerSubmittedAnswer) submittedAnswer;
            ShortAnswerQuestion saQuestion = (ShortAnswerQuestion) question;

            boolean foundCorrectSolution = false;
            double totalSolutions = saQuestion.getSpots().size();
            double correctSolutions = 0;
            double incorrectSolutions = 0;


            // iterate through each spot and compare its correct solutions with the submitted texts
            for (ShortAnswerSpot spot : saQuestion.getSpots()) {
                Set<ShortAnswerSolution> solutionsForSpot = saQuestion.getCorrectSolutionForSpot(spot);

                for(ShortAnswerSubmittedText text : saAnswer.getSubmittedTexts()){
                    if(text.getSpot().equals(spot)){

                        for(ShortAnswerSolution solution : solutionsForSpot){
                            //needs to be changed with better algorithm
                            if(solution.getText().equalsIgnoreCase(text.getText())){
                                correctSolutions++;
                                foundCorrectSolution = true;
                                break;
                            }
                        }
                        if(!foundCorrectSolution){
                            incorrectSolutions++;
                        }
                    }
                }
            }

            // calculate the fraction of the total score the user should get
            // every correct mapping increases fraction by 1/mappedDropLocations,
            // every incorrect mapping decreases fraction by 1/mappedDropLocations
            double fraction = ((correctSolutions / totalSolutions) - (incorrectSolutions / totalSolutions));

            // end result is maxScore * fraction, but at least 0
            return Math.max(0, question.getScore() * fraction);
        } else {
            // the submitted answer's type doesn't fit the question's type => it cannot be correct
            return 0.0;
        }
    }

}
