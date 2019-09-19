import { Injectable, OnDestroy } from '@angular/core';
import { JhiAlertService } from 'ng-jhipster';
import { Subject, Subscription } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { DomainChange, DomainDependent, DomainService } from 'app/code-editor/service/code-editor-domain.service';
import { ProgrammingSubmissionService, ProgrammingSubmissionState } from 'app/programming-submission/programming-submission.service';
import { DomainType } from 'app/code-editor/service/code-editor-repository.service';
import { SolutionProgrammingExerciseParticipation, StudentParticipation } from 'app/entities/participation';

/**
 * Wrapper service for using the currently selected participation id in the code-editor for retrieving the submission state.
 */
@Injectable({ providedIn: 'root' })
export class CodeEditorSubmissionService extends DomainDependent implements OnDestroy {
    private participationId: number | null;
    private exerciseId: number | null;
    private isBuildingSubject = new Subject<boolean>();
    private submissionSubscription: Subscription;

    constructor(domainService: DomainService, private submissionService: ProgrammingSubmissionService, private alertService: JhiAlertService) {
        super(domainService);
        this.initDomainSubscription();
    }

    ngOnDestroy() {
        if (this.submissionSubscription) {
            this.submissionSubscription.unsubscribe();
        }
        this.isBuildingSubject.complete();
    }

    setDomain(domain: DomainChange) {
        super.setDomain(domain);
        const [domainType, domainValue] = domain;
        // Subscribe to the submission state of the currently selected participation, map the submission to the isBuilding state.
        if (domainType === DomainType.PARTICIPATION && domainValue.id !== this.participationId) {
            this.participationId = domainValue.id;
            // There is no differentiation between the participation types atm. This could be implemented in the domain service, but this would make the implementation more complicated, too.
            this.exerciseId = (domainValue as StudentParticipation).exercise
                ? (domainValue as StudentParticipation).exercise.id
                : (domainValue as SolutionProgrammingExerciseParticipation).programmingExercise.id;
            this.submissionSubscription = this.submissionService
                .getLatestPendingSubmissionByParticipationId(this.participationId, this.exerciseId)
                .pipe(
                    tap(({ submissionState }) => submissionState === ProgrammingSubmissionState.HAS_FAILED_SUBMISSION && this.onError()),
                    map(({ submission }) => !!submission),
                    tap((isBuilding: boolean) => this.isBuildingSubject.next(isBuilding)),
                )
                .subscribe();
        } else if (domainType === DomainType.TEST_REPOSITORY) {
            // There are no submissions for the test repository, so it is never building.
            this.isBuildingSubject.next(false);
        }
    }

    onError() {
        this.alertService.error('artemisApp.submission.resultTimeout');
    }

    getBuildingState() {
        return this.isBuildingSubject.asObservable();
    }
}
