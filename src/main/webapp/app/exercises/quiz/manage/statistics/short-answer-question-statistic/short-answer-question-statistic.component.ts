import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { QuizStatisticUtil } from 'app/exercises/quiz/shared/quiz-statistic-util.service';
import { ShortAnswerQuestionUtil } from 'app/exercises/quiz/shared/short-answer-question-util.service';
import { ArtemisMarkdownService } from 'app/shared/markdown.service';
import { AccountService } from 'app/core/auth/account.service';
import { JhiWebsocketService } from 'app/core/websocket/websocket.service';
import { ShortAnswerQuestion } from 'app/entities/quiz/short-answer-question.model';
import { QuizExerciseService } from 'app/exercises/quiz/manage/quiz-exercise.service';
import { ShortAnswerSpot } from 'app/entities/quiz/short-answer-spot.model';
import { ShortAnswerQuestionStatistic } from 'app/entities/quiz/short-answer-question-statistic.model';
import { ShortAnswerSolution } from 'app/entities/quiz/short-answer-solution.model';
import { QuizExercise } from 'app/entities/quiz/quiz-exercise.model';
import { QuestionStatisticComponent } from 'app/exercises/quiz/manage/statistics/drag-and-drop-question-statistic/drag-and-drop-question-statistic.component';

@Component({
    selector: 'jhi-short-answer-question-statistic',
    templateUrl: './short-answer-question-statistic.component.html',
    providers: [QuizStatisticUtil, ShortAnswerQuestionUtil, ArtemisMarkdownService],
    styleUrls: ['./short-answer-question-statistic.component.scss'],
})
export class ShortAnswerQuestionStatisticComponent extends QuestionStatisticComponent implements OnInit, OnDestroy {
    question: ShortAnswerQuestion;
    questionStatistic: ShortAnswerQuestionStatistic;

    textParts: string[][];
    lettersForSolutions: number[] = [];

    sampleSolutions: ShortAnswerSolution[] = [];

    constructor(
        route: ActivatedRoute,
        router: Router,
        accountService: AccountService,
        translateService: TranslateService,
        quizExerciseService: QuizExerciseService,
        jhiWebsocketService: JhiWebsocketService,
        quizStatisticUtil: QuizStatisticUtil,
        public shortAnswerQuestionUtil: ShortAnswerQuestionUtil,
        private artemisMarkdown: ArtemisMarkdownService,
    ) {
        super(route, router, accountService, translateService, quizExerciseService, jhiWebsocketService);
    }

    ngOnInit() {
        super.init();
    }

    ngOnDestroy() {
        super.destroy();
    }

    /**
     * This functions loads the Quiz, which is necessary to build the Web-Template
     *
     * @param {QuizExercise} quiz: the quizExercise, which the selected question is part of.
     * @param {boolean} refresh: true if method is called from Websocket
     */
    loadQuiz(quiz: QuizExercise, refresh: boolean) {
        const updatedQuestion = super.loadQuizCommon(quiz);
        this.question = updatedQuestion as ShortAnswerQuestion;
        this.questionStatistic = this.question.quizQuestionStatistic as ShortAnswerQuestionStatistic;

        // load Layout only at the opening (not if the websocket refreshed the data)
        if (!refresh) {
            this.questionTextRendered = this.artemisMarkdown.safeHtmlForMarkdown(this.question.text);
            this.generateShortAnswerStructure();
            this.generateLettersForSolutions();

            this.loadLayout();
        }
        this.loadData();

        this.sampleSolutions = this.shortAnswerQuestionUtil.getSampleSolution(this.question);
    }

    generateShortAnswerStructure() {
        const textParts = this.shortAnswerQuestionUtil.divideQuestionTextIntoTextParts(this.question.text!);
        this.textParts = this.shortAnswerQuestionUtil.transformTextPartsIntoHTML(textParts, this.artemisMarkdown);
    }

    generateLettersForSolutions() {
        for (const mapping of this.question.correctMappings || []) {
            for (const i in this.question.spots) {
                if (mapping.spot!.id === this.question.spots[i].id) {
                    this.lettersForSolutions.push(+i);
                    break;
                }
            }
        }
    }

