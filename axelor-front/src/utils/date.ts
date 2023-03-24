import dayjs, { ManipulateType } from "dayjs";

export function addDate(date: Date | string, amount: number, unit: ManipulateType) {
  return dayjs(date).add(amount, unit).toDate();
}
export function getStartOf(date: Date | string, unit: ManipulateType) {
  return dayjs(date).startOf(unit).toDate();
}
export function getNextOf(date: Date | string, unit: ManipulateType) {
  return dayjs(date).add(1, unit).startOf(unit).toDate();
}
