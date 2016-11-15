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
  "Jamcams": (x) => x.view,
  "Postcode Districts": (x) => x.name,
  "Public Land Assets": (x) => x.holdingname,
  "London House Sales": formatAddress,
  "Nightlife": (x) => x.name,
  "London Crime": (x) => `${x.crimetype} (${x.monthyear})`,
  "Road Disruptions": (x) => `${x.location}`,
  "Outdoor Advertising Stock London": (x) => `${x.Name}`,
  "Current Properties London": (x) => `${x.property_type}`,
  "Buses": (x) => `Route ${x.route} (${x["vehicle id"]})`,
};

// // TODO: delete once disruptions data in
// "Road Disruptions": {
//   blessed: [
//     "severity",
//     "category",
//     "subcategory",
//     "currentUpdate",
//   ],
// },
//
// // TODO: add properties integration
// "Current Properties London": {
//   blessed: [
//     "Price",
//     "Street Name",
//     "Description",
//     "Agent Name",
//   ],
//   imageUrl: "image_url",
// },
//

export {
  curatedTitles,
  defaultTitle,
};
