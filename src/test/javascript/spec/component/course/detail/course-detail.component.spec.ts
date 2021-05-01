import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { ArtemisTestModule } from '../../../test.module';
import { CourseDetailComponent } from 'app/course/manage/detail/course-detail.component';
import { TranslateService } from '@ngx-translate/core';
import { ArtemisTranslatePipe } from 'app/shared/pipes/artemis-translate.pipe.ts';
import { MockComponent, MockDirective, MockPipe, MockProvider } from 'ng-mocks';
import { SecuredImageComponent } from 'app/shared/image/secured-image.component';
import { MockSyncStorage } from '../../../helpers/mocks/service/mock-sync-storage.service';
import { LocalStorageService, SessionStorageService } from 'ngx-webstorage';
import { MockTranslateService } from '../../../helpers/mocks/service/mock-translate.service';
import * as moment from 'moment';
import * as chai from 'chai';
import * as sinonChai from 'sinon-chai';
import * as sinon from 'sinon';
import { JhiAlertService, JhiTranslateDirective } from 'ng-jhipster';
import { MockRouterLinkDirective } from '../../lecture-unit/lecture-unit-management.component.spec';
import { DeleteButtonDirective } from 'app/shared/delete-dialog/delete-button.directive';
import { AlertComponent } from 'app/shared/alert/alert.component';
import { AlertErrorComponent } from 'app/shared/alert/alert-error.component';
import { ArtemisDatePipe } from 'app/shared/pipes/artemis-date.pipe';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { CourseExamArchiveButtonComponent } from 'app/shared/components/course-exam-archive-button/course-exam-archive-button.component';
import { HasAnyAuthorityDirective } from 'app/shared/auth/has-any-authority.directive';
import { CourseManagementService } from 'app/course/manage/course-management.service';
import { HttpResponse } from '@angular/common/http';
import { JhiEventManager } from 'ng-jhipster';
import { CourseDetailDoughnutChartComponent } from 'app/course/manage/detail/course-detail-doughnut-chart.component';
import { CourseDetailBarChartComponent } from 'app/course/manage/detail/course-detail-bar-chart.component';
import { CourseManagementDetailViewDto } from 'app/course/manage/course-management-detail-view-dto.model';
import { RouterTestingModule } from '@angular/router/testing';

chai.use(sinonChai);
const expect = chai.expect;

describe('Course Management Detail Component', () => {
    let component: CourseDetailComponent;
    let fixture: ComponentFixture<CourseDetailComponent>;
    let courseService: CourseManagementService;
    let eventManager: JhiEventManager;

    const route = { params: of({ courseId: 1 }) };
    const course = { id: 123, title: 'Course Title', isAtLeastInstructor: true, endDate: moment().subtract(5, 'minutes'), courseArchivePath: 'some-path' };
    const dtoMock = {
        course,
        numberOfStudentsInCourse: 100,
        numberOfTeachingAssistantsInCourse: 5,
        numberOfEditorsInCourse: 5,
        numberOfInstructorsInCourse: 10,
        // assessments
        currentPercentageAssessments: 50,
        currentAbsoluteAssessments: 10,
        currentMaxAssessments: 20,
        // complaints
        currentPercentageComplaints: 60,
        currentAbsoluteComplaints: 6,
        currentMaxComplaints: 10,
        // feedback Request
        currentPercentageMoreFeedbacks: 70,
        currentAbsoluteMoreFeedbacks: 14,
        currentMaxMoreFeedbacks: 20,
        // average score
        currentPercentageAverageScore: 90,
        currentAbsoluteAverageScore: 90,
        currentMaxAverageScore: 100,
        activeStudents: [4, 10, 14, 35],
    } as CourseManagementDetailViewDto;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [ArtemisTestModule, RouterTestingModule.withRoutes([])],
            declarations: [
                CourseDetailComponent,
                MockComponent(SecuredImageComponent),
                MockRouterLinkDirective,
                MockPipe(ArtemisTranslatePipe),
                MockDirective(DeleteButtonDirective),
                MockComponent(AlertErrorComponent),
                MockDirective(AlertComponent),
                MockPipe(ArtemisDatePipe),
                MockDirective(JhiTranslateDirective),
                MockComponent(CourseExamArchiveButtonComponent),
                MockDirective(HasAnyAuthorityDirective),
                MockComponent(CourseDetailDoughnutChartComponent),
                MockComponent(CourseDetailBarChartComponent),
            ],
            providers: [
                { provide: ActivatedRoute, useValue: route },
                { provide: LocalStorageService, useClass: MockSyncStorage },
                { provide: SessionStorageService, useClass: MockSyncStorage },
                { provide: TranslateService, useClass: MockTranslateService },
                MockProvider(JhiAlertService),
                MockProvider(NgbModal),
                MockProvider(CourseManagementService),
            ],
        }).compileComponents();
        fixture = TestBed.createComponent(CourseDetailComponent);
        component = fixture.componentInstance;
        courseService = fixture.debugElement.injector.get(CourseManagementService);
        eventManager = fixture.debugElement.injector.get(JhiEventManager);
    });

    beforeEach(fakeAsync(() => {
        const getStub = sinon.stub(courseService, 'getCourseForDetailView');
        getStub.returns(of(new HttpResponse({ body: dtoMock })));

        component.ngOnInit();
        tick();
    }));

    afterEach(function () {
        sinon.restore();
    });

    it('Should call registerChangeInCourses on init', () => {
        const registerSpy = sinon.spy(component, 'registerChangeInCourses');

        fixture.detectChanges();
        component.ngOnInit();
        expect(component.courseDTO).to.deep.equal(dtoMock);
        expect(component.course).to.deep.equal(dtoMock.course);
        expect(registerSpy).to.have.been.called;
    });

    it('should destroy event subscriber onDestroy', () => {
        component.ngOnDestroy();
        expect(eventManager.destroy).to.have.been.called;
    });

    it('should broadcast course modification on delete', () => {
        const deleteStub = sinon.stub(courseService, 'delete');
        deleteStub.returns(of(new HttpResponse({})));

        const courseId = 444;
        component.deleteCourse(courseId);

        expect(deleteStub).to.have.been.calledWith(courseId);
        expect(eventManager.broadcast).to.have.been.calledWith({
            name: 'courseListModification',
            content: 'Deleted an course',
        });
    });
});
