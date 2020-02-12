import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { NgbModal, NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { FormsModule } from '@angular/forms';
import { DebugElement } from '@angular/core';
import * as chai from 'chai';
import * as sinonChai from 'sinon-chai';
import { SinonStub, stub } from 'sinon';
import { of } from 'rxjs';
import { ArtemisTestModule } from '../../test.module';
import { ParticipationWebsocketService } from 'app/entities/participation/participation-websocket.service';
import { MockResultService } from '../../mocks/mock-result.service';
import { MockSyncStorage } from '../../mocks/mock-sync.storage';
import { MockParticipationWebsocketService } from '../../mocks/mock-participation-websocket.service';
import { MockProgrammingExerciseParticipationService } from '../../mocks/mock-programming-exercise-participation.service';
import { ArtemisSharedModule } from 'app/shared/shared.module';
import { BuildLogService } from 'app/programming-assessment/build-logs/build-log.service';
import { FormDateTimePickerModule } from 'app/shared/date-time-picker/date-time-picker.module';
import { User } from 'app/core/user/user.model';
import { AccountService } from 'app/core/auth/account.service';
import { LocalStorageService, SessionStorageService } from 'ngx-webstorage';
import { By } from '@angular/platform-browser';
import { JhiAlertService } from 'ng-jhipster';
import { MockComponent } from 'ng-mocks';
import { ProgrammingAssessmentManualResultDialogComponent } from 'app/programming-assessment/manual-result/programming-assessment-manual-result-dialog.component';
import { ResultService } from 'app/entities/result/result.service';
import { RepositoryFileService } from 'app/entities/repository/repository.service';
import { ProgrammingExerciseParticipationService } from 'app/entities/programming-exercise/services/programming-exercise-participation.service';
import { StudentParticipation } from 'app/entities/participation/student-participation.model';
import { ProgrammingAssessmentRepoExportButtonComponent } from 'app/programming-assessment/repo-export/programming-assessment-repo-export-button.component';
import { ProgrammingSubmission } from 'app/entities/programming-submission/programming-submission.model';
import { ComplaintsForTutorComponent } from 'app/complaints-for-tutor/complaints-for-tutor.component';
import { Feedback } from 'app/entities/feedback/feedback.model';
import { ProgrammingAssessmentManualResultService } from 'app/programming-assessment/manual-result/programming-assessment-manual-result.service';
import { ProgrammingExercise } from 'app/entities/programming-exercise/programming-exercise.model';
import { Complaint } from 'app/entities/complaint/complaint.model';
import { ComplaintService } from 'app/entities/complaint/complaint.service';
import { ExerciseHintService } from 'app/entities/exercise-hint/exercise-hint.service';
import { MockRepositoryFileService } from '../../mocks/mock-repository-file.service';
import { MockExerciseHintService } from '../../mocks/mock-exercise-hint.service';
import { MockNgbModalService } from '../../mocks/mock-ngb-modal.service';

chai.use(sinonChai);
const expect = chai.expect;

describe('ProgrammingAssessmentManualResultDialogComponent', () => {
    let comp: ProgrammingAssessmentManualResultDialogComponent;
    let fixture: ComponentFixture<ProgrammingAssessmentManualResultDialogComponent>;
    let debugElement: DebugElement;
    let programmingAssessmentManualResultService: ProgrammingAssessmentManualResultService;
    let complaintService: ComplaintService;
    let accountService: AccountService;

    let updateAfterComplaintStub: SinonStub;
    let findByResultId: SinonStub;
    let getIdentity: SinonStub;
    const user = <User>{ id: 1 };
    const result = <any>{
        feedbacks: [new Feedback()],
        participation: new StudentParticipation(),
        score: 80,
        successful: true,
        submission: new ProgrammingSubmission(),
        assessor: user,
        hasComplaint: true,
    };
    result.submission.id = 1;
    const complaint = <Complaint>{ id: 1, complaintText: 'Why only 80%?', result };
    const exercise = <ProgrammingExercise>{ id: 1, gradingInstructions: 'Grading Instructions' };

    beforeEach(async () => {
        return TestBed.configureTestingModule({
            imports: [TranslateModule.forRoot(), ArtemisTestModule, ArtemisSharedModule, NgbModule, FormDateTimePickerModule, FormsModule],
            declarations: [ProgrammingAssessmentManualResultDialogComponent, ComplaintsForTutorComponent, MockComponent(ProgrammingAssessmentRepoExportButtonComponent)],
            providers: [
                ProgrammingAssessmentManualResultService,
                ComplaintService,
                BuildLogService,
                AccountService,
                JhiAlertService,
                { provide: ResultService, useClass: MockResultService },
                {
                    provide: ProgrammingExerciseParticipationService,
                    useClass: MockProgrammingExerciseParticipationService,
                },
                { provide: ParticipationWebsocketService, useClass: MockParticipationWebsocketService },
                { provide: RepositoryFileService, useClass: MockRepositoryFileService },
                { provide: ExerciseHintService, useClass: MockExerciseHintService },
                { provide: NgbModal, useClass: MockNgbModalService },
                { provide: SessionStorageService, useClass: MockSyncStorage },
                { provide: LocalStorageService, useClass: MockSyncStorage },
            ],
        })
            .overrideModule(ArtemisTestModule, { set: { declarations: [], exports: [] } })
            .compileComponents()
            .then(() => {
                // Ignore console errors
                console.error = () => {
                    return false;
                };
                fixture = TestBed.createComponent(ProgrammingAssessmentManualResultDialogComponent);
                comp = fixture.componentInstance;
                debugElement = fixture.debugElement;
                programmingAssessmentManualResultService = debugElement.injector.get(ProgrammingAssessmentManualResultService);
                complaintService = debugElement.injector.get(ComplaintService);
                accountService = debugElement.injector.get(AccountService);
                updateAfterComplaintStub = stub(programmingAssessmentManualResultService, 'updateAfterComplaint');
                findByResultId = stub(complaintService, 'findByResultId');
                getIdentity = stub(accountService, 'identity');
            });
    });

    afterEach(() => {
        updateAfterComplaintStub.restore();
        findByResultId.restore();
    });

    it('should show complaint for result with complaint and check assessor', fakeAsync(() => {
        findByResultId.returns(of({ body: complaint }));
        getIdentity.returns(new Promise(resolve => resolve(user)));
        comp.result = result;
        comp.exercise = exercise;
        comp.ngOnInit();
        tick();
        expect(findByResultId.calledOnce).to.be.true;
        expect(comp.isAssessor).to.be.true;
        expect(comp.complaint).to.exist;
        fixture.detectChanges();
        const complaintsForm = debugElement.query(By.css('jhi-complaints-for-tutor-form'));
        expect(complaintsForm).to.exist;
        expect(comp.complaint).to.exist;
    }));

    it("should not show complaint when result doesn't have it", fakeAsync(() => {
        getIdentity.returns(new Promise(resolve => resolve(user)));
        result.hasComplaint = false;
        comp.result = result;
        comp.exercise = exercise;
        comp.ngOnInit();
        tick();
        expect(findByResultId.notCalled).to.be.true;
        expect(comp.complaint).to.not.exist;
        fixture.detectChanges();
        const complaintsForm = debugElement.query(By.css('jhi-complaints-for-tutor-form'));
        expect(complaintsForm).to.not.exist;
    }));
});
