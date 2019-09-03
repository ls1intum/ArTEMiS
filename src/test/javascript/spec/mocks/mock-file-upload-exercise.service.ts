import { of } from 'rxjs';
import { FileUploadExercise } from 'app/entities/file-upload-exercise/file-upload-exercise.model';
import { HttpResponse } from '@angular/common/http';

export const fileUploadExercise = new FileUploadExercise();
fileUploadExercise.id = 2;
fileUploadExercise.title = 'some title';
fileUploadExercise.maxScore = 20;

export class MockFileUploadExerciseService {
    create = (fileUploadExercise: FileUploadExercise) => of();
    update = (fileUploadExercise: FileUploadExercise, exerciseId: number, req?: any) => of();
    find = (id: number) => of(new HttpResponse({ body: fileUploadExercise }));
    query = (req?: any) => of();
    delete = (id: number) => of();
}
