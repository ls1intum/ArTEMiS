import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { RouterModule } from '@angular/router';
import { JhiLanguageService } from 'ng-jhipster';
import { JhiLanguageHelper } from 'app/core';

import { ArTEMiSSharedModule } from '../shared';
import { tutorExerciseDashboardRoute } from './tutor-exercise-dashboard.route';
import { CourseComponent, CourseExerciseService, CourseScoreCalculationService, CourseService } from '../entities/course';
import { JhiAlertService } from 'ng-jhipster';
import { TutorExerciseDashboardComponent } from './tutor-exercise-dashboard.component';
import { RepositoryService } from 'app/entities/repository';
import { ArTEMiSResultModule, ResultComponent, ResultService } from '../entities/result';
import { HomeComponent } from '../home';
import { ParticipationService } from '../entities/participation';
import { MomentModule } from 'angular2-moment';
import { JhiMainComponent } from '../layouts';
import { ClipboardModule } from 'ngx-clipboard';
import { ArTEMiSTutorCourseDashboardModule } from 'app/tutor-course-dashboard';

const ENTITY_STATES = [...tutorExerciseDashboardRoute];

@NgModule({
    imports: [BrowserModule, ArTEMiSSharedModule, ArTEMiSResultModule, MomentModule, ClipboardModule, RouterModule.forChild(ENTITY_STATES), ArTEMiSTutorCourseDashboardModule],
    declarations: [TutorExerciseDashboardComponent],
    exports: [ResultComponent],
    entryComponents: [HomeComponent, CourseComponent, JhiMainComponent, ResultComponent],
    providers: [
        CourseService,
        JhiAlertService,
        RepositoryService,
        ResultService,
        CourseExerciseService,
        ParticipationService,
        CourseScoreCalculationService,
        { provide: JhiLanguageService, useClass: JhiLanguageService }
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class ArTEMiSTutorExerciseDashboardModule {
    constructor(private languageService: JhiLanguageService, private languageHelper: JhiLanguageHelper) {
        this.languageHelper.language.subscribe((languageKey: string) => {
            if (languageKey !== undefined) {
                this.languageService.changeLanguage(languageKey);
            }
        });
    }
}
