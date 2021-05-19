import { Component, OnInit } from '@angular/core';
import { GradeType, GradingScale } from 'app/entities/grading-scale.model';
import { GradeStep } from 'app/entities/grade-step.model';
import { ActivatedRoute } from '@angular/router';
import { EntityResponseType, GradingSystemService } from 'app/grading-system/grading-system.service';
import { ButtonSize } from 'app/shared/components/button.component';
import { Observable, Subject } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs/operators';

@Component({
    selector: 'jhi-grading-system',
    templateUrl: './grading-system.component.html',
    styleUrls: ['./grading-system.component.scss'],
})
export class GradingSystemComponent implements OnInit {
    ButtonSize = ButtonSize;
    gradingScale = new GradingScale();
    lowerBoundInclusivity = true;
    existingGradingScale = false;
    firstPassingGrade: string;
    courseId?: number;
    examId?: number;
    isExam = false;
    dialogErrorSource = new Subject<string>();
    dialogError$ = this.dialogErrorSource.asObservable();
    notFound = false;
    isLoading = false;

    constructor(private gradingSystemService: GradingSystemService, private route: ActivatedRoute) {}

    ngOnInit(): void {
        this.route.params.subscribe((params) => {
            this.isLoading = true;
            this.courseId = Number(params['courseId']);
            if (params['examId']) {
                this.examId = Number(params['examId']);
                this.isExam = true;
            }
            if (this.isExam) {
                this.handleFindObservable(this.gradingSystemService.findGradingScaleForExam(this.courseId!, this.examId!));
            } else {
                this.handleFindObservable(this.gradingSystemService.findGradingScaleForCourse(this.courseId!));
            }
        });
    }

    private handleFindObservable(findObservable: Observable<EntityResponseType>) {
        findObservable
            .pipe(
                finalize(() => {
                    this.isLoading = false;
                }),
            )
            .subscribe((gradingSystemResponse) => {
                if (gradingSystemResponse.body) {
                    this.handleFindResponse(gradingSystemResponse.body);
                }
            }, this.handleErrorResponse());
    }

    /**
     * Handles 404 Not Found response in case not grading scale exists
     *
     * @private
     */
    private handleErrorResponse() {
        return (err: HttpErrorResponse) => {
            if (err.status === 404) {
                this.notFound = true;
            }
        };
    }

    /**
     * If the grading scale exists, sorts its grade steps,
     * and sets the inclusivity and first passing grade properties
     *
     * @param gradingScale the grading scale retrieved from the get request
     * @private
     */
    handleFindResponse(gradingScale?: GradingScale): void {
        if (gradingScale) {
            gradingScale.gradeSteps = this.gradingSystemService.sortGradeSteps(gradingScale.gradeSteps);
            this.gradingScale = gradingScale;
            this.existingGradingScale = true;
            this.setBoundInclusivity();
            this.determineFirstPassingGrade();
        }
    }

    /**
     * Sorts the grade steps by lower bound percentage, sets their inclusivity
     * and passing grade properties and saves the grading scale via the service
     */
    save(): void {
        this.isLoading = true;
        this.notFound = false;
        this.gradingScale.gradeSteps = this.gradingSystemService.sortGradeSteps(this.gradingScale.gradeSteps);
        this.gradingScale.gradeSteps = this.setInclusivity(this.gradingScale.gradeSteps);
        this.gradingScale.gradeSteps = this.setPassingGrades(this.gradingScale.gradeSteps);
        // new grade steps shouldn't have ids set
        this.gradingScale.gradeSteps.forEach((gradeStep) => {
            gradeStep.id = undefined;
        });
        if (this.existingGradingScale) {
            if (this.isExam) {
                this.handleSaveObservable(this.gradingSystemService.updateGradingScaleForExam(this.courseId!, this.examId!, this.gradingScale));
            } else {
                this.handleSaveObservable(this.gradingSystemService.updateGradingScaleForCourse(this.courseId!, this.gradingScale));
            }
        } else {
            if (this.isExam) {
                this.handleSaveObservable(this.gradingSystemService.createGradingScaleForExam(this.courseId!, this.examId!, this.gradingScale));
            } else {
                this.handleSaveObservable(this.gradingSystemService.createGradingScaleForCourse(this.courseId!, this.gradingScale));
            }
        }
    }

    private handleSaveObservable(saveObservable: Observable<EntityResponseType>) {
        saveObservable
            .pipe(
                finalize(() => {
                    this.isLoading = false;
                }),
            )
            .subscribe((gradingSystemResponse) => {
                this.handleSaveResponse(gradingSystemResponse.body!);
            });
    }

