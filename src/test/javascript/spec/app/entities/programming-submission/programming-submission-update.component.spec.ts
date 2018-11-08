/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';

import { ArTeMiSTestModule } from '../../../test.module';
import { ProgrammingSubmissionUpdateComponent } from 'app/entities/programming-submission/programming-submission-update.component';
import { ProgrammingSubmissionService } from 'app/entities/programming-submission/programming-submission.service';
import { ProgrammingSubmission } from 'app/shared/model/programming-submission.model';

describe('Component Tests', () => {
    describe('ProgrammingSubmission Management Update Component', () => {
        let comp: ProgrammingSubmissionUpdateComponent;
        let fixture: ComponentFixture<ProgrammingSubmissionUpdateComponent>;
        let service: ProgrammingSubmissionService;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [ArTeMiSTestModule],
                declarations: [ProgrammingSubmissionUpdateComponent]
            })
                .overrideTemplate(ProgrammingSubmissionUpdateComponent, '')
                .compileComponents();

            fixture = TestBed.createComponent(ProgrammingSubmissionUpdateComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(ProgrammingSubmissionService);
        });

        describe('save', () => {
            it('Should call update service on save for existing entity', fakeAsync(() => {
                // GIVEN
                const entity = new ProgrammingSubmission(123);
                spyOn(service, 'update').and.returnValue(of(new HttpResponse({ body: entity })));
                comp.programmingSubmission = entity;
                // WHEN
                comp.save();
                tick(); // simulate async

                // THEN
                expect(service.update).toHaveBeenCalledWith(entity);
                expect(comp.isSaving).toEqual(false);
            }));

            it('Should call create service on save for new entity', fakeAsync(() => {
                // GIVEN
                const entity = new ProgrammingSubmission();
                spyOn(service, 'create').and.returnValue(of(new HttpResponse({ body: entity })));
                comp.programmingSubmission = entity;
                // WHEN
                comp.save();
                tick(); // simulate async

                // THEN
                expect(service.create).toHaveBeenCalledWith(entity);
                expect(comp.isSaving).toEqual(false);
            }));
        });
    });
});
