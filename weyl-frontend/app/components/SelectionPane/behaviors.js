// TODO: remove this jank

const PREFERRED_TITLES = [
  "name",
];

const isUsable = (x, k) => (k in x) && (x[k].trim() !== "") && !x[k].match(/\d+/g);

const formatAddress = (x) => {
  const component = ["addr1", "addr2", "addr3", "addr4"].find(k => isUsable(x, k));
  return (component !== undefined)
    ? `${x[component]}, ${x.postcode}`
    : x.postcode;
};

const defaultTitle = (x) => {
  const title = PREFERRED_TITLES.find(k => k in x);
  return (title !== undefined) ? x[title] : "<< Unknown title >>";
};

const curatedTitles = {
  "London House Sales": formatAddress,
  "London Crime": (x) => `${x.crimetype} (${x.monthyear})`,
  "Buses": (x) => `Route ${x.route} (${x["vehicle id"]})`,
};

export {
  curatedTitles,
  defaultTitle,
};
