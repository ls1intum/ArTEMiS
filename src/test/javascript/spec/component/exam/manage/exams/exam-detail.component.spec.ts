import * as chai from 'chai';
import * as sinonChai from 'sinon-chai';
import * as sinon from 'sinon';
import { ComponentFixture, fakeAsync, TestBed } from '@angular/core/testing';
import { TranslatePipe } from '@ngx-translate/core';
import { MockComponent, MockDirective, MockPipe, MockProvider } from 'ng-mocks';
import { AlertComponent } from 'app/shared/alert/alert.component';
import { ActivatedRoute, Data } from '@angular/router';
import { JhiTranslateDirective } from 'ng-jhipster';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { RouterTestingModule } from '@angular/router/testing';
import { By } from '@angular/platform-browser';
import { Location } from '@angular/common';
import { ExamDetailComponent } from 'app/exam/manage/exams/exam-detail.component';
import { AlertErrorComponent } from 'app/shared/alert/alert-error.component';
import { Exam } from 'app/entities/exam.model';
import { AccountService } from 'app/core/auth/account.service';
import { ArtemisMarkdownService } from 'app/shared/markdown.service';
import { ArtemisDatePipe } from 'app/shared/pipes/artemis-date.pipe';
import { Course } from 'app/entities/course.model';
import { Component } from '@angular/core';

chai.use(sinonChai);
const expect = chai.expect;

@Component({
    template: '',
})
class DummyComponent {}

describe('ExamDetailComponent', () => {
    let examDetailComponentFixture: ComponentFixture<ExamDetailComponent>;
    let examDetailComponent: ExamDetailComponent;

    const exampleHTML = '<h1>Sample Markdown</h1>';
    const exam = new Exam();
    exam.id = 1;
    exam.course = new Course();
    exam.course.id = 1;
    exam.title = 'Example Exam';

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                RouterTestingModule.withRoutes([
                    { path: 'course-management/:courseId/exams/:examId/edit', component: DummyComponent },
                    { path: 'course-management/:courseId/exams/:examId/student-exams', component: DummyComponent },
                ]),
            ],
            declarations: [
                ExamDetailComponent,
                DummyComponent,
                MockPipe(TranslatePipe),
                MockPipe(ArtemisDatePipe),
                MockComponent(AlertComponent),
                MockComponent(AlertErrorComponent),
                MockComponent(FaIconComponent),
                MockDirective(JhiTranslateDirective),
            ],
            providers: [
                {
                    provide: ActivatedRoute,
                    useValue: {
                        data: {
                            subscribe: (fn: (value: Data) => void) =>
                                fn({
                                    exam,
                                }),
                        },
                    },
                },
                MockProvider(AccountService, {
                    isAtLeastInstructorInCourse: () => true,
                }),
                MockProvider(ArtemisMarkdownService, {
                    safeHtmlForMarkdown: () => exampleHTML,
                }),
            ],
            schemas: [],
        })
            .compileComponents()
            .then(() => {
                examDetailComponentFixture = TestBed.createComponent(ExamDetailComponent);
                examDetailComponent = examDetailComponentFixture.componentInstance;
            });
    });

    afterEach(function () {
        sinon.restore();
    });

    it('should load exam from route and display it to user', () => {
        examDetailComponentFixture.detectChanges();
        expect(examDetailComponent).to.be.ok;
        // stand in for other properties too who are simply loaded from the exam and displayed in spans
        const titleSpan = examDetailComponentFixture.debugElement.query(By.css('#examTitle')).nativeElement;
        expect(titleSpan).to.be.ok;
        expect(titleSpan.innerHTML).to.equal(exam.title);
    });

    it('should correctly route to edit subpage', fakeAsync(() => {
        const location = examDetailComponentFixture.debugElement.injector.get(Location);
        examDetailComponentFixture.detectChanges();
        const editButton = examDetailComponentFixture.debugElement.query(By.css('#editButton')).nativeElement;
        editButton.click();
        examDetailComponentFixture.whenStable().then(() => {
            expect(location.path()).to.equal('/course-management/1/exams/1/edit');
        });
    }));

    it('should correctly route to student exams subpage', fakeAsync(() => {
        const location = examDetailComponentFixture.debugElement.injector.get(Location);
        examDetailComponentFixture.detectChanges();
        const studentExamsButton = examDetailComponentFixture.debugElement.query(By.css('#studentExamsButton')).nativeElement;
        studentExamsButton.click();
        examDetailComponentFixture.whenStable().then(() => {
            expect(location.path()).to.equal('/course-management/1/exams/1/student-exams');
        });
    }));
});
