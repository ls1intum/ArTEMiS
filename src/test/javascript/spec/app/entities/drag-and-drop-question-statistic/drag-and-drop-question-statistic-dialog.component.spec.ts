/* tslint:disable max-line-length */
import { async, ComponentFixture, fakeAsync, inject, TestBed, tick } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs/Observable';
import { JhiEventManager } from 'ng-jhipster';

import { ArtemisTestModule } from '../../../test.module';
import { DragAndDropQuestionStatisticDialogComponent } from '../../../../../../main/webapp/app/entities/drag-and-drop-question-statistic/drag-and-drop-question-statistic-dialog.component';
import { DragAndDropQuestionStatisticService } from '../../../../../../main/webapp/app/entities/drag-and-drop-question-statistic/drag-and-drop-question-statistic.service';
import { DragAndDropQuestionStatistic } from '../../../../../../main/webapp/app/entities/drag-and-drop-question-statistic/drag-and-drop-question-statistic.model';

describe('Component Tests', () => {
    describe('DragAndDropQuestionStatistic Management Dialog Component', () => {
        let comp: DragAndDropQuestionStatisticDialogComponent;
        let fixture: ComponentFixture<DragAndDropQuestionStatisticDialogComponent>;
        let service: DragAndDropQuestionStatisticService;
        let mockEventManager: any;
        let mockActiveModal: any;

        beforeEach(async(() => {
            TestBed.configureTestingModule({
                imports: [ArtemisTestModule],
                declarations: [DragAndDropQuestionStatisticDialogComponent],
                providers: [DragAndDropQuestionStatisticService],
            })
                .overrideTemplate(DragAndDropQuestionStatisticDialogComponent, '')
                .compileComponents();
        }));

        beforeEach(() => {
            fixture = TestBed.createComponent(DragAndDropQuestionStatisticDialogComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(DragAndDropQuestionStatisticService);
            mockEventManager = fixture.debugElement.injector.get(JhiEventManager);
            mockActiveModal = fixture.debugElement.injector.get(NgbActiveModal);
        });

        describe('save', () => {
            it('Should call update service on save for existing entity', inject(
                [],
                fakeAsync(() => {
                    // GIVEN
                    const entity = new DragAndDropQuestionStatistic(123);
                    spyOn(service, 'update').and.returnValue(Observable.of(new HttpResponse({ body: entity })));
                    comp.dragAndDropQuestionStatistic = entity;
                    // WHEN
                    comp.save();
                    tick(); // simulate async

                    // THEN
                    expect(service.update).toHaveBeenCalledWith(entity);
                    expect(comp.isSaving).toEqual(false);
                    expect(mockEventManager.broadcastSpy).toHaveBeenCalledWith({ name: 'dragAndDropQuestionStatisticListModification', content: 'OK' });
                    expect(mockActiveModal.dismissSpy).toHaveBeenCalled();
                }),
            ));

            it('Should call create service on save for new entity', inject(
                [],
                fakeAsync(() => {
                    // GIVEN
                    const entity = new DragAndDropQuestionStatistic();
                    spyOn(service, 'create').and.returnValue(Observable.of(new HttpResponse({ body: entity })));
                    comp.dragAndDropQuestionStatistic = entity;
                    // WHEN
                    comp.save();
                    tick(); // simulate async

                    // THEN
                    expect(service.create).toHaveBeenCalledWith(entity);
                    expect(comp.isSaving).toEqual(false);
                    expect(mockEventManager.broadcastSpy).toHaveBeenCalledWith({ name: 'dragAndDropQuestionStatisticListModification', content: 'OK' });
                    expect(mockActiveModal.dismissSpy).toHaveBeenCalled();
                }),
            ));
        });
    });
});
