import {MINUTE} from "~/lib/timeUtils";

export function calculateSkillpoints(primaryAttr: number, secondaryAttr: number, duration: number, omega: boolean) {
    return (primaryAttr + secondaryAttr / 2) * duration / MINUTE * (omega ? 1 : 0.5);
}

export function calculateBoosterDuration(duration: number, biology: number): number {
    return duration * (1 + 0.2 * biology);
}

export function calculateAcceleratedSkillpoints(bonus: number, duration: number, omega: boolean): number {
    return calculateSkillpoints(bonus, bonus, duration, omega);
}
