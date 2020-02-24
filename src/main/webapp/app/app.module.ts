import './vendor.ts';

import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ArtemisTutorCourseDashboardModule } from 'app/course/tutor-course-dashboard/tutor-course-dashboard.module';
import { ArtemisInstructorExerciseStatsDashboardModule } from 'app/exercises/shared/instructor-exercise-dashboard/instructor-exercise-dashboard.module';
import { ArtemisSystemNotificationModule } from 'app/core/system-notification/system-notification.module';
import { ArtemisCourseScoresModule } from 'app/course/course-scores.module';
import { NavbarComponent } from 'app/shared/layouts/navbar/navbar.component';
import { ArtemisFileUploadAssessmentModule } from 'app/exercises/file-upload/assess/file-upload-assessment.module';
import { ArtemisFileUploadParticipationModule } from 'app/exercises/file-upload/participate/file-upload-participation.module';
import { ArtemisNotificationModule } from 'app/overview/notification/notification.module';
import { NotificationContainerComponent } from 'app/shared/layouts/notification-container/notification-container.component';
import { PageRibbonComponent } from 'app/shared/layouts/profiles/page-ribbon.component';
import { ArtemisComplaintsForTutorModule } from 'app/complaints/complaints-for-tutor/complaints-for-tutor.module';
import { ArtemisHeaderExercisePageWithDetailsModule } from 'app/exercises/shared/exercise-headers/exercise-headers.module';
import { SystemNotificationComponent } from 'app/shared/layouts/system-notification/system-notification.component';
import { ArtemisAppRoutingModule } from 'app/app-routing.module';
import { ArtemisExerciseScoresModule } from 'app/exercises/shared/exercise-scores/exercise-scores.module';
import { ArtemisExampleTextSubmissionModule } from 'app/exercises/text/manage/example-text-submission/example-text-submission.module';
import { ArtemisTextModule } from 'app/exercises/text/participate/text-editor/text-editor.module';
import { JhiMainComponent } from 'app/shared/layouts/main/main.component';
import { ArtemisSharedModule } from 'app/shared/shared.module';
import { ArtemisOverviewModule } from 'app/overview/overview.module';
import { ArtemisConnectionNotificationModule } from 'app/shared/layouts/connection-notification/connection-notification.module';
import { ArtemisTextAssessmentModule } from 'app/exercises/text/assess/text-assessment/text-assessment.module';
import { ArtemisListOfComplaintsModule } from 'app/complaints/list-of-complaints/list-of-complaints.module';
import { FooterComponent } from 'app/shared/layouts/footer/footer.component';
import { ArtemisParticipationModule } from 'app/exercises/shared/participation/participation.module';
import { ArtemisLegalModule } from 'app/core/legal/legal.module';
import { ArtemisTutorExerciseDashboardModule } from 'app/exercises/shared/tutor-exercise-dashboard/tutor-exercise-dashboard.module';
import { ActiveMenuDirective } from 'app/shared/layouts/navbar/active-menu.directive';
import { ErrorComponent } from 'app/shared/layouts/error/error.component';
import { ArtemisCoreModule } from 'app/core/core.module';
import { ArtemisModelingParticipationModule } from 'app/exercises/modeling/participate/modeling-participation.module';
import { GuidedTourModule } from 'app/guided-tour/guided-tour.module';
import { ArtemisAccountModule } from 'app/account/account.module';
import { ArtemisInstructorCourseStatsDashboardModule } from 'app/course/instructor-course-dashboard/instructor-course-dashboard.module';
import { ArtemisComplaintsModule } from 'app/complaints/complaints.module';
import { ArtemisHomeModule } from 'app/home/home.module';
import { OrionOutdatedComponent } from 'app/shared/orion/outdated-plugin-warning/orion-outdated.component';
import { ArtemisTeamModule } from 'app/exercises/shared/team/team.module';
import { ArtemisQuizParticipationModule } from 'app/exercises/quiz/participate/quiz-participation.module';
import { ArtemisModelingAssessmentEditorModule } from 'app/exercises/modeling/assess/modeling-assessment-editor/modeling-assessment-editor.module';
import { QuizExerciseExportComponent } from 'app/exercises/quiz/manage/quiz-exercise-export.component';
import { ArtemisQuizManagementModule } from 'app/exercises/quiz/manage/quiz-management.module';
import { ArtemisModelingExerciseManagementModule } from 'app/exercises/modeling/manage/modeling-exercise-management.module';
import { ArtemisCourseModule } from 'app/course/manage/course.module';

@NgModule({
    imports: [
        BrowserModule,
        BrowserAnimationsModule,
        ArtemisSharedModule,
        ArtemisCoreModule,
        ArtemisHomeModule,
        ArtemisCourseModule,
        ArtemisAppRoutingModule,
        ArtemisConnectionNotificationModule,
        GuidedTourModule,
        ArtemisLegalModule,
        ArtemisParticipationModule,
        ArtemisTeamModule,
        ArtemisOverviewModule,
        ArtemisAccountModule,
        ArtemisQuizParticipationModule,
        ArtemisQuizManagementModule,
        ArtemisCourseScoresModule,
        ArtemisExerciseScoresModule,
        ArtemisTextModule,
        ArtemisTextAssessmentModule,
        ArtemisFileUploadParticipationModule,
        ArtemisFileUploadAssessmentModule,
        ArtemisModelingAssessmentEditorModule,
        ArtemisModelingExerciseManagementModule,
        ArtemisModelingParticipationModule,
        ArtemisInstructorCourseStatsDashboardModule,
        ArtemisInstructorExerciseStatsDashboardModule,
        ArtemisTutorCourseDashboardModule,
        ArtemisTutorExerciseDashboardModule,
        ArtemisSystemNotificationModule,
        ArtemisComplaintsModule,
        ArtemisComplaintsForTutorModule,
        ArtemisNotificationModule,
        ArtemisExampleTextSubmissionModule,
        ArtemisHeaderExercisePageWithDetailsModule,
        ArtemisListOfComplaintsModule,
    ],
    declarations: [
        JhiMainComponent,
        NavbarComponent,
        ErrorComponent,
        OrionOutdatedComponent,
        PageRibbonComponent,
        ActiveMenuDirective,
        FooterComponent,
        NotificationContainerComponent,
        SystemNotificationComponent,
        // TODO: this should be much lower in the hierarchy
        QuizExerciseExportComponent,
    ],
    bootstrap: [JhiMainComponent],
})
export class ArtemisAppModule {}
