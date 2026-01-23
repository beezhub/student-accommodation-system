export interface AllocationRule {
  id: number;
  buildingId?: number;
  ruleName: string;
  preferenceMatchWeight: number;
  desirabilityWeight: number;
  accessibilityWeight: number;
  active: boolean;
  priority: number;
  createdAt: Date;
}
