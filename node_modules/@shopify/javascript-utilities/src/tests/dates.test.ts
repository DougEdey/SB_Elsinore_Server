import { Weekdays, getWeeksForMonth, abbreviationForWeekday } from '../dates';

describe('abbreviationForWeekday()', () => {
  it('abbreviates the word correctly', () => {
    expect(abbreviationForWeekday(Weekdays.Sunday)).toBe('Su');
    expect(abbreviationForWeekday(Weekdays.Monday)).toBe('Mo');
    expect(abbreviationForWeekday(Weekdays.Tuesday)).toBe('Tu');
    expect(abbreviationForWeekday(Weekdays.Wednesday)).toBe('We');
    expect(abbreviationForWeekday(Weekdays.Thursday)).toBe('Th');
    expect(abbreviationForWeekday(Weekdays.Friday)).toBe('Fr');
    expect(abbreviationForWeekday(Weekdays.Saturday)).toBe('Sa');
  });
});

describe('getWeeksForMonth()', () => {
  it('starts the week on Sunday by default', () => {
    const weeks = getWeeksForMonth(1, 2018);
    weeks.map((week) => {
      const startDay = week[0];
      if (startDay !== null) {
        expect(startDay.getDay()).toBe(Weekdays.Sunday);
      }
    });
  });

  it('always has 7 value for each weeks', () => {
    const weeks = getWeeksForMonth(6, 2018);
    weeks.map((week) => {
      expect(week).toHaveLength(7);
    });
  });

  it('first day of the week is the one passed as argument', () => {
    [
      Weekdays.Sunday,
      Weekdays.Monday,
      Weekdays.Tuesday,
      Weekdays.Wednesday,
      Weekdays.Thursday,
      Weekdays.Friday,
      Weekdays.Saturday,
    ].map((weekDay) => {
      const weeks = getWeeksForMonth(8, 2018, weekDay);
      weeks.map((week) => {
        const startDay = week[0];
        if (startDay !== null) {
          expect(startDay.getDay()).toBe(weekDay);
        } else {
          expect(startDay).toBeNull();
        }
      });
    });
  });

  it('sets values to null before first day of month', () => {
    const weeks = getWeeksForMonth(6, 2018, Weekdays.Monday);
    expect(weeks[0][0]).toBeNull();
    expect(weeks[0][1]).toBeNull();
    expect(weeks[0][2]).toBeNull();
    expect(weeks[0][3]).toBeNull();
    expect(weeks[0][4]).toBeNull();
    expect(weeks[0][5]).toBeNull();
    expect(weeks[0][6]).not.toBeNull();
  });
});
