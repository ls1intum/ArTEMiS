import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { ArtemisSharedCommonModule } from 'app/shared/shared-common.module';
import { ExamParticipationComponent } from 'app/exam/participate/exam-participation.component';
import { ExamParticipationCoverComponent } from './exam-cover/exam-participation-cover.component';
import { ExamParticipationSummaryComponent } from 'app/exam/participate/summary/exam-participation-summary.component';
import { QuizExamSubmissionComponent } from 'app/exam/participate/exercises/quiz/quiz-exam-submission.component';
import { examParticipationState } from 'app/exam/participate/exam-participation.route';
import { ArtemisQuizQuestionTypesModule } from 'app/exercises/quiz/shared/questions/artemis-quiz-question-types.module';
import { ArtemisSharedModule } from 'app/shared/shared.module';
import { TextExamSubmissionComponent } from 'app/exam/participate/exercises/text/text-exam-submission.component';
import { ModelingExamSubmissionComponent } from 'app/exam/participate/exercises/modeling/modeling-exam-submission.component';
import { ArtemisModelingEditorModule } from 'app/exercises/modeling/shared/modeling-editor.module';
import { ArtemisFullscreenModule } from 'app/shared/fullscreen/fullscreen.module';
import { ExamNavigationBarComponent } from './exam-navigation-bar/exam-navigation-bar.component';
import { ArtemisSharedComponentModule } from 'app/shared/components/shared-component.module';

const ENTITY_STATES = [...examParticipationState];

@NgModule({
    imports: [
        RouterModule.forChild(ENTITY_STATES),
        ArtemisSharedCommonModule,
        ArtemisSharedModule,
        ArtemisQuizQuestionTypesModule,
        ArtemisModelingEditorModule,
        ArtemisFullscreenModule,
        ArtemisSharedComponentModule,
    ],
    declarations: [
        ExamParticipationComponent,
        ExamParticipationCoverComponent,
        ExamParticipationSummaryComponent,
        QuizExamSubmissionComponent,
        TextExamSubmissionComponent,
        ModelingExamSubmissionComponent,
        ExamNavigationBarComponent,
    ],
})
export class ArtemisExamParticipationModule {}
