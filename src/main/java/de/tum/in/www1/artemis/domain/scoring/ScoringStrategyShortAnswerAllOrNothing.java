package de.tum.in.www1.artemis.domain.scoring;

import de.tum.in.www1.artemis.domain.*;
import java.util.HashSet;
import java.util.Set;

/**
 * All or nothing means the full score is given if the answer is 100% correct,
 * otherwise a score of 0 is given
 */
public class ScoringStrategyShortAnswerAllOrNothing implements ScoringStrategy {
    @Override
    public double calculateScore(Question question, SubmittedAnswer submittedAnswer) {
        // return maximal Score if the question is invalid
        if (question.isInvalid()) {
            return question.getScore();
        }
        if (submittedAnswer instanceof ShortAnswerSubmittedAnswer && question instanceof ShortAnswerQuestion) {
            ShortAnswerSubmittedAnswer shortAnswerAnswer = (ShortAnswerSubmittedAnswer) submittedAnswer;
            ShortAnswerQuestion shortAnswerQuestion = (ShortAnswerQuestion) question;

            double[] values = ScoringStrategyShortAnswerUtil.getCorrectAndIncorrectSolutionsrShortAnswerQuestion(shortAnswerQuestion, shortAnswerAnswer);
            double correctSolutions = values[0];

            if(correctSolutions == shortAnswerQuestion.getSpots().size()){
                return shortAnswerQuestion.getScore();
            } else {
                return 0.0;
            }
        }
        // the submitted answer's type doesn't fit the question's type => it cannot be correct
        return 0.0;
    }
}
