import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { ArtemisExamScoresModule } from 'app/exam/exam-scores/exam-scores.module';
import { ExamManagementComponent } from 'app/exam/manage/exam-management.component';
import { examManagementState } from 'app/exam/manage/exam-management.route';
import { ExamUpdateComponent } from 'app/exam/manage/exams/exam-update.component';
import { ExamDetailComponent } from 'app/exam/manage/exams/exam-detail.component';
import { ExerciseGroupsComponent } from 'app/exam/manage/exercise-groups/exercise-groups.component';
import { ExerciseGroupUpdateComponent } from 'app/exam/manage/exercise-groups/exercise-group-update.component';
import { ExerciseGroupDetailComponent } from 'app/exam/manage/exercise-groups/exercise-group-detail.component';
import { ExamStudentsComponent } from 'app/exam/manage/students/exam-students.component';
import { StudentExamsComponent } from 'app/exam/manage/student-exams/student-exams.component';
import { StudentExamDetailComponent } from 'app/exam/manage/student-exams/student-exam-detail.component';
import { ArtemisTextExerciseModule } from 'app/exercises/text/manage/text-exercise/text-exercise.module';
import { ArtemisFileUploadExerciseManagementModule } from 'app/exercises/file-upload/manage/file-upload-exercise-management.module';
import { ArtemisProgrammingExerciseManagementModule } from 'app/exercises/programming/manage/programming-exercise-management.module';
import { ArtemisQuizManagementModule } from 'app/exercises/quiz/manage/quiz-management.module';
import { ArtemisSharedModule } from 'app/shared/shared.module';
import { ArtemisDataTableModule } from 'app/shared/data-table/data-table.module';
import { NgxDatatableModule } from '@swimlane/ngx-datatable';
import { FormDateTimePickerModule } from 'app/shared/date-time-picker/date-time-picker.module';
import { ArtemisSharedComponentModule } from 'app/shared/components/shared-component.module';
import { ArtemisMarkdownEditorModule } from 'app/shared/markdown-editor/markdown-editor.module';
import { MomentModule } from 'ngx-moment';
import { DurationPipe } from 'app/shared/pipes/artemis-duration.pipe';
import { StudentsExamImportDialogComponent } from 'app/exam/manage/students/students-exam-import-dialog/students-exam-import-dialog.component';
import { StudentsExamImportButtonComponent } from 'app/exam/manage/students/students-exam-import-dialog/students-exam-import-button.component';
import { StudentExamStatusComponent } from 'app/exam/manage/student-exams/student-exam-status.component';
import { StudentExamSummaryComponent } from 'app/exam/manage/student-exams/student-exam-summary.component';
import { ArtemisParticipationSummaryModule } from 'app/exam/participate/summary/exam-participation-summary.module';
import { ExamExerciseRowButtonsComponent } from 'app/exercises/shared/exam-exercise-row-buttons/exam-exercise-row-buttons.component';

const ENTITY_STATES = [...examManagementState];

@NgModule({
    // TODO: For better modularization we could define an exercise module with the corresponding exam routes
    imports: [
        RouterModule.forChild(ENTITY_STATES),
        ArtemisTextExerciseModule,
        ArtemisExamScoresModule,
        ArtemisSharedModule,
        FormDateTimePickerModule,
        ArtemisSharedComponentModule,
        ArtemisMarkdownEditorModule,
        NgxDatatableModule,
        ArtemisDataTableModule,
        ArtemisTextExerciseModule,
        ArtemisFileUploadExerciseManagementModule,
        ArtemisProgrammingExerciseManagementModule,
        ArtemisQuizManagementModule,
        MomentModule,
        ArtemisParticipationSummaryModule,
    ],
    declarations: [
        ExamManagementComponent,
        ExamUpdateComponent,
        ExamDetailComponent,
        ExerciseGroupsComponent,
        ExerciseGroupUpdateComponent,
        ExerciseGroupDetailComponent,
        ExamExerciseRowButtonsComponent,
        ExamStudentsComponent,
        StudentExamStatusComponent,
        StudentExamsComponent,
        StudentsExamImportDialogComponent,
        StudentsExamImportButtonComponent,
        StudentExamDetailComponent,
        DurationPipe,
        StudentExamSummaryComponent,
    ],
})
export class ArtemisExamManagementModule {}
