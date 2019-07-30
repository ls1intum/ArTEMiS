import { ProgrammingExercise } from 'app/entities/programming-exercise';
import { Participation, ParticipationType } from 'app/entities/participation/participation.model';

export class TemplateProgrammingExerciseParticipation extends Participation {
    public programmingExercise: ProgrammingExercise;
    public repositoryUrl: string;
    public buildPlanId: string;

    constructor() {
        super(ParticipationType.TEMPLATE);
    }
}
