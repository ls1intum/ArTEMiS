import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { JhiLanguageService } from 'ng-jhipster';
import { JhiLanguageHelper } from 'app/core';
import { ArTEMiSSharedModule } from '../../shared';
import { ArTEMiSQuizExerciseModule } from '../quiz-exercise/quiz-exercise.module';
import { ArTEMiSTextExerciseModule } from '../text-exercise/text-exercise.module';
import { ArTEMiSModelingExerciseModule } from '../modeling-exercise/modeling-exercise.module';
import { ArTEMiSFileUploadExerciseModule } from '../file-upload-exercise/file-upload-exercise.module';
import { ArTEMiSProgrammingExerciseModule } from '../programming-exercise/programming-exercise.module';

import {
    CourseComponent,
    CourseDeleteDialogComponent,
    CourseDeletePopupComponent,
    CourseDetailComponent,
    CourseExerciseService,
    coursePopupRoute,
    courseRoute,
    CourseService,
    CourseExercisesOverviewComponent,
    CourseUpdateComponent,
} from './';
import { CourseExerciseCardComponent } from 'app/entities/course/course-exercise-card.component';
import { FormDateTimePickerModule } from '../../shared/date-time-picker/date-time-picker.module';
import { ArTEMiSColorSelectorModule } from 'app/components/color-selector/color-selector.module';
import { ImageCropperModule } from 'ngx-image-cropper';
import { SortByModule } from 'app/components/pipes';

const ENTITY_STATES = [...courseRoute, ...coursePopupRoute];

@NgModule({
    imports: [
        ArTEMiSSharedModule,
        ArTEMiSProgrammingExerciseModule,
        ArTEMiSFileUploadExerciseModule,
        ArTEMiSQuizExerciseModule,
        ArTEMiSTextExerciseModule,
        ArTEMiSModelingExerciseModule,
        RouterModule.forChild(ENTITY_STATES),
        SortByModule,
        FormDateTimePickerModule,
        ReactiveFormsModule,
        ArTEMiSColorSelectorModule,
        ImageCropperModule,
    ],
    declarations: [
        CourseComponent,
        CourseDetailComponent,
        CourseDeleteDialogComponent,
        CourseUpdateComponent,
        CourseDeletePopupComponent,
        CourseExerciseCardComponent,
        CourseExercisesOverviewComponent,
    ],
    entryComponents: [CourseComponent, CourseUpdateComponent, CourseDeleteDialogComponent, CourseDeletePopupComponent, CourseExerciseCardComponent, CourseDeletePopupComponent],
    providers: [CourseService, CourseExerciseService, { provide: JhiLanguageService, useClass: JhiLanguageService }],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class ArTEMiSCourseModule {
    constructor(private languageService: JhiLanguageService, private languageHelper: JhiLanguageHelper) {
        this.languageHelper.language.subscribe((languageKey: string) => {
            if (languageKey !== undefined) {
                this.languageService.changeLanguage(languageKey);
            }
        });
    }
}
