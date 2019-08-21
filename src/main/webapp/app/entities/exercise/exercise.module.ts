import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { JhiLanguageService } from 'ng-jhipster';
import { JhiLanguageHelper } from 'app/core';

import { ArtemisSharedModule } from '../../shared';
import {
    ExerciseLtiConfigurationDialogComponent,
    ExerciseLtiConfigurationPopupComponent,
    ExerciseLtiConfigurationService,
    exercisePopupRoute,
    ExercisePopupService,
    ExerciseResetDialogComponent,
    ExerciseResetPopupComponent,
    ExerciseService,
} from './';

const ENTITY_STATES = [...exercisePopupRoute];

@NgModule({
    imports: [ArtemisSharedModule, RouterModule.forChild(ENTITY_STATES)],
    declarations: [ExerciseLtiConfigurationDialogComponent, ExerciseLtiConfigurationPopupComponent, ExerciseResetDialogComponent, ExerciseResetPopupComponent],
    entryComponents: [ExerciseLtiConfigurationDialogComponent, ExerciseLtiConfigurationPopupComponent, ExerciseResetDialogComponent, ExerciseResetPopupComponent],
    providers: [ExercisePopupService, ExerciseService, ExerciseLtiConfigurationService, { provide: JhiLanguageService, useClass: JhiLanguageService }],
})
export class ArtemisExerciseModule {
    constructor(private languageService: JhiLanguageService, private languageHelper: JhiLanguageHelper) {
        this.languageHelper.language.subscribe((languageKey: string) => {
            if (languageKey) {
                this.languageService.changeLanguage(languageKey);
            }
        });
    }
}
