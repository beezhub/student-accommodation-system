import { GenderRestriction } from './building-attributes.model';

export interface RoomAttributes {
  id: number;
  roomId: number;
  accessibilityCompliant: boolean;
  genderRestriction: GenderRestriction;
  desirabilityOverride?: number;
}