    /**
     * Sorts the grading scale's grade steps after it has been saved
     * and sets the existingGradingScale property
     *
     * @param newGradingScale the grading scale that was just saved
     * @private
     */
    private handleSaveResponse(newGradingScale?: GradingScale): void {
        if (newGradingScale) {
            newGradingScale.gradeSteps = this.gradingSystemService.sortGradeSteps(newGradingScale.gradeSteps);
            this.gradingScale = newGradingScale;
            this.existingGradingScale = true;
        }
    }

    /**
     * Deletes a grading scale for the given course/exam via the service
     */
    delete(): void {
        if (!this.existingGradingScale) {
            return;
        }
        this.isLoading = true;
        if (this.isExam) {
            this.handleDeleteObservable(this.gradingSystemService.deleteGradingScaleForExam(this.courseId!, this.examId!));
        } else {
            this.handleDeleteObservable(this.gradingSystemService.deleteGradingScaleForCourse(this.courseId!));
        }
        this.gradingScale = new GradingScale();
    }

    handleDeleteObservable(deleteObservable: Observable<EntityResponseType>) {
        deleteObservable
            .pipe(
                finalize(() => {
                    this.isLoading = false;
                }),
            )
            .subscribe(() => {
                this.existingGradingScale = false;
                this.dialogErrorSource.next('');
            });
    }

    /**
     * Sets the lowerBoundInclusivity property based on grade steps based on the grade steps
     * Called on initialization
     */
    setBoundInclusivity(): void {
        this.lowerBoundInclusivity = this.gradingScale.gradeSteps.every((gradeStep) => {
            return gradeStep.lowerBoundInclusive || gradeStep.lowerBoundPercentage === 0;
        });
    }

    /**
     * Sets the inclusivity for all grade steps based on the lowerBoundInclusivity property
     * Called before a post/put request
     *
     * @param gradeSteps the grade steps which will be adjusted
     */
    setInclusivity(gradeSteps: GradeStep[]): GradeStep[] {
        gradeSteps.forEach((gradeStep) => {
            if (this.lowerBoundInclusivity) {
                gradeStep.lowerBoundInclusive = true;
                gradeStep.upperBoundInclusive = gradeStep.upperBoundPercentage === 100;
            } else {
                gradeStep.upperBoundInclusive = true;
                gradeStep.lowerBoundInclusive = gradeStep.lowerBoundPercentage === 0;
            }
        });
        return gradeSteps;
    }

    /**
     * Sets the firstPassingGrade property based on the grade steps
     * Called on initialization
     */
    determineFirstPassingGrade(): void {
        this.firstPassingGrade =
            this.gradingScale.gradeSteps.find((gradeStep) => {
                return gradeStep.isPassingGrade;
            })?.gradeName ?? this.gradingScale.gradeSteps[this.gradingScale.gradeSteps.length - 1].gradeName;
    }

    /**
     * Sets the isPassingGrade property for all grade steps based on the lowerBoundInclusive property
     * Called before a post/put request
     *
     * @param gradeSteps the grade steps which will be adjusted
     */
    setPassingGrades(gradeSteps: GradeStep[]): GradeStep[] {
        let passingGrade = false;
        gradeSteps.forEach((gradeStep) => {
            if (gradeStep.gradeName === this.firstPassingGrade) {
                passingGrade = true;
            }
            gradeStep.isPassingGrade = passingGrade;
        });
        return gradeSteps;
    }

    deleteGradeNames(): void {
        this.gradingScale.gradeSteps.forEach((gradeStep) => {
            gradeStep.gradeName = '';
        });
    }

    gradeStepsWithNonemptyNames(): GradeStep[] {
        if (this.gradingScale.gradeSteps) {
            return this.gradingScale.gradeSteps.filter((gradeStep) => {
                return gradeStep.gradeName !== '';
            });
        } else {
            return [];
        }
    }

    /**
     * Checks if grading scale has GradeType.GRADE
     */
    isGradeType(): boolean {
        return this.gradingScale.gradeType === GradeType.GRADE;
    }

    /**
     * Create a new grade step add the end of the current grade step set
     */
    createGradeStep(): void {
        const gradeStep: GradeStep = {
            gradeName: '',
            lowerBoundPercentage: 100,
            upperBoundPercentage: 100,
            isPassingGrade: true,
            lowerBoundInclusive: this.lowerBoundInclusivity,
            upperBoundInclusive: true,
        };
        if (!this.gradingScale) {
            this.gradingScale = new GradingScale();
        }
        if (!this.gradingScale.gradeSteps) {
            this.gradingScale.gradeSteps = [];
        }
        this.gradingScale.gradeSteps.push(gradeStep);
    }

