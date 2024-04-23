import {MINUTE} from "~/lib/timeUtils";

export function calculateBoosterDuration(duration: number, biology: number): number {
    return duration * (1 + 0.2 * biology);
}

export function calculateAcceleratedSkillpointsOmega(bonus: number, duration: number): number {
    return bonus * 1.5 * duration / MINUTE;
}

export function calculateAcceleratedSkillpointsAlpha(bonus: number, duration: number): number {
    return calculateAcceleratedSkillpointsOmega(bonus, duration) / 2;
}
