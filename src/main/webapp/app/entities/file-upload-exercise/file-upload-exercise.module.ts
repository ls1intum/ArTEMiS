import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { ArTEMiSSharedModule } from '../../shared';
import {
    FileUploadExerciseComponent,
    FileUploadExerciseDeleteDialogComponent,
    FileUploadExerciseDeletePopupComponent,
    FileUploadExerciseDetailComponent,
    FileUploadExerciseDialogComponent,
    FileUploadExercisePopupComponent,
    fileUploadExercisePopupRoute,
    FileUploadExercisePopupService,
    fileUploadExerciseRoute,
    FileUploadExerciseService,
    FileUploadExerciseUpdateComponent
} from './';
import { SortByModule } from '../../components/pipes';

const ENTITY_STATES = [...fileUploadExerciseRoute, ...fileUploadExercisePopupRoute];

@NgModule({
    imports: [ArTEMiSSharedModule, RouterModule.forChild(ENTITY_STATES), SortByModule],
    declarations: [
        FileUploadExerciseComponent,
        FileUploadExerciseDetailComponent,
        FileUploadExerciseUpdateComponent,
        FileUploadExerciseDialogComponent,
        FileUploadExerciseDeleteDialogComponent,
        FileUploadExercisePopupComponent,
        FileUploadExerciseDeletePopupComponent
    ],
    entryComponents: [
        FileUploadExerciseComponent,
        FileUploadExerciseUpdateComponent,
        FileUploadExerciseDialogComponent,
        FileUploadExercisePopupComponent,
        FileUploadExerciseDeleteDialogComponent,
        FileUploadExerciseDeletePopupComponent
    ],
    providers: [FileUploadExerciseService, FileUploadExercisePopupService],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class ArTEMiSFileUploadExerciseModule {}