    getSampleSolutionForSpot(spotTag: string): ShortAnswerSolution {
        const index = this.question.spots!.findIndex((spot) => spot.spotNr === this.shortAnswerQuestionUtil.getSpotNr(spotTag));
        return this.sampleSolutions[index];
    }

    /**
     * build the Chart-Layout based on the the Json-entity (questionStatistic)
     */
    loadLayout() {
        // reset old data
        this.labels = [];
        this.backgroundColors = [];
        this.backgroundSolutionColors = [];

        // set label and backgroundcolor based on the spots
        this.question.spots!.forEach((spot, i) => {
            this.labels.push(String.fromCharCode(65 + i) + '.');
            this.backgroundColors.push(this.getBackgroundColor('#428bca'));
            this.backgroundSolutionColors.push(this.getBackgroundColor('#5cb85c'));
        });

        this.addLastBarLayout();
        this.loadInvalidLayout();
    }

    /**
     * add Layout for the last bar
     */
    addLastBarLayout() {
        // add Color for last bar
        this.backgroundColors.push(this.getBackgroundColor('#5bc0de'));
        this.backgroundSolutionColors[this.question.spots!.length] = this.getBackgroundColor('#5bc0de');

        // add Text for last label based on the language
        this.translateService.get('showStatistic.quizStatistic.yAxes').subscribe((lastLabel) => {
            this.labels[this.question.spots!.length] = lastLabel.split(' ');
            this.chartLabels.length = 0;
            for (let i = 0; i < this.labels.length; i++) {
                this.chartLabels.push(this.labels[i]);
            }
        });
    }

    /**
     * change label and Color if a spot is invalid
     */
    loadInvalidLayout() {
        // set Background for invalid answers = grey
        this.translateService.get('showStatistic.invalid').subscribe((invalidLabel) => {
            this.question.spots!.forEach((spot, i) => {
                if (spot.invalid) {
                    this.backgroundColors[i] = this.getBackgroundColor('#838383');
                    this.backgroundSolutionColors[i] = this.getBackgroundColor('#838383');
                    // add 'invalid' to bar-Label
                    this.labels[i] = String.fromCharCode(65 + i) + '. ' + invalidLabel;
                }
            });
        });
    }

    /**
     * load the Data from the Json-entity to the chart: myChart
     */
    loadData() {
        // reset old data
        this.ratedData = [];
        this.unratedData = [];

        // set data based on the spots for each spot
        this.question.spots!.forEach((spot) => {
            const spotCounter = this.questionStatistic.shortAnswerSpotCounters?.find((sCounter) => {
                return spot.id === sCounter.spot?.id;
            })!;
            this.ratedData.push(spotCounter.ratedCounter!);
            this.unratedData.push(spotCounter.unRatedCounter!);
        });
        // add data for the last bar (correct Solutions)
        this.ratedCorrectData = this.questionStatistic.ratedCorrectCounter!;
        this.unratedCorrectData = this.questionStatistic.unRatedCorrectCounter!;
        this.chartLabels.length = 0;
        for (let i = 0; i < this.labels.length; i++) {
            this.chartLabels.push(this.labels[i]);
        }

        this.loadDataInDiagram();
    }

    /**
     * switch between showing and hiding the solution in the chart
     */
    switchRated() {
        this.rated = !this.rated;
        this.loadDataInDiagram();
    }

    /**
     * switch between showing and hiding the solution in the chart
     *  1. change the bar-Labels
     */
    switchSolution() {
        this.showSolution = !this.showSolution;
        this.loadDataInDiagram();
    }

    /**
     * converts a number in a letter (0 -> A, 1 -> B, ...)
     *
     * @param index the given number
     */
    getLetter(index: number) {
        return String.fromCharCode(65 + (index - 1));
    }

    /**
     * Get the solution that was mapped to the given spot in the sample solution
     *
     * @param spot {object} the spot that the solution should be mapped to
     * @return the mapped solution or undefined if no solution has been mapped to this location
     */
    correctSolutionForSpot(spot: ShortAnswerSpot) {
        const currMapping = this.shortAnswerQuestionUtil.solveShortAnswer(this.question).filter((mapping) => mapping.spot!.id === spot.id)[0];
        if (currMapping) {
            return currMapping.solution;
        } else {
            return undefined;
        }
    }
}
