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
public class ScoringStrategyDragAndDropProportionalWithPenalty implements ScoringStrategy {
    @Override
    public double calculateScore(Question question, SubmittedAnswer submittedAnswer) {
        //check if the question is invalid: if true: -> return with full points
        if (question.isInvalid()) {
            return question.getScore();
        }

        if (submittedAnswer instanceof DragAndDropSubmittedAnswer && question instanceof DragAndDropQuestion) {
            DragAndDropSubmittedAnswer dndAnswer = (DragAndDropSubmittedAnswer) submittedAnswer;
            DragAndDropQuestion dndQuestion = (DragAndDropQuestion) question;

            double mappedDropLocations = 0;
            double correctMappings = 0;
            double incorrectMappings = 0;

            // iterate through each drop location and compare its correct mappings with the answer's mapping
            for (DropLocation dropLocation : dndQuestion.getDropLocations()) {
                Set<DragItem> correctDragItems = dndQuestion.getCorrectDragItemsForDropLocation(dropLocation);
                DragItem selectedDragItem = dndAnswer.getSelectedDragItemForDropLocation(dropLocation);

                // count the number of drop locations that were meant to not stay empty
                if (correctDragItems.size() > 0) {
                    mappedDropLocations++;
                }

                // invalid drop location or invalid drag item => always correct
                if (dropLocation.isInvalid() || (selectedDragItem != null && selectedDragItem.isInvalid())) {
                    // but points are only given for drop locations that were meant to not stay empty
                    if (correctDragItems.size() > 0) {
                        correctMappings++;
                    }
                } else {
                    // check if user's mapping is correct
                    if (dropLocation.isDropLocationCorrect(dndAnswer)) {
                        // points are only given for drop locations that were meant to not stay empty
                        if (correctDragItems.size() > 0) {
                            correctMappings++;
                        }
                    } else {
                        // wrong mappings always deduct points
                        incorrectMappings++;
                    }
                }
            }

            // calculate the fraction of the total score the user should get
            // every correct mapping increases fraction by 1/mappedDropLocations,
            // every incorrect mapping decreases fraction by 1/mappedDropLocations
            double fraction = ((correctMappings / mappedDropLocations) - (incorrectMappings / mappedDropLocations));

            // end result is maxScore * fraction, but at least 0
            return Math.max(0, question.getScore() * fraction);
        }
        // the submitted answer's type doesn't fit the question's type => it cannot be correct
        return 0.0;
    }
}
