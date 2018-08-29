import { BaseEntity, User } from './../../shared';
import { Exercise } from '../exercise';

export class LtiOutcomeUrl implements BaseEntity {

    public id: number;
    public url: string;
    public sourcedId: string;
    public user: User;
    public exercise: Exercise;

    constructor() {
    }
}
