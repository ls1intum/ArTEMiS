import { Component, EventEmitter, Input, Output } from '@angular/core';

export enum ButtonType {
    PRIMARY = 'btn-primary',
    SECONDARY = 'btn-secondary',
    SUCCESS = 'btn-success',
    WARNING = 'btn-warning',
    ERROR = 'btn-danger',
    INFO = 'btn-info',
}

export enum ButtonSize {
    SMALL = 'btn-sm',
    MEDIUM = 'btn-md',
    LARGE = 'btn-lg',
}

/**
 * A generic button component that has a disabled & loading state.
 * The only event output is the click.
 *
 * Can be used as a button with text and/or icon.
 */
@Component({
    selector: 'jhi-button',
    template: `
        <button [ngClass]="['jhi-btn', 'btn', btnType, btnSize]" ngbTooltip="{{ tooltip | translate }}" [disabled]="disabled || isLoading" (click)="onClick.emit($event)">
            <fa-icon class="jhi-btn__loading" *ngIf="isLoading" icon="circle-notch" [spin]="true" size="sm"></fa-icon>
            <fa-icon class="jhi-btn__icon" *ngIf="icon && !isLoading" [icon]="icon" size="sm"></fa-icon>
            <span class="jhi-btn__title" [class.ml-1]="icon || isLoading" *ngIf="title" [jhiTranslate]="title"></span>
        </button>
    `,
})
export class ButtonComponent {
    @Input() btnType = ButtonType.PRIMARY;
    @Input() btnSize = ButtonSize.MEDIUM;
    // Fa-icon name.
    @Input() icon: string;
    // Translation placeholders, will be translated in the component.
    @Input() title: string;
    @Input() tooltip: string;

    @Input() disabled = false;
    @Input() isLoading = false;

    @Output() onClick = new EventEmitter<MouseEvent>();
}
