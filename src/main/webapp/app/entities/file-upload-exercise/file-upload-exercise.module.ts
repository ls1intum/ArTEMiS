import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { ArtemisSharedModule } from 'app/shared/shared.module';
import { FormDateTimePickerModule } from 'app/shared/date-time-picker/date-time-picker.module';
import { ArtemisCategorySelectorModule } from 'app/components/category-selector/category-selector.module';
import { ArtemisDifficultyPickerModule } from 'app/components/exercise/difficulty-picker/difficulty-picker.module';
import { ArtemisPresentationScoreModule } from 'app/components/exercise/presentation-score/presentation-score.module';
import { ArtemisAssessmentSharedModule } from 'app/assessment-shared/assessment-shared.module';
import { SortByModule } from 'app/components/pipes/sort-by.module';
import { ArtemisMarkdownEditorModule } from 'app/markdown-editor/markdown-editor.module';
import { FileUploadExerciseService } from 'app/entities/file-upload-exercise/file-upload-exercise.service';
import { fileUploadExerciseRoute } from 'app/entities/file-upload-exercise/file-upload-exercise.route';
import { FileUploadExerciseComponent } from 'app/entities/file-upload-exercise/file-upload-exercise.component';
import { FileUploadExerciseUpdateComponent } from 'app/entities/file-upload-exercise/file-upload-exercise-update.component';
import { FileUploadExerciseDetailComponent } from 'app/entities/file-upload-exercise/file-upload-exercise-detail.component';
import { ArtemisTeamConfigFormGroupModule } from 'app/components/exercise/team-config-form-group/team-config-form-group.module';

const ENTITY_STATES = [...fileUploadExerciseRoute];

@NgModule({
    imports: [
        ArtemisSharedModule,
        RouterModule.forChild(ENTITY_STATES),
        SortByModule,
        FormDateTimePickerModule,
        ArtemisCategorySelectorModule,
        ArtemisDifficultyPickerModule,
        ArtemisMarkdownEditorModule,
        ArtemisPresentationScoreModule,
        ArtemisAssessmentSharedModule,
        ArtemisTeamConfigFormGroupModule,
    ],
    declarations: [FileUploadExerciseComponent, FileUploadExerciseDetailComponent, FileUploadExerciseUpdateComponent],
    exports: [FileUploadExerciseComponent],
    providers: [FileUploadExerciseService],
})
export class ArtemisFileUploadExerciseModule {}
