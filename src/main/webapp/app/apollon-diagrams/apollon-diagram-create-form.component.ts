import { AfterViewInit, Component, ElementRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { JhiAlertService } from 'ng-jhipster';
import { ApollonDiagram, ApollonDiagramService } from 'app/entities/apollon-diagram';

@Component({
    selector: 'jhi-apollon-diagram-create-form',
    templateUrl: './apollon-diagram-create-form.component.html',
    providers: [ApollonDiagramService],
})
export class ApollonDiagramCreateFormComponent implements AfterViewInit {
    apollonDiagram: ApollonDiagram;
    isSaving: boolean;
    @ViewChild('titleInput', { static: false }) titleInput: ElementRef;

    constructor(private activeModal: NgbActiveModal, private apollonDiagramService: ApollonDiagramService, private router: Router, private jhiAlertService: JhiAlertService) {}

    /**
     * Adds focus on the title input field
     */
    ngAfterViewInit() {
        this.titleInput.nativeElement.focus();
    }

    /**
     * Saves the diagram
     */
    save() {
        this.isSaving = true;
        this.apollonDiagramService.create(this.apollonDiagram).subscribe(
            response => {
                const newDiagram = response.body as ApollonDiagram;
                this.isSaving = false;
                this.dismiss();
                this.router.navigate(['apollon-diagrams', newDiagram.id]);
            },
            response => {
                this.jhiAlertService.error('artemisApp.apollonDiagram.create.error');
            },
        );
    }

    /**
     * Closes the modal
     */
    dismiss() {
        this.activeModal.dismiss('cancel');
    }
}
