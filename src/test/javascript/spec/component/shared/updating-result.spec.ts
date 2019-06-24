import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LocalStorageService, SessionStorageService } from 'ngx-webstorage';
import { MomentModule } from 'ngx-moment';
import * as moment from 'moment';
import { TranslateModule } from '@ngx-translate/core';
import { AccountService, JhiLanguageHelper, WindowRef } from 'app/core';
import { ChangeDetectorRef, DebugElement, SimpleChange, SimpleChanges } from '@angular/core';
import { SinonStub, spy, stub } from 'sinon';
import { BehaviorSubject, of } from 'rxjs';
import * as chai from 'chai';
import * as sinonChai from 'sinon-chai';
import { AceEditorModule } from 'ng2-ace-editor';
import { TreeviewModule } from 'ngx-treeview';
import { CodeEditorFileService } from 'app/code-editor';
import { ArTEMiSTestModule } from '../../test.module';
import { MockParticipationWebsocketService, MockSyncStorage } from '../../mocks';
import { Result, ResultComponent, UpdatingResultComponent } from 'app/entities/result';
import { ArTEMiSSharedModule } from 'app/shared';
import { Participation, ParticipationWebsocketService } from 'app/entities/participation';
import { MockAccountService } from '../../mocks/mock-account.service';
import { Exercise } from 'app/entities/exercise';

chai.use(sinonChai);
const expect = chai.expect;

describe('UpdatingResultComponent', () => {
    let comp: UpdatingResultComponent;
    let fixture: ComponentFixture<UpdatingResultComponent>;
    let debugElement: DebugElement;
    let participationWebsocketService: ParticipationWebsocketService;

    let subscribeForLatestResultOfParticipationStub: SinonStub;
    let subscribeForLatestResultOfParticipationSubject: BehaviorSubject<Result>;

    const exercise = { id: 20 } as Exercise;
    const student = { id: 99 };
    const gradedResult1 = { id: 10, rated: true, completionDate: moment('2019-06-06T22:15:29.203+02:00') } as Result;
    const gradedResult2 = { id: 11, rated: true, completionDate: moment('2019-06-06T22:17:29.203+02:00') } as Result;
    const ungradedResult1 = { id: 12, rated: false, completionDate: moment('2019-06-06T22:25:29.203+02:00') } as Result;
    const ungradedResult2 = { id: 13, rated: false, completionDate: moment('2019-06-06T22:32:29.203+02:00') } as Result;
    const results = [gradedResult2, ungradedResult1, gradedResult1, ungradedResult2] as Result[];
    const initialParticipation = { id: 1, exercise, results, student } as Participation;
    const newGradedResult = { id: 14, rated: true } as Result;
    const newUngradedResult = { id: 15, rated: false } as Result;

    beforeEach(async () => {
        return TestBed.configureTestingModule({
            imports: [TranslateModule.forRoot(), ArTEMiSTestModule, ArTEMiSSharedModule, MomentModule],
            declarations: [UpdatingResultComponent, ResultComponent],
            providers: [
                JhiLanguageHelper,
                WindowRef,
                CodeEditorFileService,
                ChangeDetectorRef,
                { provide: AccountService, useClass: MockAccountService },
                { provide: ParticipationWebsocketService, useClass: MockParticipationWebsocketService },
                { provide: LocalStorageService, useClass: MockSyncStorage },
                { provide: SessionStorageService, useClass: MockSyncStorage },
            ],
        })
            .compileComponents()
            .then(() => {
                fixture = TestBed.createComponent(UpdatingResultComponent);
                comp = fixture.componentInstance;
                debugElement = fixture.debugElement;

                participationWebsocketService = debugElement.injector.get(ParticipationWebsocketService);
                subscribeForLatestResultOfParticipationSubject = new BehaviorSubject<Result>(null);

                subscribeForLatestResultOfParticipationStub = stub(participationWebsocketService, 'subscribeForLatestResultOfParticipation').returns(
                    subscribeForLatestResultOfParticipationSubject,
                );
            });
    });

    afterEach(() => {
        subscribeForLatestResultOfParticipationStub.restore();
        subscribeForLatestResultOfParticipationSubject = new BehaviorSubject<Result>(null);
        subscribeForLatestResultOfParticipationStub.returns(subscribeForLatestResultOfParticipationSubject);
    });

    const cleanInitializeGraded = (participation = initialParticipation) => {
        comp.participation = participation;
        const changes: SimpleChanges = {
            participation: new SimpleChange(undefined, participation, true),
        };
        comp.ngOnChanges(changes);
        fixture.detectChanges();
    };

    const cleanInitializeUngraded = (participation = initialParticipation) => {
        comp.participation = participation;
        comp.showUngradedResults = true;
        const changes: SimpleChanges = {
            participation: new SimpleChange(undefined, participation, true),
        };
        comp.ngOnChanges(changes);
        fixture.detectChanges();
    };

    it('should not try to subscribe for new results if no participation is provided', () => {
        const changes: SimpleChanges = {
            participation: new SimpleChange(undefined, undefined, true),
        };
        comp.ngOnChanges(changes);
        fixture.detectChanges();

        expect(subscribeForLatestResultOfParticipationStub).to.not.have.been.called;
        expect(comp.result).to.be.undefined;
    });

    it('should use the newest rated result of the provided participation and subscribe for new results', () => {
        cleanInitializeGraded();
        expect(subscribeForLatestResultOfParticipationStub).to.have.been.calledOnceWithExactly(initialParticipation.id);
        expect(comp.result.id).to.equal(gradedResult2.id);
    });

    it('should use the newest (un)rated result of the provided participation and subscribe for new results', () => {
        cleanInitializeUngraded();
        expect(subscribeForLatestResultOfParticipationStub).to.have.been.calledOnceWithExactly(initialParticipation.id);
        expect(comp.result.id).to.equal(ungradedResult2.id);
    });

    it('should react to rated, but not to unrated results if showUngradedResults is false', () => {
        cleanInitializeGraded();
        const currentResult = comp.result;
        subscribeForLatestResultOfParticipationSubject.next(newUngradedResult);
        expect(comp.result.id).to.equal(currentResult.id);
        subscribeForLatestResultOfParticipationSubject.next(newGradedResult);
        expect(comp.result.id).to.equal(newGradedResult.id);
    });

    it('should react to both rated and unrated results if showUngradedResults is true', async () => {
        cleanInitializeUngraded();
        subscribeForLatestResultOfParticipationSubject.next(newUngradedResult);
        expect(comp.result.id).to.equal(newUngradedResult.id);
        subscribeForLatestResultOfParticipationSubject.next(newGradedResult);
        expect(comp.result.id).to.equal(newGradedResult.id);
    });

    it('should update result and establish new websocket connection on participation change', () => {
        cleanInitializeGraded();
        const unsubscribeSpy = spy(comp.resultUpdateListener, 'unsubscribe');
        const newParticipation = { id: 80, exercise, student, results: [{ id: 1, rated: true }] } as Participation;
        cleanInitializeGraded(newParticipation);
        expect(unsubscribeSpy).to.have.been.calledOnce;
        expect(comp.result.id).to.equal(newParticipation.results[0].id);
        expect(subscribeForLatestResultOfParticipationStub).to.have.been.calledTwice;
        expect(subscribeForLatestResultOfParticipationStub).to.have.been.calledWithExactly(initialParticipation.id);
        expect(subscribeForLatestResultOfParticipationStub).to.have.been.calledWithExactly(newParticipation.id);

        subscribeForLatestResultOfParticipationSubject.next(newGradedResult);
        expect(comp.result.id).to.equal(newGradedResult.id);
    });
});
