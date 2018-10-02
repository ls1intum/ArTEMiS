import { Routes } from '@angular/router';

import { UserRouteAccessService } from '../core';
import { QuizStatisticComponent } from './quiz-statistic/quiz-statistic.component';
import { QuizPointStatisticComponent } from './quiz-point-statistic/quiz-point-statistic.component';
import { MultipleChoiceQuestionStatisticComponent } from './multiple-choice-question-statistic/multiple-choice-question-statistic.component';
import { DragAndDropQuestionStatisticComponent } from './drag-and-drop-question-statistic/drag-and-drop-question-statistic.component';

export const statisticRoute: Routes = [
    {
        path: 'quiz/:quizId/quiz-statistic',
        component: QuizStatisticComponent,
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'arTeMiSApp.course.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'quiz/:quizId/quiz-point-statistic',
        component: QuizPointStatisticComponent,
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'arTeMiSApp.course.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'quiz/:quizId/multiple-choice-question-statistic/:questionId',
        component: MultipleChoiceQuestionStatisticComponent,
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'arTeMiSApp.course.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'quiz/:quizId/drag-and-drop-question-statistic/:questionId',
        component: DragAndDropQuestionStatisticComponent,
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'arTeMiSApp.course.home.title'
        },
        canActivate: [UserRouteAccessService]
    }
];
