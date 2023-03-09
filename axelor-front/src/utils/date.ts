import moment from "moment";

export type TimeUnit = moment.unitOfTime.DurationConstructor;

export function addDate(date: Date | string, amount: number, unit: TimeUnit) {
  return moment(date).add(amount, unit).toDate();
}
export function getStartOf(date: Date | string, unit: TimeUnit) {
  return moment(date).startOf(unit).toDate();
}
export function getNextOf(date: Date | string, unit: TimeUnit) {
  return moment(date).add(1, unit).startOf(unit).toDate();
}
