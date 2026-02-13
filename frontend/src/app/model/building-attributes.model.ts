export type GenderRestriction = 'NONE' | 'MALE_ONLY' | 'FEMALE_ONLY';

export interface BuildingAttributes {
  id: number;
  buildingId: number;
  genderRestriction: GenderRestriction;
  accessibilityFeatures: string;
  desirabilityScore: number; // 1-10
  allowMixedGender: boolean;
}
