import moment from "moment";

export function formatDateTime(dateTime) {
  return moment(dateTime).format("LL LTS");
}
