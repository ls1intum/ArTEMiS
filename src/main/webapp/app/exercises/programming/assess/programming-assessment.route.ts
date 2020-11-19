import { Injectable, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterModule, Routes } from '@angular/router';
import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
import { CodeEditorTutorAssessmentContainerComponent } from 'app/exercises/programming/assess/code-editor-tutor-assessment-container.component';
import { Authority } from 'app/shared/constants/authority.constants';
import { ProgrammingSubmissionService } from 'app/exercises/programming/participate/programming-submission.service';
import { Observable } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { JhiAlertService } from 'ng-jhipster';

@Injectable({ providedIn: 'root' })
export class NewStudentParticipationResolver implements Resolve<number | undefined> {
    constructor(private programmingSubmissionService: ProgrammingSubmissionService, private jhiAlertService: JhiAlertService) {}

    /**
     * Resolves the needed StudentParticipations for the ProgrammingSubmissionAssessmentComponent using the ProgrammingAssessmentsService.
     * @param route
     */
    resolve(route: ActivatedRouteSnapshot) {
        const exerciseId = Number(route.paramMap.get('exerciseId'));

        if (exerciseId) {
            return this.programmingSubmissionService.getProgrammingSubmissionForExerciseWithoutAssessment(exerciseId, true).pipe(
                map((submission) => submission.participation!.id!),
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
        path: ':courseId/programming-exercises/:exerciseId/code-editor/new/assessment',
        component: CodeEditorTutorAssessmentContainerComponent,
        data: {
            authorities: [Authority.ADMIN, Authority.INSTRUCTOR, Authority.TA],
            pageTitle: 'artemisApp.programmingExercise.home.title',
        },
        resolve: {
            studentParticipationId: NewStudentParticipationResolver,
        },
        runGuardsAndResolvers: 'always',
        canActivate: [UserRouteAccessService],
    },
    {
        path: ':courseId/programming-exercises/:exerciseId/code-editor/:participationId/assessment',
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
