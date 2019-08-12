import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';
import { JhiLanguageService } from 'ng-jhipster';
import { JhiLanguageHelper } from 'app/core';

import { ArTEMiSSharedModule } from 'app/shared';
import {
    LectureComponent,
    LectureDetailComponent,
    LectureUpdateComponent,
    LectureDeletePopupComponent,
    LectureDeleteDialogComponent,
    lectureRoute,
    lecturePopupRoute,
    LectureAttachmentsComponent,
} from './';
import { FormDateTimePickerModule } from 'app/shared/date-time-picker/date-time-picker.module';
import { ArTEMiSConfirmButtonModule } from 'app/components/confirm-button/confirm-button.module';

const ENTITY_STATES = [...lectureRoute, ...lecturePopupRoute];

@NgModule({
    imports: [ArTEMiSSharedModule, RouterModule.forChild(ENTITY_STATES), FormDateTimePickerModule, ArTEMiSConfirmButtonModule],
    declarations: [LectureComponent, LectureDetailComponent, LectureUpdateComponent, LectureDeleteDialogComponent, LectureAttachmentsComponent, LectureDeletePopupComponent],
    entryComponents: [LectureComponent, LectureUpdateComponent, LectureDeleteDialogComponent, LectureDeletePopupComponent, LectureAttachmentsComponent],
    providers: [{ provide: JhiLanguageService, useClass: JhiLanguageService }],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class ArtemisLectureModule {
    constructor(private languageService: JhiLanguageService, private languageHelper: JhiLanguageHelper) {
        this.languageHelper.language.subscribe((languageKey: string) => {
            if (languageKey) {
                this.languageService.changeLanguage(languageKey);
            }
        });
    }
}
