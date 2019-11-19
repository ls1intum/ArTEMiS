import { JhiAlertService } from 'ng-jhipster';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs/Subscription';
import { ActivatedRoute } from '@angular/router';
import { DifferencePipe } from 'ngx-moment';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { HttpResponse } from '@angular/common/http';
import { Moment } from 'moment';
import { areManualResultsAllowed, Exercise, ExerciseService, ExerciseType } from 'app/entities/exercise';
import { Course, CourseService } from 'app/entities/course';
import { Result, ResultService } from 'app/entities/result';
import { SourceTreeService } from 'app/components/util/sourceTree.service';
import { ModelingAssessmentService } from 'app/entities/modeling-assessment';
import { ParticipationService, ProgrammingExerciseStudentParticipation, StudentParticipation } from 'app/entities/participation';
import { ProgrammingSubmissionService } from 'app/programming-submission';
import { debounceTime, distinctUntilChanged, map, take, tap } from 'rxjs/operators';
import { Observable, of, zip } from 'rxjs';
import { AssessmentType } from 'app/entities/assessment-type';
import { ColumnMode, SortType } from '@swimlane/ngx-datatable';
import { SortByPipe } from 'app/components/pipes';
import { compose, filter } from 'lodash/fp';

enum FilterProp {
    ALL = 'all',
    SUCCESSFUL = 'successful',
    UNSUCCESSFUL = 'unsuccessful',
    MANUAL = 'manual',
    AUTOMATIC = 'automatic',
}

enum SortOrder {
    ASC = 'asc',
    DESC = 'desc',
}

enum SortIcon {
    NONE = 'sort',
    ASC = 'sort-up',
    DESC = 'sort-down',
}

const SortOrderIcon = {
    [SortOrder.ASC]: SortIcon.ASC,
    [SortOrder.DESC]: SortIcon.DESC,
};

type SortProp = {
    field: string;
    order: SortOrder;
};

const resultsPerPageCacheKey = 'exercise-scores-results-per-age';

@Component({
    selector: 'jhi-exercise-scores',
    templateUrl: './exercise-scores.component.html',
    styleUrls: ['exercise-scores.component.scss'],
    providers: [JhiAlertService, ModelingAssessmentService, SourceTreeService],
})
export class ExerciseScoresComponent implements OnInit, OnDestroy {
    // make constants available to html for comparison
    FilterProp = FilterProp;
    readonly QUIZ = ExerciseType.QUIZ;
    readonly PROGRAMMING = ExerciseType.PROGRAMMING;
    readonly MODELING = ExerciseType.MODELING;
    PAGING_VALUES = [10, 20, 50, 100, 200, 500, 1000, 2000];
    DEFAULT_PAGING_VALUE = 50;

    ColumnMode = ColumnMode;
    SortType = SortType;

    course: Course;
    exercise: Exercise;
    paramSub: Subscription;
    reverse: boolean;
    results: Result[];
    allResults: Result[];
    eventSubscriber: Subscription;
    newManualResultAllowed: boolean;
    resultsPerPage: number;

    resultCriteria: {
        filterProp: FilterProp;
        textSearch: string[];
        sortProp: SortProp;
    };

    isLoading: boolean;

    constructor(
        private route: ActivatedRoute,
        private momentDiff: DifferencePipe,
        private courseService: CourseService,
        private exerciseService: ExerciseService,
        private resultService: ResultService,
        private modelingAssessmentService: ModelingAssessmentService,
        private participationService: ParticipationService,
        private programmingSubmissionService: ProgrammingSubmissionService,
        private sourceTreeService: SourceTreeService,
        private modalService: NgbModal,
        private sortByPipe: SortByPipe,
    ) {
        this.resultCriteria = {
            filterProp: FilterProp.ALL,
            textSearch: [],
            sortProp: { field: 'id', order: SortOrder.ASC },
        };
        this.results = [];
        this.allResults = [];
    }

    ngOnInit() {
        this.resultsPerPage = this.getCachedResultsPerPage();
        this.paramSub = this.route.params.subscribe(params => {
            this.isLoading = true;
            this.courseService.find(params['courseId']).subscribe((res: HttpResponse<Course>) => {
                this.course = res.body!;
            });
            this.exerciseService.find(params['exerciseId']).subscribe((res: HttpResponse<Exercise>) => {
                this.exercise = res.body!;
                // After both calls are done, the loading flag is removed. If the exercise is not a programming exercise, only the result call is needed.
                zip(this.getResults(), this.loadAndCacheProgrammingExerciseSubmissionState())
                    .pipe(take(1))
                    .subscribe(() => (this.isLoading = false));
                this.newManualResultAllowed = areManualResultsAllowed(this.exercise);
            });
        });
    }

    /**
     * We need to preload the pending submissions here, otherwise every updating-result would trigger a single REST call.
     * Will return immediately if the exercise is not of type PROGRAMMING.
     */
    private loadAndCacheProgrammingExerciseSubmissionState() {
        return this.exercise.type === ExerciseType.PROGRAMMING ? this.programmingSubmissionService.getSubmissionStateOfExercise(this.exercise.id) : of(null);
    }

