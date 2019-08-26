import { NgModule } from '@angular/core';
import { JhiAlertService, JhiLanguageService } from 'ng-jhipster';
import { RouterModule } from '@angular/router';
import { MomentModule } from 'ngx-moment';
import { AceEditorModule } from 'ng2-ace-editor';
import { TreeviewModule } from 'ngx-treeview';

import { JhiLanguageHelper } from 'app/core';
import { codeEditorRoute } from './code-editor.route';
import { ArtemisResultModule } from 'app/entities/result';

import {
    CodeEditorAceComponent,
    CodeEditorActionsComponent,
    CodeEditorBuildLogService,
    CodeEditorBuildOutputComponent,
    CodeEditorConflictStateService,
    CodeEditorFileBrowserComponent,
    CodeEditorFileBrowserCreateNodeComponent,
    CodeEditorFileBrowserDeleteComponent,
    CodeEditorFileBrowserFileComponent,
    CodeEditorFileBrowserFolderComponent,
    CodeEditorFileService,
    CodeEditorGridComponent,
    CodeEditorGridService,
    CodeEditorInstructionsComponent,
    CodeEditorInstructorContainerComponent,
    CodeEditorRepositoryFileService,
    CodeEditorRepositoryService,
    CodeEditorResolveConflictModalComponent,
    CodeEditorSessionService,
    CodeEditorStatusComponent,
    CodeEditorStudentContainerComponent,
    CodeEditorSubmissionService,
    DomainService,
} from './';
import { ArtemisExerciseHintModule } from 'app/entities/exercise-hint/exercise-hint.module';
import { ExerciseHintStudentDialogComponent } from 'app/entities/exercise-hint';
import { ArtemisSharedModule } from 'app/shared';
import { ArtemisProgrammingExerciseInstructionsEditorModule } from 'app/entities/programming-exercise/instructions/instructions-editor';
import { ArtemisProgrammingExerciseStatusModule } from 'app/entities/programming-exercise/status';
import { ArtemisProgrammingExerciseActionsModule } from 'app/entities/programming-exercise/actions/programming-exercise-actions.module';

const ENTITY_STATES = [...codeEditorRoute];

@NgModule({
    imports: [
        RouterModule.forChild(ENTITY_STATES),
        AceEditorModule,
        MomentModule,
        ArtemisSharedModule,
        ArtemisResultModule,
        ArtemisProgrammingExerciseInstructionsEditorModule,
        ArtemisProgrammingExerciseStatusModule,
        ArtemisProgrammingExerciseActionsModule,
        TreeviewModule.forRoot(),
        ArtemisExerciseHintModule,
    ],
    declarations: [
        CodeEditorGridComponent,
        CodeEditorInstructorContainerComponent,
        CodeEditorStudentContainerComponent,
        CodeEditorFileBrowserComponent,
        CodeEditorFileBrowserDeleteComponent,
        CodeEditorFileBrowserFileComponent,
        CodeEditorFileBrowserFolderComponent,
        CodeEditorFileBrowserCreateNodeComponent,
        CodeEditorAceComponent,
        CodeEditorBuildOutputComponent,
        CodeEditorInstructionsComponent,
        CodeEditorStatusComponent,
        CodeEditorActionsComponent,
        CodeEditorResolveConflictModalComponent,
    ],
    exports: [CodeEditorInstructorContainerComponent, CodeEditorStudentContainerComponent],
    entryComponents: [
        CodeEditorInstructorContainerComponent,
        CodeEditorStudentContainerComponent,
        CodeEditorFileBrowserDeleteComponent,
        ExerciseHintStudentDialogComponent,
        CodeEditorResolveConflictModalComponent,
    ],
    providers: [
        JhiAlertService,
        DomainService,
        CodeEditorRepositoryService,
        CodeEditorRepositoryFileService,
        CodeEditorBuildLogService,
        CodeEditorSessionService,
        CodeEditorFileService,
        CodeEditorGridService,
        CodeEditorConflictStateService,
        CodeEditorSubmissionService,
        { provide: JhiLanguageService, useClass: JhiLanguageService },
    ],
})
export class ArtemisCodeEditorModule {
    constructor(private languageService: JhiLanguageService, private languageHelper: JhiLanguageHelper) {
        this.languageHelper.language.subscribe((languageKey: string) => {
            if (languageKey) {
                this.languageService.changeLanguage(languageKey);
            }
        });
    }
}
