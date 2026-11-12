import { ApplicationStep4AnswersService, Step4Answers } from './application-step4-answers.service';

describe('ApplicationStep4AnswersService', () => {
  const answers: Step4Answers = {
    studentNumber: 'S1',
    phoneNumber: '0821234567',
    dateOfBirth: new Date(2004, 2, 14),
    gender: 'Female',
    courseName: 'BSc',
    requiresAccessibility: true,
    preferredBuildingIds: [2, 5],
    specialRequirements: 'Ground floor',
  };
  let service: ApplicationStep4AnswersService;

  beforeEach(() => {
    sessionStorage.clear();
    service = new ApplicationStep4AnswersService();
  });

  afterEach(() => sessionStorage.clear());

  it('load_afterSave_returnsTheSameAnswers', () => {
    service.save(9, answers);

    expect(service.load(9)).toEqual(answers);
  });

  it('save_dateOfBirth_storesTheCalendarDayNotAUtcTimestamp', () => {
    service.save(9, answers);

    expect(sessionStorage.getItem('applicationStep4:9')).toContain('"dateOfBirth":"2004-03-14"');
  });

  it('save_usesSessionStorageNotLocalStorage', () => {
    service.save(9, answers);

    expect(localStorage.getItem('applicationStep4:9')).toBeNull();
  });

  it('load_anotherUser_returnsNull', () => {
    service.save(9, answers);

    expect(service.load(10)).toBeNull();
  });

  it('load_afterClear_returnsNull', () => {
    service.save(9, answers);
    service.clear(9);

    expect(service.load(9)).toBeNull();
  });

  it('load_corruptValue_returnsNullAndDropsIt', () => {
    sessionStorage.setItem('applicationStep4:9', '{not json');

    expect(service.load(9)).toBeNull();
    expect(sessionStorage.getItem('applicationStep4:9')).toBeNull();
  });

  it('load_noDateOfBirth_leavesItUnset', () => {
    service.save(9, { ...answers, dateOfBirth: null });

    expect(service.load(9)?.dateOfBirth).toBeUndefined();
  });
});
