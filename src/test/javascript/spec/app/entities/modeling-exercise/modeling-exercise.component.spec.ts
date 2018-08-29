/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, async } from '@angular/core/testing';
import { Observable } from 'rxjs/Observable';
import { HttpHeaders, HttpResponse } from '@angular/common/http';

import { ArTEMiSTestModule } from '../../../test.module';
import { ModelingExerciseComponent } from '../../../../../../main/webapp/app/entities/modeling-exercise/modeling-exercise.component';
import { ModelingExerciseService } from '../../../../../../main/webapp/app/entities/modeling-exercise/modeling-exercise.service';
import { ModelingExercise } from '../../../../../../main/webapp/app/entities/modeling-exercise/modeling-exercise.model';

describe('Component Tests', () => {

    describe('ModelingExercise Management Component', () => {
        let comp: ModelingExerciseComponent;
        let fixture: ComponentFixture<ModelingExerciseComponent>;
        let service: ModelingExerciseService;

        beforeEach(async(() => {
            TestBed.configureTestingModule({
                imports: [ArTEMiSTestModule],
                declarations: [ModelingExerciseComponent],
                providers: [
                    ModelingExerciseService
                ]
            })
            .overrideTemplate(ModelingExerciseComponent, '')
            .compileComponents();
        }));

        beforeEach(() => {
            fixture = TestBed.createComponent(ModelingExerciseComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(ModelingExerciseService);
        });

        describe('OnInit', () => {
            it('Should call load all on init', () => {
                // GIVEN
                const headers = new HttpHeaders().append('link', 'link;link');
                spyOn(service, 'query').and.returnValue(Observable.of(new HttpResponse({
                    body: [new ModelingExercise(123)],
                    headers
                })));

                // WHEN
                comp.ngOnInit();

                // THEN
                expect(service.query).toHaveBeenCalled();
                expect(comp.modelingExercises[0]).toEqual(jasmine.objectContaining({id: 123}));
            });
        });
    });

});
