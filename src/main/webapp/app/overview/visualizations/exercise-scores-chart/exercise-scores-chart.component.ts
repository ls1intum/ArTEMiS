import { AfterViewInit, Component, ElementRef, Input, OnDestroy, ViewChild } from '@angular/core';
import * as Chart from 'chart.js';
import { ChartDataSets, ChartOptions, ChartPoint, ChartType } from 'chart.js';
import { BaseChartDirective, Color, Label } from 'ng2-charts';
import { ExerciseScoresChartService, ExerciseScoresDTO } from 'app/overview/visualizations/exercise-scores-chart.service';
import { JhiAlertService } from 'ng-jhipster';
import { onError } from 'app/shared/util/global.utils';
import { finalize } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import * as _ from 'lodash';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'jhi-exercise-scores-chart',
    templateUrl: './exercise-scores-chart.component.html',
    styleUrls: ['./exercise-scores-chart.component.scss'],
})
export class ExerciseScoresChartComponent implements AfterViewInit, OnDestroy {
    @Input()
    courseId: number;
    isLoading = false;
    public exerciseScores: ExerciseScoresDTO[] = [];

    @ViewChild(BaseChartDirective)
    chartDirective: BaseChartDirective;
    chartInstance: Chart;
    @ViewChild('chartDiv')
    chartDiv: ElementRef;
    public lineChartData: ChartDataSets[] = [
        {
            fill: false,
            data: [],
            label: this.translateService.instant('artemisApp.exercise-scores-chart.yourScoreLabel'),
            pointStyle: 'circle',
            borderWidth: 3,
            lineTension: 0,
            spanGaps: true,
        },
        {
            fill: false,
            data: [],
            label: this.translateService.instant('artemisApp.exercise-scores-chart.averageScoreLabel'),
            pointStyle: 'rect',
            borderWidth: 3,
            lineTension: 0,
            spanGaps: true,
            borderDash: [1, 1],
        },
        {
            fill: false,
            data: [],
            label: this.translateService.instant('artemisApp.exercise-scores-chart.maximumScoreLabel'),
            pointStyle: 'triangle',
            borderWidth: 3,
            lineTension: 0,
            spanGaps: true,
            borderDash: [15, 3, 3, 3],
        },
    ];
    public lineChartLabels: Label[] = this.exerciseScores.map((exerciseScoreDTO) => exerciseScoreDTO.exerciseTitle!);
    public lineChartOptions: ChartOptions = {
        onHover: (event: any, chartElement) => {
            event.target.style.cursor = chartElement[0] ? 'pointer' : 'default';
        },
        onClick: (evt) => {
            const point: any = this.chartInstance.getElementAtEvent(evt)[0];

            if (point) {
                const value: any = this.chartInstance.data.datasets![point._datasetIndex]!.data![point._index];
                if (value.exerciseId) {
                    this.navigateToExercise(value.exerciseId);
                }
            }
        },
        tooltips: {
            callbacks: {
                label(tooltipItem, data) {
                    let label = data.datasets![tooltipItem.datasetIndex!].label || '';

                    if (label) {
                        label += ': ';
                    }
                    label += Math.round((tooltipItem.yLabel as number) * 100) / 100;
                    label += ' %';
                    return label;
                },
                footer(tooltipItem, data) {
                    const dataset = data.datasets![tooltipItem[0].datasetIndex!].data![tooltipItem[0].index!];
                    const exerciseType = (dataset as any).exerciseType;
                    return [`Exercise Type: ${exerciseType}`];
                },
            },
        },
        responsive: true,
        maintainAspectRatio: false,
        title: {
            display: false,
        },
        legend: {
            position: 'left',
        },
        scales: {
            yAxes: [
                {
                    scaleLabel: {
                        display: true,
                        labelString: this.translateService.instant('artemisApp.exercise-scores-chart.yAxis'),
                        fontSize: 12,
                    },
                    ticks: {
                        suggestedMax: 100,
                        suggestedMin: 0,
                        beginAtZero: true,
                        precision: 0,
                        fontSize: 12,
                    },
                },
            ],
            xAxes: [
                {
                    scaleLabel: {
                        display: true,
                        labelString: this.translateService.instant('artemisApp.exercise-scores-chart.xAxis'),
                        fontSize: 12,
                    },
                    ticks: {
                        autoSkip: false,
                        fontSize: 12,
                        callback(exerciseTitle: string) {
                            if (exerciseTitle.length > 20) {
                                // shorten exercise title if too long (will be displayed in full in tooltip)
                                return exerciseTitle.substr(0, 20) + '...';
                            } else {
                                return exerciseTitle;
                            }
                        },
                    },
                },
            ],
        },
    };
    public lineChartColors: Color[] = [
        {
            borderColor: 'skyBlue',
            backgroundColor: 'skyBlue',
            hoverBackgroundColor: 'black',
            hoverBorderColor: 'black',
        },
        {
            borderColor: 'salmon',
            backgroundColor: 'salmon',
            hoverBackgroundColor: 'black',
            hoverBorderColor: 'black',
        },
        {
            borderColor: 'limeGreen',
            backgroundColor: 'limeGreen',
            hoverBackgroundColor: 'black',
            hoverBorderColor: 'black',
        },
    ];
    public lineChartLegend = true;
    public lineChartType: ChartType = 'line';
    public lineChartPlugins = [];

