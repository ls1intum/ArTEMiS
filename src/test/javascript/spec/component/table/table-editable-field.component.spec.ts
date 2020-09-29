import { ComponentFixture, fakeAsync, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { TranslateModule } from '@ngx-translate/core';
import { DebugElement } from '@angular/core';
import * as chai from 'chai';
import { ArtemisTestModule } from '../../test.module';
import { ArtemisTableModule } from 'app/shared/table/table.module';
import { TableEditableFieldComponent } from 'app/shared/table/table-editable-field.component';

const expect = chai.expect;

describe('TableEditableFieldComponent', () => {
    let comp: TableEditableFieldComponent<any>;
    let fixture: ComponentFixture<TableEditableFieldComponent<any>>;
    let debugElement: DebugElement;

    const tableInputValue = '.table-editable-field__input';

    beforeEach(async () => {
        return TestBed.configureTestingModule({
            imports: [TranslateModule.forRoot(), ArtemisTestModule, ArtemisTableModule],
        })
            .compileComponents()
            .then(() => {
                fixture = TestBed.createComponent(TableEditableFieldComponent);
                comp = fixture.componentInstance;
                debugElement = fixture.debugElement;
            });
    });

    it('should render value as provided', () => {
        const value = 'test';

        comp.value = value;
        fixture.detectChanges();

        const tableInput = debugElement.query(By.css(tableInputValue));

        expect(tableInput).to.exist;
        expect(tableInput.nativeElement.value).to.equal(value);
    });

    it('should show input and fire update event on enter', fakeAsync(() => {
        const value = 'test';
        const fakeUpdateValue = { emit: jest.fn(() => {}) } as any;

        comp.value = value;
        comp.onValueUpdate = fakeUpdateValue;
        fixture.detectChanges();

        const tableInput = debugElement.query(By.css(tableInputValue));
        expect(tableInput).to.exist;
        expect(tableInput.nativeElement.value).to.equal(value);

        tableInput.nativeElement.dispatchEvent(new Event('blur'));
        expect(fakeUpdateValue.emit.mock.calls.length).to.equal(1);
    }));
});
