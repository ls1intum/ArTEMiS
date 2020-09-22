import { BaseEntity } from 'app/shared/model/base-entity';
import { DragAndDropSubmittedAnswer } from 'app/entities/quiz/drag-and-drop-submitted-answer.model';
import { DragAndDropQuestion } from 'app/entities/quiz/drag-and-drop-question.model';
import { DragItem } from 'app/entities/quiz/drag-item.model';
import { DropLocation } from 'app/entities/quiz/drop-location.model';

export class DragAndDropMapping implements BaseEntity {
    public id?: number;
    public tempID?: number;
    public dragItemIndex?: number;
    public dropLocationIndex?: number;
    public invalid?: boolean;
    public submittedAnswer?: DragAndDropSubmittedAnswer;
    public question?: DragAndDropQuestion;
    public dragItem?: DragItem;
    public dropLocation?: DropLocation;

    constructor(dragItem?: DragItem, dropLocation?: DropLocation) {
        this.dragItem = dragItem;
        this.dropLocation = dropLocation;
        this.invalid = false; // default value
    }
}
