import { BaseEntity } from 'app/shared';
import { DragAndDropQuestion } from '../drag-and-drop-question';

export class DragItem implements BaseEntity {
    public id: number;
    public tempID: number;
    public pictureFilePath: string | null;
    public text: string | null;
    public question: DragAndDropQuestion;
    public invalid = false; // default value

    constructor() {}
}
