/* tslint:disable max-line-length */
import { async, ComponentFixture, fakeAsync, inject, TestBed, tick } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs/Observable';
import { JhiEventManager } from 'ng-jhipster';

import { ArtemisTestModule } from '../../../test.module';
import { QuestionDialogComponent } from '../../../../../../main/webapp/app/entities/quiz-question/question-dialog.component';
import { QuestionService } from '../../../../../../main/webapp/app/entities/quiz-question/question.service';
import { QuizQuestion } from '../../../../../../main/webapp/app/entities/quiz-question/quiz-question.model';
import { QuizQuestionStatisticService } from '../../../../../../main/webapp/app/entities/quiz-question-statistic';
import { QuizExerciseService } from '../../../../../../main/webapp/app/entities/quiz-exercise';

describe('Component Tests', () => {
    describe('Question Management Dialog Component', () => {
        let comp: QuestionDialogComponent;
        let fixture: ComponentFixture<QuestionDialogComponent>;
        let service: QuestionService;
        let mockEventManager: any;
        let mockActiveModal: any;

        beforeEach(async(() => {
            TestBed.configureTestingModule({
                imports: [ArtemisTestModule],
                declarations: [QuestionDialogComponent],
                providers: [QuizQuestionStatisticService, QuizExerciseService, QuestionService],
            })
                .overrideTemplate(QuestionDialogComponent, '')
                .compileComponents();
        }));

        beforeEach(() => {
            fixture = TestBed.createComponent(QuestionDialogComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(QuestionService);
            mockEventManager = fixture.debugElement.injector.get(JhiEventManager);
            mockActiveModal = fixture.debugElement.injector.get(NgbActiveModal);
        });

        describe('save', () => {
            it('Should call update service on save for existing entity', inject(
                [],
                fakeAsync(() => {
                    // GIVEN
                    const entity = new QuizQuestion(123);
                    spyOn(service, 'update').and.returnValue(Observable.of(new HttpResponse({ body: entity })));
                    comp.question = entity;
                    // WHEN
                    comp.save();
                    tick(); // simulate async

                    // THEN
                    expect(service.update).toHaveBeenCalledWith(entity);
                    expect(comp.isSaving).toEqual(false);
                    expect(mockEventManager.broadcastSpy).toHaveBeenCalledWith({ name: 'questionListModification', content: 'OK' });
                    expect(mockActiveModal.dismissSpy).toHaveBeenCalled();
                }),
            ));

            it('Should call create service on save for new entity', inject(
                [],
                fakeAsync(() => {
                    // GIVEN
                    const entity = new QuizQuestion();
                    spyOn(service, 'create').and.returnValue(Observable.of(new HttpResponse({ body: entity })));
                    comp.question = entity;
                    // WHEN
                    comp.save();
                    tick(); // simulate async

                    // THEN
                    expect(service.create).toHaveBeenCalledWith(entity);
                    expect(comp.isSaving).toEqual(false);
                    expect(mockEventManager.broadcastSpy).toHaveBeenCalledWith({ name: 'questionListModification', content: 'OK' });
                    expect(mockActiveModal.dismissSpy).toHaveBeenCalled();
                }),
            ));
        });
    });
});
