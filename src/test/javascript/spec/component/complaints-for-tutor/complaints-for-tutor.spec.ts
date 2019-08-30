import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import * as chai from 'chai';
import { ComplaintsForTutorComponent } from 'app/complaints-for-tutor';
import { BrowserModule, By } from '@angular/platform-browser';
import { ComplaintService } from 'app/entities/complaint/complaint.service';
import { MockComplaintService } from '../../mocks/mock-complaint.service';
import { MockAlertService } from '../../helpers/mock-alert.service';
import { JhiAlertService } from 'ng-jhipster';
import { ArtemisSharedModule } from 'app/shared';
import { MomentModule } from 'ngx-moment';
import { ClipboardModule } from 'ngx-clipboard';
import { DebugElement } from '@angular/core';
import { HttpClientModule } from '@angular/common/http';
import { ComplaintResponseService } from 'app/entities/complaint-response/complaint-response.service';
import { Complaint } from 'app/entities/complaint';

const expect = chai.expect;
describe('ComplaintsForTutorComponent', () => {
    let comp: ComplaintsForTutorComponent;
    let fixture: ComponentFixture<ComplaintsForTutorComponent>;
    let debugElement: DebugElement;

    const complaint = new Complaint();
    complaint.id = 12;
    complaint.accepted = true;
    complaint.complaintText = 'Random';

    const complaint2 = new Complaint();
    complaint2.id = 11;
    complaint2.complaintText = '';

    const subscribe = {
        subscribe: (fn: (value: any) => void) => {
            fn({
                body: complaint,
            });
        },
    };

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            imports: [BrowserModule, ArtemisSharedModule, MomentModule, ClipboardModule, HttpClientModule],
            declarations: [ComplaintsForTutorComponent],
            providers: [
                {
                    provide: ComplaintService,
                    useClass: MockComplaintService,
                },
                {
                    provide: JhiAlertService,
                    useClass: MockAlertService,
                },
                {
                    provide: ComplaintResponseService,
                    useValue: {
                        findByComplaintId() {
                            return subscribe;
                        },
                    },
                },
            ],
        })
            .compileComponents()
            .then(() => {
                fixture = TestBed.createComponent(ComplaintsForTutorComponent);
                comp = fixture.componentInstance;
                debugElement = fixture.debugElement;
                comp.complaint = complaint;
                comp.isAllowedToRespond = true;
            });
    }));

    it('should show alert and accept message when complaint is handled', () => {
        comp.ngOnInit();
        fixture.detectChanges();
        expect(comp.complaint.accepted).to.be.true;
        expect(comp.handled).to.be.true;
        expect(comp.complaintText).to.be.equal(complaint.complaintText);

        expect(debugElement.query(By.css('.alert.alert-info'))).to.not.be.null;
        expect(debugElement.query(By.css('.text-light.bg-success.small'))).to.not.be.null;
    });

    it('should not show alert and accept message when complaint is not handled', () => {
        comp.complaint = complaint2;
        comp.ngOnInit();
        fixture.detectChanges();
        expect(comp.handled).to.be.false;

        expect(debugElement.query(By.css('.alert.alert-info'))).to.be.null;
        expect(debugElement.query(By.css('.text-light.bg-success.small'))).to.be.null;
    });
});
