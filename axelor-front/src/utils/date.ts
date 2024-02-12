import dayjs, { Dayjs, ManipulateType } from "dayjs";

export function addDate(
  date: Date | string,
  amount: number,
  unit: ManipulateType,
) {
  return dayjs(date).add(amount, unit).toDate();
}

export function getStartOf(date: Dayjs | Date | string, unit: ManipulateType) {
  return dayjs(date).startOf(unit);
}

export function getStartOfAsDate(date: Date | string, unit: ManipulateType) {
  return getStartOf(date, unit).toDate();
}

export function getNextOf(date: Dayjs | Date | string, unit: ManipulateType) {
  return dayjs(date).add(1, unit).startOf(unit);
}

export function getNextOfAsDate(
  date: Dayjs | Date | string,
  unit: ManipulateType,
) {
  return getNextOf(date, unit).toDate();
}