    /**
     * Delete grade step at given index
     *
     * @param index the index of the grade step
     */
    deleteGradeStep(index: number): void {
        this.gradingScale.gradeSteps.splice(index, 1);
    }

    /**
     * Generates a default grading scale to be used as template
     */
    generateDefaultGradingScale(): void {
        this.gradingScale = this.getDefaultGradingScale();
        this.firstPassingGrade = this.gradingScale.gradeSteps[3].gradeName;
        this.lowerBoundInclusivity = true;
    }

    /**
     * Returns the mock grading scale from the university course PSE
     */
    getDefaultGradingScale(): GradingScale {
        const gradeStep1: GradeStep = {
            gradeName: '5.0',
            lowerBoundPercentage: 0,
            upperBoundPercentage: 40,
            lowerBoundInclusive: true,
            upperBoundInclusive: false,
            isPassingGrade: false,
        };
        const gradeStep2: GradeStep = {
            gradeName: '4.7',
            lowerBoundPercentage: 40,
            upperBoundPercentage: 45,
            lowerBoundInclusive: true,
            upperBoundInclusive: false,
            isPassingGrade: false,
        };
        const gradeStep3: GradeStep = {
            gradeName: '4.3',
            lowerBoundPercentage: 45,
            upperBoundPercentage: 50,
            lowerBoundInclusive: true,
            upperBoundInclusive: false,
            isPassingGrade: false,
        };
        const gradeStep4: GradeStep = {
            gradeName: '4.0',
            lowerBoundPercentage: 50,
            upperBoundPercentage: 55,
            lowerBoundInclusive: true,
            upperBoundInclusive: false,
            isPassingGrade: true,
        };
        const gradeStep5: GradeStep = {
            gradeName: '3.7',
            lowerBoundPercentage: 55,
            upperBoundPercentage: 60,
            lowerBoundInclusive: true,
            upperBoundInclusive: false,
            isPassingGrade: true,
        };
        const gradeStep6: GradeStep = {
            gradeName: '3.3',
            lowerBoundPercentage: 60,
            upperBoundPercentage: 65,
            lowerBoundInclusive: true,
            upperBoundInclusive: false,
            isPassingGrade: true,
        };
        const gradeStep7: GradeStep = {
            gradeName: '3.0',
            lowerBoundPercentage: 65,
            upperBoundPercentage: 70,
            lowerBoundInclusive: true,
            upperBoundInclusive: false,
            isPassingGrade: true,
        };
        const gradeStep8: GradeStep = {
            gradeName: '2.7',
            lowerBoundPercentage: 70,
            upperBoundPercentage: 75,
            lowerBoundInclusive: true,
            upperBoundInclusive: false,
            isPassingGrade: true,
        };
        const gradeStep9: GradeStep = {
            gradeName: '2.3',
            lowerBoundPercentage: 75,
            upperBoundPercentage: 80,
            lowerBoundInclusive: true,
            upperBoundInclusive: false,
            isPassingGrade: true,
        };
        const gradeStep10: GradeStep = {
            gradeName: '2.0',
            lowerBoundPercentage: 80,
            upperBoundPercentage: 85,
            lowerBoundInclusive: true,
            upperBoundInclusive: false,
            isPassingGrade: true,
        };
        const gradeStep11: GradeStep = {
            gradeName: '1.7',
            lowerBoundPercentage: 85,
            upperBoundPercentage: 90,
            lowerBoundInclusive: true,
            upperBoundInclusive: false,
            isPassingGrade: true,
        };
        const gradeStep12: GradeStep = {
            gradeName: '1.3',
            lowerBoundPercentage: 90,
            upperBoundPercentage: 95,
            lowerBoundInclusive: true,
            upperBoundInclusive: false,
            isPassingGrade: true,
        };
        const gradeStep13: GradeStep = {
            gradeName: '1.0',
            lowerBoundPercentage: 95,
            upperBoundPercentage: 100,
            lowerBoundInclusive: true,
            upperBoundInclusive: true,
            isPassingGrade: true,
        };
        return {
            gradeSteps: [
                gradeStep1,
                gradeStep2,
                gradeStep3,
                gradeStep4,
                gradeStep5,
                gradeStep6,
                gradeStep7,
                gradeStep8,
                gradeStep9,
                gradeStep10,
                gradeStep11,
                gradeStep12,
                gradeStep13,
            ],
            gradeType: GradeType.GRADE,
        };
    }
}
