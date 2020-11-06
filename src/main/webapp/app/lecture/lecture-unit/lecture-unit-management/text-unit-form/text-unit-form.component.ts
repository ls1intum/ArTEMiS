import { Component, EventEmitter, Input, OnChanges, OnInit, Output } from '@angular/core';
import { Moment } from 'moment';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

export interface TextUnitFormData {
    name?: string;
    releaseDate?: Moment;
    content?: string;
}

@Component({
    selector: 'jhi-text-unit-form',
    templateUrl: './text-unit-form.component.html',
    styles: [],
})
export class TextUnitFormComponent implements OnInit, OnChanges {
    @Input()
    formData: TextUnitFormData;
    @Input()
    isEditMode = false;

    @Output()
    formSubmitted: EventEmitter<TextUnitFormData> = new EventEmitter<TextUnitFormData>();
    form: FormGroup;
    // not included via reactive form as
    content: string;

    constructor(private fb: FormBuilder) {}

    get nameControl() {
        return this.form.get('name');
    }

    get releaseDateControl() {
        return this.form.get('releaseDate');
    }

    ngOnChanges(): void {
        this.initializeForm();
        if (this.isEditMode) {
            this.setFormValues(this.formData);
        }
    }

    ngOnInit(): void {
        this.initializeForm();
    }

    private initializeForm() {
        if (this.form) {
            return;
        }
        this.form = this.fb.group({
            name: [undefined, [Validators.required, Validators.maxLength(255)]],
            releaseDate: [undefined],
        });
    }

    private setFormValues(formData: TextUnitFormData) {
        this.form.patchValue(formData);
    }

    submitForm() {
        const textUnitFormData: TextUnitFormData = { ...this.form.value };
        this.formSubmitted.emit(textUnitFormData);
    }

    get isSubmitPossible() {
        return !this.form.invalid;
    }
}