    getResults() {
        return this.resultService
            .getResultsForExercise(this.exercise.course!.id, this.exercise.id, {
                ratedOnly: true,
                withSubmissions: this.exercise.type === ExerciseType.MODELING,
                withAssessors: this.exercise.type === ExerciseType.MODELING,
            })
            .pipe(
                tap((res: HttpResponse<Result[]>) => {
                    const tempResults: Result[] = res.body!;
                    tempResults.forEach(result => {
                        result.participation!.results = [result];
                        (result.participation! as StudentParticipation).exercise = this.exercise;
                        result.durationInMinutes = this.durationInMinutes(
                            result.completionDate!,
                            result.participation!.initializationDate ? result.participation!.initializationDate : this.exercise.releaseDate!,
                        );
                    });
                    this.allResults = tempResults;
                    this.updateResults();
                    // Nest submission into participation so that it is available for the result component
                    this.results = this.results.map(result => {
                        if (result.participation && result.submission) {
                            result.participation.submissions = [result.submission];
                        }
                        return result;
                    });
                }),
            );
    }

    filterResultByProp = (filterProp: FilterProp, result: Result) => {
        switch (filterProp) {
            case FilterProp.SUCCESSFUL:
                return result.successful;
            case FilterProp.UNSUCCESSFUL:
                return !result.successful;
            case FilterProp.MANUAL:
                return result.assessmentType === AssessmentType.MANUAL;
            case FilterProp.AUTOMATIC:
                return result.assessmentType === AssessmentType.AUTOMATIC;
            default:
                return true;
        }
    };

    /**
     * Filter the given results by the provided search words.
     * Returns results that match any of the provides search words, if searchWords is empty returns all results.
     *
     * @param searchWords list of student logins or names.
     * @param result Result[]
     */
    filterResultByTextSearch = (searchWords: string[], result: Result) => {
        const searchableFields = [(result.participation as StudentParticipation).student.login, (result.participation as StudentParticipation).student.name].filter(
            Boolean,
        ) as string[];
        // When no search word is inputted, we return all results.
        if (!searchWords.length) {
            return true;
        }
        // Otherwise we do a fuzzy search on the inputted search words.
        return searchableFields.some(field => searchWords.some(word => word && field.toLowerCase().includes(word.toLowerCase())));
    };

    /**
     * Updates the UI with all available filter/sort settings.
     * First performs the filtering, then sorts the remaining results.
     */
    updateResults() {
        const filteredResults = compose(
            filter((result: Result) => this.filterResultByTextSearch(this.resultCriteria.textSearch, result)),
            filter((result: Result) => this.filterResultByProp(this.resultCriteria.filterProp, result)),
        )(this.allResults);
        // TODO: It would be nice to do this with a normal sortBy/orderBy.
        this.results = this.sortByPipe.transform(filteredResults, this.resultCriteria.sortProp.field, this.resultCriteria.sortProp.order === SortOrder.DESC);
    }

    durationInMinutes(completionDate: Moment, initializationDate: Moment) {
        return this.momentDiff.transform(completionDate, initializationDate, 'minutes');
    }

    goToBuildPlan(result: Result) {
        // TODO: get the continuous integration URL as a client constant during the management info call
        window.open('https://bamboobruegge.in.tum.de/browse/' + (result.participation! as ProgrammingExerciseStudentParticipation).buildPlanId);
    }

    goToRepository(result: Result) {
        window.open((result.participation! as ProgrammingExerciseStudentParticipation).repositoryUrl);
    }

    updateResultFilter(newValue: FilterProp) {
        this.resultCriteria.filterProp = newValue;
        this.updateResults();
    }

    exportNames() {
        if (this.results.length > 0) {
            const rows: string[] = [];
            this.results.forEach((result, index) => {
                const studentParticipation = result.participation! as StudentParticipation;
                let studentName = studentParticipation.student.firstName!;
                if (studentParticipation.student.lastName != null && studentParticipation.student.lastName !== '') {
                    studentName = studentName + ' ' + studentParticipation.student.lastName;
                }
                rows.push(index === 0 ? 'data:text/csv;charset=utf-8,' + studentName : studentName);
            });
            const csvContent = rows.join('\n');
            const encodedUri = encodeURI(csvContent);
            const link = document.createElement('a');
            link.setAttribute('href', encodedUri);
            link.setAttribute('download', 'results-names.csv');
            document.body.appendChild(link); // Required for FF
            link.click();
        }
    }

