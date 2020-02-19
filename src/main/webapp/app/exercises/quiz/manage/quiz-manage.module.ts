import { NgModule } from '@angular/core';
import { MultipleChoiceQuestionEditComponent } from './multiple-choice-question/multiple-choice-question-edit.component';
import { DragAndDropQuestionEditComponent } from './drag-and-drop-question/drag-and-drop-question-edit.component';
import { ShortAnswerQuestionEditComponent } from './short-answer-question/short-answer-question-edit.component';
import { AngularFittextModule } from 'angular-fittext';
import { AceEditorModule } from 'ng2-ace-editor';
import { ArtemisMarkdownEditorModule } from 'app/shared/markdown-editor/markdown-editor.module';
import { DndModule } from 'ng2-dnd';
import { QuizScoringInfoModalComponent } from './quiz-scoring-info-modal/quiz-scoring-info-modal.component';
import { ArtemisSharedModule } from 'app/shared/shared.module';
import { ArtemisQuizParticipationModule } from 'app/exercises/quiz/participate/quiz-participation.module';
import { ArtemisQuizStatisticModule } from 'app/exercises/quiz/manage/statistics/quiz-statistic.module';
import { ArtemisApollonDiagramsModule } from 'app/exercises/quiz/manage/apollon-diagrams/apollon-diagram.module';
import { quizExerciseRoute } from 'app/exercises/quiz/manage/quiz-exercise.route';
import { QuizExerciseComponent } from 'app/exercises/quiz/manage/quiz-exercise.component';
import { QuizExerciseDetailComponent } from 'app/exercises/quiz/manage/quiz-exercise-detail.component';
import { RouterModule } from '@angular/router';
import { SortByModule } from 'app/shared/pipes/sort-by.module';
import { FormDateTimePickerModule } from 'app/shared/date-time-picker/date-time-picker.module';
import { ArtemisCategorySelectorModule } from 'app/shared/category-selector/category-selector.module';
import { ArtemisDifficultyPickerModule } from 'app/exercises/shared/difficulty-picker/difficulty-picker.module';
import { QuizReEvaluateComponent } from 'app/exercises/quiz/manage/re-evaluate/quiz-re-evaluate.component';
import { ReEvaluateMultipleChoiceQuestionComponent } from 'app/exercises/quiz/manage/re-evaluate/multiple-choice-question/re-evaluate-multiple-choice-question.component';
import { ReEvaluateDragAndDropQuestionComponent } from 'app/exercises/quiz/manage/re-evaluate/drag-and-drop-question/re-evaluate-drag-and-drop-question.component';
import { ReEvaluateShortAnswerQuestionComponent } from 'app/exercises/quiz/manage/re-evaluate/short-answer-question/re-evaluate-short-answer-question.component';
import { QuizReEvaluateWarningComponent } from 'app/exercises/quiz/manage/re-evaluate/quiz-re-evaluate-warning.component';

const ENTITY_STATES = [...quizExerciseRoute];

@NgModule({
    imports: [
        ArtemisSharedModule,
        RouterModule.forChild(ENTITY_STATES),
        DndModule.forRoot(),
        SortByModule,
        AngularFittextModule,
        AceEditorModule,
        ArtemisQuizParticipationModule,
        ArtemisMarkdownEditorModule,
        ArtemisQuizStatisticModule,
        ArtemisApollonDiagramsModule,
        FormDateTimePickerModule,
        ArtemisCategorySelectorModule,
        ArtemisDifficultyPickerModule,
    ],
    declarations: [
        QuizExerciseComponent,
        QuizExerciseDetailComponent,
        MultipleChoiceQuestionEditComponent,
        DragAndDropQuestionEditComponent,
        QuizScoringInfoModalComponent,
        ShortAnswerQuestionEditComponent,
        QuizReEvaluateComponent,
        ReEvaluateMultipleChoiceQuestionComponent,
        ReEvaluateDragAndDropQuestionComponent,
        ReEvaluateShortAnswerQuestionComponent,
        QuizReEvaluateWarningComponent,
    ],
    exports: [QuizExerciseComponent],
})
export class ArtemisQuizManageModule {}
