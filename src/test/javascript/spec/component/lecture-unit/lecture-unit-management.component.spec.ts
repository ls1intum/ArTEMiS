import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LectureUnitManagementComponent } from 'app/lecture/lecture-unit/lecture-unit-management/lecture-unit-management.component';
import { AttachmentUnit } from 'app/entities/lecture-unit/attachmentUnit.model';
import { TranslatePipe } from '@ngx-translate/core';
import * as chai from 'chai';
import * as sinonChai from 'sinon-chai';
import { ExerciseUnit } from 'app/entities/lecture-unit/exerciseUnit.model';
import * as sinon from 'sinon';
import { MockPipe } from 'ng-mocks/dist/lib/mock-pipe/mock-pipe';
import { Component, EventEmitter, Output } from '@angular/core';
import { ExerciseUnitComponent } from 'app/overview/course-lectures/exercise-unit/exercise-unit.component';
import { MockComponent, MockDirective, MockProvider } from 'ng-mocks';
import { AttachmentUnitComponent } from 'app/overview/course-lectures/attachment-unit/attachment-unit.component';
import { VideoUnitComponent } from 'app/overview/course-lectures/video-unit/video-unit.component';
import { TextUnitComponent } from 'app/overview/course-lectures/text-unit/text-unit.component';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { MockRouter } from '../../helpers/mocks/mock-router';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { LectureUnitService } from 'app/lecture/lecture-unit/lecture-unit-management/lectureUnit.service';
import { LectureService } from 'app/lecture/lecture.service';
import { JhiAlertService } from 'ng-jhipster';
import { TextUnit } from 'app/entities/lecture-unit/textUnit.model';
import { VideoUnit } from 'app/entities/lecture-unit/videoUnit.model';
import { Lecture } from 'app/entities/lecture.model';
import { HttpResponse } from '@angular/common/http';
import { AlertComponent } from 'app/shared/alert/alert.component';
import { DeleteButtonDirective } from 'app/shared/delete-dialog/delete-button.directive';
import { HasAnyAuthorityDirective } from 'app/shared/auth/has-any-authority.directive';
import { of } from 'rxjs';
import { By } from '@angular/platform-browser';
import { ActionType } from 'app/shared/delete-dialog/delete-dialog.model';

chai.use(sinonChai);
const expect = chai.expect;

@Component({ selector: 'jhi-unit-creation-card', template: '' })
class UnitCreationCardStubComponent {
    @Output()
    createTextUnit: EventEmitter<any> = new EventEmitter<any>();
    @Output()
    createExerciseUnit: EventEmitter<any> = new EventEmitter<any>();
    @Output()
    createVideoUnit: EventEmitter<any> = new EventEmitter<any>();
    @Output()
    createAttachmentUnit: EventEmitter<any> = new EventEmitter<any>();
}

