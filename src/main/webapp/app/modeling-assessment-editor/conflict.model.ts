import { Score } from 'app/modeling-assessment-editor/score.model';
import { User } from 'app/core';
import { Feedback } from 'app/entities/feedback';

export class Conflict {
    conflictedElementId: string;
    conflictingFeedback: Feedback;
    scoresInConflict: Score[];
    initiator: User;
    id: string;
}
