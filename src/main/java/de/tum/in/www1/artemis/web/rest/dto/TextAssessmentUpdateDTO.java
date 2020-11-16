package de.tum.in.www1.artemis.web.rest.dto;

import java.util.Set;

import de.tum.in.www1.artemis.domain.AssessmentUpdate;
import de.tum.in.www1.artemis.domain.TextBlock;

public class TextAssessmentUpdateDTO extends AssessmentUpdate {

    private Set<TextBlock> textBlocks;

    public Set<TextBlock> getTextBlocks() {
        return textBlocks;
    }

    public void setTextBlocks(Set<TextBlock> textBlocks) {
        this.textBlocks = textBlocks;
    }
}
