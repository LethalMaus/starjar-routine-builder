import { newItem } from "../src/parent-plan.js";
export const fullTask = {
  ...newItem("task"),
  name: "Brush teeth",
  icon: "CleanHands",
  badges: Array.from({ length: 20 }, (_, i) => ({
    completionsRequired: i + 1,
    title: "Achievement " + (i + 1),
    bonusPoints: i,
  })),
  streakBonus: {
    bonusPoints: 5,
    everyValue: 1,
    everyUnit: "DAY",
    forValue: 5,
    forUnit: "DAY",
  },
  reminder: {
    frequencyValue: 1,
    frequencyUnit: "WEEK",
    daysOfWeek: [1, 3, 5],
    time: "07:30",
    startDate: 1800000000000,
    endType: "AFTER_OCCURRENCES",
    endDate: null,
    endOccurrences: 10,
  },
  isUrgent: true,
  dueByDate: 1800010000000,
  urgencyBonus: 2,
};
