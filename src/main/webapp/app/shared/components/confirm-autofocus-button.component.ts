import { Component, EventEmitter, Input, Output } from '@angular/core';
import { IconProp } from '@fortawesome/fontawesome-svg-core';
import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';

@Component({
    templateUrl: './confirm-autofocus-modal.component.html',
})
export class ConfirmAutofocusModalComponent {
    title: string;
    text: string;
    translateText: boolean;

    constructor(public modal: NgbActiveModal) {}
}

@Component({
    selector: 'jhi-confirm-button',
    template: ` <jhi-button [icon]="icon" [title]="title" [tooltip]="tooltip" [disabled]="disabled" [isLoading]="isLoading" (onClick)="onOpenConfirmationModal()"></jhi-button> `,
})
export class ConfirmAutofocusButtonComponent {
    @Input() icon: IconProp;
    @Input() title: string;
    @Input() tooltip: string;
    @Input() disabled = false;
    @Input() isLoading = false;

    @Input() confirmationTitle: string;
    @Input() confirmationText: string;
    @Input() translateText?: boolean;
    @Output() onConfirm = new EventEmitter<void>();
    @Output() onCancel = new EventEmitter<void>();

    constructor(private modalService: NgbModal) {}

    /**
     * open confirmation modal with text and title
     */
    onOpenConfirmationModal() {
        const modalRef: NgbModalRef = this.modalService.open(ConfirmAutofocusModalComponent as Component, { size: 'lg', backdrop: 'static' });
        modalRef.componentInstance.text = this.confirmationText;
        modalRef.componentInstance.title = this.confirmationTitle;
        if (this.translateText !== undefined) {
            modalRef.componentInstance.translateText = this.translateText;
        } else {
            modalRef.componentInstance.translateText = false;
        }
        modalRef.result.then(
            () => {
                this.onConfirm.emit();
            },
            () => {
                this.onCancel.emit();
            },
        );
    }
}
