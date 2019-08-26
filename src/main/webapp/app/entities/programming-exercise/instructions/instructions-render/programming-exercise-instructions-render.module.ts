import { NgModule } from '@angular/core';
import { JhiLanguageService } from 'ng-jhipster';
import { JhiLanguageHelper } from 'app/core/language/language.helper';
import {
    ProgrammingExerciseInstructionComponent,
    ProgrammingExerciseInstructionResultDetailComponent,
    ProgrammingExerciseInstructionService,
    ProgrammingExerciseInstructionStepWizardComponent,
    ProgrammingExerciseInstructionTaskStatusComponent,
    ProgrammingExercisePlantUmlExtensionWrapper,
    ProgrammingExercisePlantUmlService,
    ProgrammingExerciseTaskExtensionWrapper,
} from './';
import { ArtemisSharedModule } from 'app/shared';
import { ArtemisMarkdownEditorModule } from 'app/markdown-editor';

@NgModule({
    imports: [ArtemisSharedModule, ArtemisMarkdownEditorModule],
    declarations: [
        ProgrammingExerciseInstructionComponent,
        ProgrammingExerciseInstructionStepWizardComponent,
        ProgrammingExerciseInstructionTaskStatusComponent,
        ProgrammingExerciseInstructionResultDetailComponent,
    ],
    entryComponents: [ProgrammingExerciseInstructionComponent, ProgrammingExerciseInstructionResultDetailComponent, ProgrammingExerciseInstructionTaskStatusComponent],
    exports: [ProgrammingExerciseInstructionComponent],
    providers: [ProgrammingExerciseTaskExtensionWrapper, ProgrammingExercisePlantUmlExtensionWrapper, ProgrammingExerciseInstructionService, ProgrammingExercisePlantUmlService],
})
export class ArtemisProgrammingExerciseInstructionsRenderModule {
    constructor(private languageService: JhiLanguageService, private languageHelper: JhiLanguageHelper) {
        this.languageHelper.language.subscribe((languageKey: string) => {
            if (languageKey) {
                this.languageService.changeLanguage(languageKey);
            }
        });
    }
}
