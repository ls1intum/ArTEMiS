import { Component, ElementRef, EventEmitter, Input, Output, ViewChild } from '@angular/core';

/**
 * An inline editing field to use for tables.
 */
@Component({
    selector: 'jhi-table-editable-field',
    styles: ['.table-editable-field {display: flex; align-items: center}', '.table-editable-field__input {flex: 2 1 auto;}'],
    template: `
        <div class="table-editable-field">
            <input
                #editingInput
                class="table-editable-field__input form-control mr-2"
                (blur)="sendValueUpdate($event)"
                (keyup.enter)="sendValueUpdate($event)"
                [value]="value"
                type="text"
            />
        </div>
    `,
})
export class TableEditableFieldComponent<T> {
    @ViewChild('editingInput', { static: false }) editingInput: ElementRef;

    @Input() value: T;
    @Output() onValueUpdate = new EventEmitter<T>();

    /**
     * Triggers a value update signal and delegates the task to method specified in the Output decorator,
     * sending in also the updated value of the object.
     * @param event The event that occurred.
     */
    sendValueUpdate(event: any) {
        this.onValueUpdate.emit(event.target.value);
    }
}
