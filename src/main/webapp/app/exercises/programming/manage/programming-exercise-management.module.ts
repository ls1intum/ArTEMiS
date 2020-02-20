import { NgModule } from '@angular/core';
import { ArtemisSharedModule } from 'app/shared/shared.module';
import { ArtemisProgrammingExerciseManagementRoutingModule } from 'app/exercises/programming/manage/programming-exercise-management-routing.module';
import { OrionModule } from 'app/shared/orion/orion.module';
import { FeatureToggleModule } from 'app/shared/feature-toggle/feature-toggle.module';
import { ArtemisProgrammingExerciseTestCaseModule } from 'app/exercises/programming/manage/test-cases/programming-exercise-test-case.module';
import { ArtemisProgrammingExerciseStatusModule } from 'app/exercises/programming/manage/status/programming-exercise-status.module';
import { ProgrammingExerciseUtilsModule } from 'app/exercises/programming/manage/utils/programming-exercise-utils.module';
import { SortByModule } from 'app/shared/pipes/sort-by.module';
import { ProgrammingExerciseComponent } from 'app/exercises/programming/manage/programming-exercise.component';
import { ProgrammingExerciseDetailComponent } from 'app/exercises/programming/manage/programming-exercise-detail.component';
import { ArtemisAssessmentSharedModule } from 'app/assessment/assessment-shared.module';
import { ArtemisProgrammingAssessmentModule } from 'app/exercises/programming/assess/programming-assessment/programming-assessment.module';
import { ArtemisResultModule } from 'app/exercises/shared/result/result.module';
import { ArtemisProgrammingExerciseActionsModule } from 'app/exercises/programming/manage/actions/programming-exercise-actions.module';
import { ArtemisProgrammingExerciseInstructionsEditorModule } from 'app/exercises/programming/manage/instructions/instructions-editor/programming-exercise-instructions-editor.module';
import { ArtemisProgrammingExerciseUpdateModule } from 'app/exercises/programming/manage/update/programming-exercise-update.module';

@NgModule({
    imports: [
        ArtemisSharedModule,
        SortByModule,
        OrionModule,
        FeatureToggleModule,
        ProgrammingExerciseUtilsModule,
        ArtemisResultModule,
        ArtemisAssessmentSharedModule,
        ArtemisProgrammingExerciseManagementRoutingModule,
        ArtemisProgrammingExerciseTestCaseModule,
        ArtemisProgrammingExerciseStatusModule,
        ArtemisProgrammingAssessmentModule,
        ArtemisProgrammingExerciseActionsModule,
        ArtemisProgrammingExerciseInstructionsEditorModule,
        ArtemisProgrammingExerciseUpdateModule,
    ],
    declarations: [ProgrammingExerciseComponent, ProgrammingExerciseDetailComponent],
    exports: [ProgrammingExerciseComponent],
})
export class ArtemisProgrammingExerciseManagementModule {}