describe('LectureUnitManagementComponent', () => {
    let lectureUnitManagementComponent: LectureUnitManagementComponent;
    let lectureUnitManagementComponentFixture: ComponentFixture<LectureUnitManagementComponent>;
    let unitCreationCardStubComponent: UnitCreationCardStubComponent;

    let lectureService: LectureService;
    let lectureUnitService: LectureUnitService;
    let findLectureStub: sinon.SinonStub;
    let updateOrderStub: sinon.SinonStub;

    let attachmentUnit: AttachmentUnit;
    let exerciseUnit: ExerciseUnit;
    let textUnit: TextUnit;
    let videoUnit: VideoUnit;
    let lecture: Lecture;

    beforeEach(() => {
        return TestBed.configureTestingModule({
            imports: [],
            declarations: [
                LectureUnitManagementComponent,
                UnitCreationCardStubComponent,
                MockPipe(TranslatePipe),
                MockComponent(ExerciseUnitComponent),
                MockComponent(AttachmentUnitComponent),
                MockComponent(VideoUnitComponent),
                MockComponent(TextUnitComponent),
                MockComponent(FaIconComponent),
                MockComponent(AlertComponent),
                MockDirective(DeleteButtonDirective),
                MockDirective(HasAnyAuthorityDirective),
            ],
            providers: [
                MockProvider(LectureUnitService),
                MockProvider(LectureService),
                MockProvider(JhiAlertService),
                { provide: Router, useClass: MockRouter },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        params: {
                            subscribe: (fn: (value: Params) => void) =>
                                fn({
                                    lectureId: 1,
                                }),
                        },
                    },
                },
            ],
        })
            .compileComponents()
            .then(() => {
                lectureUnitManagementComponentFixture = TestBed.createComponent(LectureUnitManagementComponent);
                lectureUnitManagementComponent = lectureUnitManagementComponentFixture.componentInstance;
                lectureService = TestBed.inject(LectureService);
                lectureUnitService = TestBed.inject(LectureUnitService);

                findLectureStub = sinon.stub(lectureService, 'find');
                updateOrderStub = sinon.stub(lectureUnitService, 'updateOrder');

                textUnit = new TextUnit();
                textUnit.id = 0;
                videoUnit = new VideoUnit();
                videoUnit.id = 1;
                exerciseUnit = new ExerciseUnit();
                exerciseUnit.id = 2;
                attachmentUnit = new AttachmentUnit();
                attachmentUnit.id = 3;

                lecture = new Lecture();
                lecture.id = 0;
                lecture.lectureUnits = [textUnit, videoUnit, exerciseUnit, attachmentUnit];

                findLectureStub.returns(
                    of(
                        new HttpResponse({
                            body: lecture,
                            status: 200,
                        }),
                    ),
                );

                updateOrderStub.returns(
                    of(
                        new HttpResponse({
                            body: [],
                            status: 200,
                        }),
                    ),
                );

                lectureUnitManagementComponentFixture.detectChanges();
                unitCreationCardStubComponent = lectureUnitManagementComponentFixture.debugElement.query(By.directive(UnitCreationCardStubComponent)).componentInstance;
            });
    });
    it('should initialize', () => {
        lectureUnitManagementComponentFixture.detectChanges();
        expect(lectureUnitManagementComponent).to.be.ok;
    });

    it('should move down', () => {
        const originalOrder = [...lecture.lectureUnits!];
        lectureUnitManagementComponentFixture.detectChanges();
        const moveDownSpy = sinon.spy(lectureUnitManagementComponent, 'moveDown');
        const moveUpSpy = sinon.spy(lectureUnitManagementComponent, 'moveUp');
        const upButton = lectureUnitManagementComponentFixture.debugElement.query(By.css('#up-0'));
        expect(upButton).to.exist;
        upButton.nativeElement.click();
        expect(moveUpSpy).to.not.have.been.calledOnce;
        // not moved as first one
        expect(lectureUnitManagementComponent.lectureUnits[0].id).to.equal(originalOrder[0].id);
        const downButton = lectureUnitManagementComponentFixture.debugElement.query(By.css('#down-0'));
        expect(downButton).to.exist;
        downButton.nativeElement.click();
        expect(moveDownSpy).to.have.been.calledOnce;
        expect(lectureUnitManagementComponent.lectureUnits[0].id).to.equal(originalOrder[1].id);
        expect(lectureUnitManagementComponent.lectureUnits[1].id).to.equal(originalOrder[0].id);
    });

    it('should move up', () => {
        const originalOrder = [...lecture.lectureUnits!];
        lectureUnitManagementComponentFixture.detectChanges();
        const moveDownSpy = sinon.spy(lectureUnitManagementComponent, 'moveDown');
        const moveUpSpy = sinon.spy(lectureUnitManagementComponent, 'moveUp');
        const lastPosition = lectureUnitManagementComponent.lectureUnits.length - 1;
        const downButton = lectureUnitManagementComponentFixture.debugElement.query(By.css(`#down-${lastPosition}`));
        expect(downButton).to.exist;
        downButton.nativeElement.click();
        expect(moveDownSpy).to.not.have.been.calledOnce;

        expect(lectureUnitManagementComponent.lectureUnits[lastPosition].id).to.equal(originalOrder[lastPosition].id);
        const upButton = lectureUnitManagementComponentFixture.debugElement.query(By.css(`#up-${lastPosition}`));
        expect(upButton).to.exist;
        upButton.nativeElement.click();
        expect(moveUpSpy).to.have.been.calledOnce;
        expect(lectureUnitManagementComponent.lectureUnits[lastPosition].id).to.equal(originalOrder[lastPosition - 1].id);
    });

    it('should navigate to edit attachment unit page', () => {
        const editButtonClickedSpy = sinon.spy(lectureUnitManagementComponent, 'editButtonClicked');
        const buttons = lectureUnitManagementComponentFixture.debugElement.queryAll(By.css(`.edit`));
        for (const button of buttons) {
            button.nativeElement.click();
        }
        expect(editButtonClickedSpy).to.have.been.called;
    });

    it('should navigate to create exercise unit page', () => {
        const createExerciseUnitSpy = sinon.spy(lectureUnitManagementComponent, 'createExerciseUnit');
        unitCreationCardStubComponent.createExerciseUnit.emit();
        expect(createExerciseUnitSpy).to.have.been.called;
    });
    it('should navigate to create text unit page', () => {
        const createTextUnitSpy = sinon.spy(lectureUnitManagementComponent, 'createTextUnit');
        unitCreationCardStubComponent.createTextUnit.emit();
        expect(createTextUnitSpy).to.have.been.called;
    });
    it('should navigate to create video unit page', () => {
        const createVideoUnitSpy = sinon.spy(lectureUnitManagementComponent, 'createVideoUnit');
        unitCreationCardStubComponent.createVideoUnit.emit();
        expect(createVideoUnitSpy).to.have.been.called;
    });
    it('should navigate to create attachment unit page', () => {
        const createAttachmentUnitSpy = sinon.spy(lectureUnitManagementComponent, 'createAttachmentUnit');
        unitCreationCardStubComponent.createAttachmentUnit.emit();
        expect(createAttachmentUnitSpy).to.have.been.called;
    });
    it('should give the correct delete question translation key', () => {
        expect(lectureUnitManagementComponent.getDeleteQuestionKey(new AttachmentUnit())).to.equal('artemisApp.attachmentUnit.delete.question');
        expect(lectureUnitManagementComponent.getDeleteQuestionKey(new ExerciseUnit())).to.equal('artemisApp.exerciseUnit.delete.question');
        expect(lectureUnitManagementComponent.getDeleteQuestionKey(new TextUnit())).to.equal('artemisApp.textUnit.delete.question');
        expect(lectureUnitManagementComponent.getDeleteQuestionKey(new VideoUnit())).to.equal('artemisApp.videoUnit.delete.question');
    });

    it('should give the correct confirmation text translation key', () => {
        expect(lectureUnitManagementComponent.getDeleteConfirmationTextKey(new AttachmentUnit())).to.equal('artemisApp.attachmentUnit.delete.typeNameToConfirm');
        expect(lectureUnitManagementComponent.getDeleteConfirmationTextKey(new ExerciseUnit())).to.equal('artemisApp.exerciseUnit.delete.typeNameToConfirm');
        expect(lectureUnitManagementComponent.getDeleteConfirmationTextKey(new VideoUnit())).to.equal('artemisApp.videoUnit.delete.typeNameToConfirm');
        expect(lectureUnitManagementComponent.getDeleteConfirmationTextKey(new TextUnit())).to.equal('artemisApp.textUnit.delete.typeNameToConfirm');
    });

    it('should give the correct action type', () => {
        expect(lectureUnitManagementComponent.getActionType(new AttachmentUnit())).to.equal(ActionType.Delete);
        expect(lectureUnitManagementComponent.getActionType(new ExerciseUnit())).to.equal(ActionType.Unlink);
        expect(lectureUnitManagementComponent.getActionType(new TextUnit())).to.equal(ActionType.Delete);
        expect(lectureUnitManagementComponent.getActionType(new VideoUnit())).to.equal(ActionType.Delete);
    });
});
