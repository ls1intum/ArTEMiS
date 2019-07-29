import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { JhiLanguageService } from 'ng-jhipster';
import { JhiLanguageHelper } from 'app/core';
import { NgxDatatableModule } from '@swimlane/ngx-datatable';

import { ArTEMiSSharedModule } from 'app/shared';
import {
    ProgrammingExerciseComponent,
    ProgrammingExerciseDeleteDialogComponent,
    ProgrammingExerciseDeletePopupComponent,
    ProgrammingExerciseDetailComponent,
    ProgrammingExerciseDialogComponent,
    ProgrammingExercisePopupComponent,
    ProgrammingExerciseInstructionComponent,
    ProgrammingExerciseEditableInstructionComponent,
    programmingExercisePopupRoute,
    ProgrammingExercisePopupService,
    programmingExerciseRoute,
    ProgrammingExerciseService,
    ProgrammingExerciseUpdateComponent,
    ProgrammingExerciseInstructorStatusComponent,
    ProgrammingExerciseInstructionTestcaseStatusComponent,
    ProgrammingExerciseTestCaseService,
    ProgrammingExerciseParticipationService,
    ProgrammingExerciseManageTestCasesComponent,
} from './';
import { ArTEMiSMarkdownEditorModule } from 'app/markdown-editor';
import { SortByModule } from 'app/components/pipes';
import { FormDateTimePickerModule } from 'app/shared/date-time-picker/date-time-picker.module';
import { ArTEMiSCategorySelectorModule } from 'app/components/category-selector/category-selector.module';
import { ArTEMiSDifficultyPickerModule } from 'app/components/exercise/difficulty-picker/difficulty-picker.module';
import { ArTEMiSResultModule } from 'app/entities/result';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { ProgrammingExerciseInstructorExerciseStatusComponent } from 'app/entities/programming-exercise/status/programming-exercise-instructor-exercise-status.component';

const ENTITY_STATES = [...programmingExerciseRoute, ...programmingExercisePopupRoute];

@NgModule({
    imports: [
        ArTEMiSSharedModule,
        RouterModule.forChild(ENTITY_STATES),
        SortByModule,
        FormDateTimePickerModule,
        ArTEMiSCategorySelectorModule,
        ArTEMiSDifficultyPickerModule,
        ArTEMiSResultModule,
        ArTEMiSMarkdownEditorModule,
        NgxDatatableModule,
    ],
    declarations: [
        ProgrammingExerciseComponent,
        ProgrammingExerciseDetailComponent,
        ProgrammingExerciseUpdateComponent,
        ProgrammingExerciseDialogComponent,
        ProgrammingExerciseDeleteDialogComponent,
        ProgrammingExercisePopupComponent,
        ProgrammingExerciseDeletePopupComponent,
        ProgrammingExerciseInstructionComponent,
        ProgrammingExerciseEditableInstructionComponent,
        ProgrammingExerciseInstructorStatusComponent,
        ProgrammingExerciseInstructorExerciseStatusComponent,
        ProgrammingExerciseInstructionTestcaseStatusComponent,
        ProgrammingExerciseManageTestCasesComponent,
    ],
    entryComponents: [
        ProgrammingExerciseComponent,
        ProgrammingExerciseDialogComponent,
        ProgrammingExerciseUpdateComponent,
        ProgrammingExercisePopupComponent,
        ProgrammingExerciseDeleteDialogComponent,
        ProgrammingExerciseDeletePopupComponent,
        ProgrammingExerciseInstructorStatusComponent,
        ProgrammingExerciseInstructionComponent,
        ProgrammingExerciseInstructorExerciseStatusComponent,
        ProgrammingExerciseEditableInstructionComponent,
        FaIconComponent,
    ],
    exports: [
        ProgrammingExerciseComponent,
        ProgrammingExerciseInstructionComponent,
        ProgrammingExerciseEditableInstructionComponent,
        ProgrammingExerciseInstructorExerciseStatusComponent,
    ],
    providers: [ProgrammingExerciseService, ProgrammingExerciseTestCaseService, ProgrammingExercisePopupService, ProgrammingExerciseParticipationService],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class ArTEMiSProgrammingExerciseModule {
    constructor(private languageService: JhiLanguageService, private languageHelper: JhiLanguageHelper) {
        this.languageHelper.language.subscribe((languageKey: string) => {
            if (languageKey !== undefined) {
                this.languageService.changeLanguage(languageKey);
            }
        });
    }
}