    exportResults() {
        if (this.results.length > 0) {
            const rows: string[] = [];
            this.results.forEach((result, index) => {
                const studentParticipation = result.participation! as StudentParticipation;
                let studentName = studentParticipation.student.firstName!;
                if (studentParticipation.student.lastName != null && studentParticipation.student.lastName !== '') {
                    studentName = studentName + ' ' + studentParticipation.student.lastName;
                }
                const studentId = studentParticipation.student.login;
                const score = result.score;

                if (index === 0) {
                    if (this.exercise.type !== ExerciseType.PROGRAMMING) {
                        rows.push('data:text/csv;charset=utf-8,Name, Username, Score');
                    } else {
                        rows.push('data:text/csv;charset=utf-8,Name, Username, Score, Repo Link');
                    }
                }
                if (this.exercise.type !== ExerciseType.PROGRAMMING) {
                    rows.push(studentName + ', ' + studentId + ', ' + score);
                } else {
                    const repoLink = (studentParticipation as ProgrammingExerciseStudentParticipation).repositoryUrl;
                    rows.push(studentName + ', ' + studentId + ', ' + score + ', ' + repoLink);
                }
            });
            const csvContent = rows.join('\n');
            const encodedUri = encodeURI(csvContent);
            const link = document.createElement('a');
            link.setAttribute('href', encodedUri);
            link.setAttribute('download', 'results-scores.csv');
            document.body.appendChild(link); // Required for FF
            link.click();
        }
    }

    private invertSort = (order: SortOrder) => {
        return order === SortOrder.ASC ? SortOrder.DESC : SortOrder.ASC;
    };

    /**
     * Returns the Font Awesome icon name for a column header's sorting icon
     * based on the currently active sortProp field and order.
     *
     * @param field Result field
     */
    iconForSortPropField(field: string) {
        if (this.resultCriteria.sortProp.field !== field) {
            return SortIcon.NONE;
        }
        return SortOrderIcon[this.resultCriteria.sortProp.order];
    }

    /**
     * Sets the selected sort field, then updates the available results in the UI.
     * Toggles the order direction (asc, desc) when the field has not changed.
     *
     * @param field Result field
     */
    onSort(field: string) {
        const sameField = this.resultCriteria.sortProp && this.resultCriteria.sortProp.field === field;
        const order = sameField ? this.invertSort(this.resultCriteria.sortProp.order) : SortOrder.ASC;
        this.resultCriteria.sortProp = { field, order };
        this.updateResults();
    }

    /**
     * Formats the results in the autocomplete overlay.
     *
     * @param result
     */
    searchResultFormatter = (result: Result) => {
        const login = (result.participation as StudentParticipation).student.login;
        const name = (result.participation as StudentParticipation).student.name;
        return `${login} (${name})`;
    };

    searchInputFormatter = () => {
        return this.resultCriteria.textSearch.join(', ');
    };

    /**
     * Splits the provides search words by comma and updates the autocompletion overlay.
     * Also updates the available results in the UI.
     *
     * @param text$ stream of text input.
     */
    onSearch = (text$: Observable<string>) => {
        return text$.pipe(
            debounceTime(200),
            distinctUntilChanged(),
            map(text => {
                const searchWords = text.split(',').map(word => word.trim());
                // When the result field is cleared, we translate the resulting empty string to an empty array (otherwise no results would be found).
                return searchWords.length === 1 && !searchWords[0] ? [] : searchWords;
            }),
            // For available results in table.
            tap(searchWords => {
                this.resultCriteria.textSearch = searchWords;
                this.updateResults();
            }),
            // For autocomplete.
            map((searchWords: string[]) => {
                // We only execute the autocomplete for the last keyword in the provided list.
                const lastSearchWord = searchWords.length ? searchWords[searchWords.length - 1] : null;
                // Don't execute autocomplete for less then two inputted characters.
                if (!lastSearchWord || lastSearchWord.length < 3) {
                    return false;
                }
                return this.results.filter(result => {
                    const searchableFields = [(result.participation as StudentParticipation).student.login, (result.participation as StudentParticipation).student.name].filter(
                        Boolean,
                    ) as string[];
                    return searchableFields.some(value => value.toLowerCase().includes(lastSearchWord.toLowerCase()) && value.toLowerCase() !== lastSearchWord.toLowerCase());
                });
            }),
        );
    };

    onAutocompleteSelect = (result: Result) => {
        this.resultCriteria.textSearch[this.resultCriteria.textSearch.length - 1] = (result.participation as StudentParticipation).student.login!;
        this.updateResults();
    };

    getCachedResultsPerPage = () => {
        const cachedValue = localStorage.getItem(resultsPerPageCacheKey);
        return cachedValue ? parseInt(cachedValue, 10) : this.DEFAULT_PAGING_VALUE;
    };

    setResultsPerPage = (paging: number) => {
        this.isLoading = true;
        setTimeout(() => {
            this.resultsPerPage = paging;
            this.isLoading = false;
        }, 500);
        localStorage.setItem(resultsPerPageCacheKey, paging.toString());
    };

    refresh() {
        this.isLoading = true;
        this.results = [];
        this.getResults().subscribe(() => (this.isLoading = false));
    }

    ngOnDestroy() {
        this.paramSub.unsubscribe();
    }

    callback() {}
}
