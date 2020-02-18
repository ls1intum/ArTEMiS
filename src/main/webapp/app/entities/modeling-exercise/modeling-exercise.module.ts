import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ArtemisSharedModule } from 'app/shared/shared.module';
import { FormDateTimePickerModule } from 'app/shared/date-time-picker/date-time-picker.module';
import { ArtemisCategorySelectorModule } from 'app/components/category-selector/category-selector.module';
import { ArtemisDifficultyPickerModule } from 'app/components/exercise/difficulty-picker/difficulty-picker.module';
import { DeleteDialogComponent } from 'app/shared/delete-dialog/delete-dialog.component';
import { ArtemisPresentationScoreModule } from 'app/components/exercise/presentation-score/presentation-score.module';
import { ArtemisAssessmentSharedModule } from 'app/assessment-shared/assessment-shared.module';
import { SortByModule } from 'app/components/pipes/sort-by.module';
import { ArtemisMarkdownEditorModule } from 'app/markdown-editor/markdown-editor.module';
import { ArtemisModelingEditorModule } from 'app/modeling-editor/modeling-editor.module';
import { ModelingExerciseUpdateComponent } from 'app/entities/modeling-exercise/modeling-exercise-update.component';
import { modelingExerciseRoute } from 'app/entities/modeling-exercise/modeling-exercise.route';
import { ModelingExerciseComponent } from 'app/entities/modeling-exercise/modeling-exercise.component';
import { ModelingExerciseService } from 'app/entities/modeling-exercise/modeling-exercise.service';
import { ModelingExerciseDetailComponent } from 'app/entities/modeling-exercise/modeling-exercise-detail.component';
import { ArtemisTeamConfigFormGroupModule } from 'app/components/exercise/team-config-form-group/team-config-form-group.module';

const ENTITY_STATES = [...modelingExerciseRoute];

@NgModule({
    imports: [
        ArtemisSharedModule,
        RouterModule.forChild(ENTITY_STATES),
        SortByModule,
        FormDateTimePickerModule,
        ArtemisMarkdownEditorModule,
        ArtemisCategorySelectorModule,
        ArtemisDifficultyPickerModule,
        ArtemisPresentationScoreModule,
        ArtemisModelingEditorModule,
        ArtemisAssessmentSharedModule,
        ArtemisTeamConfigFormGroupModule,
    ],
    declarations: [ModelingExerciseComponent, ModelingExerciseDetailComponent, ModelingExerciseUpdateComponent],
    entryComponents: [ModelingExerciseComponent, DeleteDialogComponent],
    providers: [ModelingExerciseService],
    exports: [ModelingExerciseComponent],
})
export class ArtemisModelingExerciseModule {}
