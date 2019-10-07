import { BaseEntity } from 'app/shared';
import { Exercise } from '../exercise';
import { Moment } from 'moment';
import { Lecture } from 'app/entities/lecture';
import { TutorGroup } from 'app/entities/tutor-group';

export class Course implements BaseEntity {
    public id: number;
    public title: string;
    public description: string;
    public shortName: string;
    public studentGroupName: string;
    public teachingAssistantGroupName: string;
    public instructorGroupName: string;
    public startDate: Moment | null;
    public endDate: Moment | null;
    public color: string;
    public courseIcon: string;
    public onlineCourse = false; // default value
    public registrationEnabled = false; // default value
    public presentationScore = 0; // default value
    public maxComplaints: number;

    public exercises: Exercise[];
    public lectures: Lecture[];
    public tutorGroups: TutorGroup[];

    // helper attributes
    public isAtLeastTutor = false; // default value
    public isAtLeastInstructor = false; // default value
    public relativeScore: number;
    public absoluteScore: number;
    public maxScore: number;

    constructor() {}
}
