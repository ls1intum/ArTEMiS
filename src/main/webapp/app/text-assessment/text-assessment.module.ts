import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { JhiLanguageService } from 'ng-jhipster';
import { JhiLanguageHelper } from 'app/core';

import { textAssessmentRoutes } from './text-assessment.route';
import { TextAssessmentComponent } from './text-assessment.component';
import { TextSelectDirective } from './text-assessment-editor/text-select.directive';
import { TextAssessmentEditorComponent } from './text-assessment-editor/text-assessment-editor.component';
import { ArTEMiSSharedModule } from 'app/shared';
import { ArTEMiSResultModule } from 'app/entities/result';
import { TextAssessmentDetailComponent } from './text-assessment-detail/text-assessment-detail.component';
import { TextAssessmentDashboardComponent } from './text-assessment-dashboard/text-assessment-dashboard.component';
import { SortByModule } from 'app/components/pipes';
import { RouterModule } from '@angular/router';
import { ResizableInstructionsComponent } from 'app/text-assessment/resizable-instructions/resizable-instructions.component';
import { ArTEMiSComplaintsForTutorModule } from 'app/complaints-for-tutor';
import { HighlightedTextAreaComponent } from 'app/text-assessment/highlighted-text-area/highlighted-text-area.component';

const ENTITY_STATES = [...textAssessmentRoutes];
@NgModule({
    imports: [CommonModule, SortByModule, RouterModule.forChild(ENTITY_STATES), ArTEMiSSharedModule, ArTEMiSResultModule, ArTEMiSComplaintsForTutorModule],
    declarations: [
        TextAssessmentComponent,
        TextSelectDirective,
        TextAssessmentEditorComponent,
        TextAssessmentDetailComponent,
        TextAssessmentDashboardComponent,
        ResizableInstructionsComponent,
        HighlightedTextAreaComponent,
    ],
    exports: [TextAssessmentEditorComponent, TextAssessmentDetailComponent, ResizableInstructionsComponent],
    providers: [{ provide: JhiLanguageService, useClass: JhiLanguageService }],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class ArTEMiSTextAssessmentModule {
    constructor(private languageService: JhiLanguageService, private languageHelper: JhiLanguageHelper) {
        this.languageHelper.language.subscribe((languageKey: string) => {
            if (languageKey) {
                this.languageService.changeLanguage(languageKey);
            }
        });
    }
}
