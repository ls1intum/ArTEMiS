import { NgModule } from '@angular/core';

import { ArtemisSharedModule } from 'app/shared';
import { StudentQuestionRowComponent, StudentQuestionsComponent } from './';
import { ArtemisSidePanelModule } from 'app/components/side-panel/side-panel.module';
import { ArtemisConfirmIconModule } from 'app/components/confirm-icon/confirm-icon.module';

@NgModule({
    imports: [ArtemisSharedModule, ArtemisSidePanelModule, ArtemisConfirmIconModule],
    declarations: [StudentQuestionsComponent, StudentQuestionRowComponent],
    exports: [StudentQuestionsComponent],
})
export class ArtemisStudentQuestionsModule {}
