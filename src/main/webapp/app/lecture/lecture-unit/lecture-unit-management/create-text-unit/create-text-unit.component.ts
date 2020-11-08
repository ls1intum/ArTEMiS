import { Component, OnInit } from '@angular/core';
import { TextUnit } from 'app/entities/lecture-unit/textUnit.model';
import { TextUnitService } from 'app/lecture/lecture-unit/lecture-unit-management/textUnit.service';
import { ActivatedRoute, Router } from '@angular/router';
import { JhiAlertService } from 'ng-jhipster';
import { TextUnitFormData } from 'app/lecture/lecture-unit/lecture-unit-management/text-unit-form/text-unit-form.component';
import { onError } from 'app/shared/util/global.utils';
import { finalize } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
    selector: 'jhi-create-text-unit',
    templateUrl: './create-text-unit.component.html',
    styles: [],
})
export class CreateTextUnitComponent implements OnInit {
    textUnitToCreate: TextUnit = new TextUnit();
    isLoading: boolean;
    lectureId: number;
    courseId: number;
    constructor(private activatedRoute: ActivatedRoute, private router: Router, private textUnitService: TextUnitService, private alertService: JhiAlertService) {}

    ngOnInit(): void {
        this.activatedRoute.paramMap.subscribe((params) => {
            this.lectureId = Number(params.get('lectureId'));
            this.courseId = Number(params.get('courseId'));
        });
        this.textUnitToCreate = new TextUnit();
    }

    createTextUnit(formData: TextUnitFormData) {
        if (!formData?.name) {
            return;
        }

        const { name, releaseDate, content } = formData;

        this.textUnitToCreate.name = name;
        this.textUnitToCreate.releaseDate = releaseDate;
        this.textUnitToCreate.content = content;

        this.isLoading = true;

        this.textUnitService
            .create(this.textUnitToCreate!, this.lectureId)
            .pipe(
                finalize(() => {
                    this.isLoading = false;
                }),
            )
            .subscribe(
                () => {
                    this.router.navigate(['../../'], { relativeTo: this.activatedRoute });
                },
                (res: HttpErrorResponse) => onError(this.alertService, res),
            );
    }
}
