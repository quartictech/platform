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

const defaultBehavior = {
  title: defaultTitle,
  blessed: [],
};

const curatedBehaviors = {
  "Jamcams": {
    title: (x) => x.view,
    blessed: [
      "location",
      "date",
      "postcode",
    ],
    imageUrl: "file",
  },

  "London Boroughs": {
    title: defaultTitle,
    blessed: [
      "hectares",
      "political control in council",
      "average age 2015",
      "crime rates per thousand population 2014/15",
    ],
  },

  "Postcode Districts": {
    title: (x) => x.name,
    blessed: [
      "gid",
    ],
  },

  "Public Land Assets": {
    title: (x) => x.holdingname,
    blessed: [
      "description",
      "assetcategory",
      "owner",
    ],
  },

  "London House Sales": {
    title: formatAddress,
    blessed: [
      "addr1",
      "addr2",
      "addr3",
      "addr4",
      "postcode",
      "price",
      "dateprocessed",
    ],
  },

  "Nightlife": {
    title: (x) => x.name,
    blessed: [
      "operator",
      "amenity",
      "building",
    ],
  },

  "London Crime": {
    title: (x) => `${x.crimetype} (${x.monthyear})`,
    blessed: [
      "location",
      "fallswithin",
      "lastoutcomecat",
    ],
  },

  "Road Disruptions": {
    title: (x) => `${x.location}`,
    blessed: [
      "severity",
      "category",
      "subcategory",
      "currentUpdate",
    ],
  },

  "Outdoor Advertising Stock London": {
    title: (x) => `${x.Name}`,
    blessed: [
      "Price",
      "Is Digital",
    ],
    imageUrl: "street_view",
  },

  "Buses": {
    title: (x) => `Route ${x.route} (${x["vehicle id"]})`,
    blessed: [
      "route",
      "vehicle id",
    ],
  },
};

export {
  defaultBehavior,
  curatedBehaviors,
};