    constructor(
        private router: Router,
        private activatedRoute: ActivatedRoute,
        private alertService: JhiAlertService,
        private learningAnalyticsService: ExerciseScoresChartService,
        private translateService: TranslateService,
    ) {}

    ngOnDestroy() {
        this.chartInstance.destroy();
    }

    ngAfterViewInit() {
        this.chartInstance = this.chartDirective.chart;
        this.activatedRoute.parent!.params.subscribe((params) => {
            this.courseId = +params['courseId'];
            if (this.courseId) {
                this.loadDataAndInitializeChart();
            }
        });
    }

    private loadDataAndInitializeChart() {
        this.isLoading = true;
        this.learningAnalyticsService
            .getExerciseScoresForCourse(this.courseId)
            .pipe(
                finalize(() => {
                    this.isLoading = false;
                }),
            )
            .subscribe(
                (exerciseScoresResponse) => {
                    this.exerciseScores = exerciseScoresResponse.body!;
                    this.initializeChart();
                },
                (errorResponse: HttpErrorResponse) => onError(this.alertService, errorResponse),
            );
    }

    private initializeChart() {
        const chartWidth = 80 * this.exerciseScores.length;
        this.chartDiv.nativeElement.setAttribute('style', `width: ${chartWidth}px;`);
        this.chartInstance.resize();
        const sortedExerciseScores = _.sortBy(this.exerciseScores, (exerciseScore) => exerciseScore.releaseDate);
        this.addData(this.chartInstance, sortedExerciseScores);
    }

    private addData(chart: Chart, exerciseScoresDTOs: ExerciseScoresDTO[]) {
        for (const exerciseScoreDTO of exerciseScoresDTOs) {
            chart.data.labels!.push(exerciseScoreDTO.exerciseTitle!);
            (chart.data.datasets![0].data as ChartPoint[])!.push({
                y: exerciseScoreDTO.scoreOfStudent,
                exerciseId: exerciseScoreDTO.exerciseId,
                exerciseTitle: exerciseScoreDTO.exerciseTitle,
                exerciseType: exerciseScoreDTO.exerciseType,
            } as Chart.ChartPoint);
            (chart.data.datasets![1].data as ChartPoint[])!.push({
                y: exerciseScoreDTO.averageScoreAchieved,
                exerciseId: exerciseScoreDTO.exerciseId,
                exerciseTitle: exerciseScoreDTO.exerciseTitle,
                exerciseType: exerciseScoreDTO.exerciseType,
            } as Chart.ChartPoint);
            (chart.data.datasets![2].data as ChartPoint[])!.push({
                y: exerciseScoreDTO.maxScoreAchieved,
                exerciseId: exerciseScoreDTO.exerciseId,
                exerciseTitle: exerciseScoreDTO.exerciseTitle,
                exerciseType: exerciseScoreDTO.exerciseType,
            } as Chart.ChartPoint);
        }
        this.chartInstance.update();
    }

    navigateToExercise(exerciseId: number) {
        this.router.navigate(['courses', this.courseId, 'exercises', exerciseId]);
    }
}
