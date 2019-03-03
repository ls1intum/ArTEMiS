import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { JhiLanguageService } from 'ng-jhipster';
import { JhiLanguageHelper } from 'app/core';

import { ArTEMiSSharedModule } from '../shared';
import { quizStatisticRoute } from './quiz-statistic.route';
import { HomeComponent } from '../home';
import { JhiMainComponent } from '../layouts';
import { QuizStatisticComponent } from './quiz-statistic/quiz-statistic.component';
import { ChartsModule } from 'ng2-charts';
import { QuizPointStatisticComponent } from './quiz-point-statistic/quiz-point-statistic.component';
import { QuizStatisticUtil } from '../components/util/quiz-statistic-util.service';
import { MultipleChoiceQuestionStatisticComponent } from './multiple-choice-question-statistic/multiple-choice-question-statistic.component';
import { DragAndDropQuestionStatisticComponent } from './drag-and-drop-question-statistic/drag-and-drop-question-statistic.component';
import { ShortAnswerQuestionStatisticComponent } from './short-answer-question-statistic/short-answer-question-statistic.component';
import { ArTEMiSQuizModule } from '../quiz/participate/quiz.module';

const ENTITY_STATES = [
    ...quizStatisticRoute
];

@NgModule({
    imports: [
        ArTEMiSSharedModule,
        RouterModule.forChild(ENTITY_STATES),
        ChartsModule,
        ArTEMiSQuizModule
    ],
    declarations: [
        QuizStatisticComponent,
        QuizPointStatisticComponent,
        MultipleChoiceQuestionStatisticComponent,
        DragAndDropQuestionStatisticComponent,
        ShortAnswerQuestionStatisticComponent
    ],
    entryComponents: [
        HomeComponent,
        JhiMainComponent,
        QuizStatisticComponent,
        QuizPointStatisticComponent,
        MultipleChoiceQuestionStatisticComponent,
        DragAndDropQuestionStatisticComponent,
        ShortAnswerQuestionStatisticComponent
    ],
    providers: [
        QuizStatisticUtil,
        { provide: JhiLanguageService, useClass: JhiLanguageService }
    ]
})
export class ArTEMiSStatisticModule {
    constructor(private languageService: JhiLanguageService, private languageHelper: JhiLanguageHelper) {
        this.languageHelper.language.subscribe((languageKey: string) => {
            if (languageKey !== undefined) {
                this.languageService.changeLanguage(languageKey);
            }
        });
    }
}
