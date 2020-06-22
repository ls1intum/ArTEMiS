import { NgModule } from '@angular/core';
import { ArtemisExampleModelingSolutionModule } from 'app/exercises/modeling/manage/example-modeling/example-modeling-solution.module';
import { ArtemisExampleModelingSubmissionModule } from 'app/exercises/modeling/manage/example-modeling/example-modeling-submission.module';
import { ArtemisModelingExerciseModule } from 'app/exercises/modeling/manage/modeling-exercise.module';
import { ArtemisModelingStatisticsModule } from 'app/exercises/modeling/manage/modeling-statistics/modeling-statistics.module';
import { ModelingExerciseImportComponent } from 'app/exercises/modeling/manage/modeling-exercise-import.component';
import { ArtemisSharedComponentModule } from 'app/shared/components/shared-component.module';
import { ArtemisSharedPipesModule } from 'app/shared/pipes/shared-pipes.module';
import { ArtemisSharedCommonModule } from 'app/shared/shared-common.module';

@NgModule({
    imports: [
        ArtemisExampleModelingSolutionModule,
        ArtemisExampleModelingSubmissionModule,
        ArtemisModelingExerciseModule,
        ArtemisModelingStatisticsModule,
        ArtemisSharedCommonModule,
        ArtemisSharedComponentModule,
        ArtemisSharedPipesModule,
    ],
    declarations: [ModelingExerciseImportComponent],
})
export class ArtemisModelingExerciseManagementModule {}
