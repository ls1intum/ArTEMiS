import { NgModule } from '@angular/core';
import { ModelingAssessmentComponent } from 'app/modeling-assessment/modeling-assessment.component';
import { ScoreDisplayComponent } from './score-display/score-display.component';
import { ArtemisSharedModule } from 'app/shared';

@NgModule({
    declarations: [ModelingAssessmentComponent, ScoreDisplayComponent],
    exports: [ModelingAssessmentComponent, ScoreDisplayComponent],
    imports: [ArtemisSharedModule],
})
export class ModelingAssessmentModule {}
