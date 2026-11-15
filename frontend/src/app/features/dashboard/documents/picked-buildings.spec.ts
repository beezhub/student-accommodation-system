import { pickedBuildingIds } from './picked-buildings';

describe('pickedBuildingIds', () => {
  it('noParameter_isEmpty', () => {
    expect(pickedBuildingIds(null)).toEqual([]);
    expect(pickedBuildingIds('')).toEqual([]);
  });

  it('commaSeparatedIds_areReadInOrder', () => {
    expect(pickedBuildingIds('5,6')).toEqual([5, 6]);
  });

  it('repeatsAndAnythingThatIsNotAnId_areLeftOut', () => {
    expect(pickedBuildingIds('5,abc,,-1,2.5,5, 7')).toEqual([5, 7]);
  });

  it('moreThanAFormTakes_keepsTheFirstThree', () => {
    expect(pickedBuildingIds('1,2,3,4')).toEqual([1, 2, 3]);
  });
});
