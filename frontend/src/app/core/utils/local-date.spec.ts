import { parseLocalDate, toLocalDateString } from './local-date';

describe('local-date', () => {
  it('parseLocalDate_calendarDay_isLocalMidnightOfThatDay', () => {
    expect(parseLocalDate('2000-05-14')).toEqual(new Date(2000, 4, 14));
  });

  it('parseLocalDate_fullTimestamp_isThatInstant', () => {
    const instant = new Date(2000, 4, 14, 9, 30);

    expect(parseLocalDate(instant.toISOString())).toEqual(instant);
  });

  it('toLocalDateString_date_isItsLocalDay', () => {
    expect(toLocalDateString(new Date(2000, 4, 14))).toBe('2000-05-14');
    expect(toLocalDateString(new Date(2000, 4, 14, 23, 59))).toBe('2000-05-14');
  });

  it('toLocalDateString_paddedMonthAndDay', () => {
    expect(toLocalDateString(new Date(2001, 0, 2))).toBe('2001-01-02');
  });

  it('toLocalDateString_calendarDay_isUnchanged', () => {
    expect(toLocalDateString('2000-05-14')).toBe('2000-05-14');
  });

  it('toLocalDateString_fullTimestamp_isTheLocalDayItFallsOn', () => {
    expect(toLocalDateString(new Date(2000, 4, 14).toISOString())).toBe('2000-05-14');
  });
});
