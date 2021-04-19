import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
import { CodeEditorTutorAssessmentContainerComponent } from 'app/exercises/programming/assess/code-editor-tutor-assessment-container.component';
import { Authority } from 'app/shared/constants/authority.constants';
import { ProgrammingAssessmentDashboardComponent } from 'app/exercises/programming/assess/programming-assessment-dashboard/programming-assessment-dashboard.component';

@Injectable({ providedIn: 'root' })
export class NewStudentParticipationResolver implements Resolve<number | undefined> {
    constructor(private programmingSubmissionService: ProgrammingSubmissionService, private jhiAlertService: JhiAlertService) {}

    /**
     * Resolves the needed studentParticipationId for the CodeEditorTutorAssessmentContainerComponent for submissions without assessment
     * @param route
     */
    resolve(route: ActivatedRouteSnapshot) {
        const exerciseId = Number(route.paramMap.get('exerciseId'));
        const correctionRound = Number(route.queryParamMap.get('correction-round'));
        if (exerciseId) {
            const returnValue = this.programmingSubmissionService.getProgrammingSubmissionForExerciseForCorrectionRoundWithoutAssessment(exerciseId, true, correctionRound).pipe(
                map((submission) => submission.participation!.id!),
                catchError((error) => {
                    if (error.error && error.error.errorKey === 'lockedSubmissionsLimitReached') {
                        this.jhiAlertService.error('artemisApp.submission.lockedSubmissionsLimitReached');
                    }
                    return Observable.of(error);
                }),
            );
            return returnValue;
        }
        return Observable.of(undefined);
    }
}

@Injectable({ providedIn: 'root' })
export class StudentParticipationResolver implements Resolve<number | undefined> {
    constructor(private programmingSubmissionService: ProgrammingSubmissionService, private jhiAlertService: JhiAlertService) {}

    /**
     *
     * Locks the latest submission of a programming exercises participation, if it is not already locked
     * @param route
     */
    resolve(route: ActivatedRouteSnapshot) {
        const participationId = Number(route.paramMap.get('participationId'));
        const correctionRound = Number(route.queryParamMap.get('correction-round'));
        if (participationId) {
            return this.programmingSubmissionService.lockAndGetProgrammingSubmissionParticipation(participationId, correctionRound).pipe(
                map((participation) => participation.id),
                catchError((error) => {
                    if (error.error && error.error.errorKey === 'lockedSubmissionsLimitReached') {
                        this.jhiAlertService.error('artemisApp.submission.lockedSubmissionsLimitReached');
                    }
                    return Observable.of(error);
                }),
            );
        }
        return Observable.of(undefined);
    }
}

export const routes: Routes = [
    {
        path: ':courseId/programming-exercises/:exerciseId/assessment',
        component: ProgrammingAssessmentDashboardComponent,
        data: {
            authorities: [Authority.ADMIN, Authority.INSTRUCTOR, Authority.TA],
            pageTitle: 'artemisApp.assessmentDashboard.home.title',
        },
        canActivate: [UserRouteAccessService],
    },
    {
        path: ':courseId/programming-exercises/:exerciseId/submissions/:submissionId/assessment',
        component: CodeEditorTutorAssessmentContainerComponent,
        data: {
            authorities: [Authority.ADMIN, Authority.INSTRUCTOR, Authority.TA],
            pageTitle: 'artemisApp.programmingExercise.home.title',
        },
        canActivate: [UserRouteAccessService],
    },
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule],
})
export class ArtemisProgrammingAssessmentRoutingModule {}
