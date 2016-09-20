// TODO: remove this jank

const PREFERRED_TITLES = [
  "name",
];

const isUsable = (x, k) => x.hasOwnProperty(k) && x[k].trim() !== "" && !x[k].match(/\d+/g);

const formatAddress = (x) => {
  const component = ["addr1", "addr2", "addr3", "addr4"].find(k => isUsable(x, k));
  return (component !== undefined)
    ? `${x[component]}, ${x.postcode}`
    : x.postcode;
};

const defaultTitle = (x) => {
  const title = PREFERRED_TITLES.find(k => x.hasOwnProperty(k));
  return (title !== undefined) ? x[title] : "<< Unknown title >>";
};

const defaultBehavior = {
  title: defaultTitle,
  blessed: []
};

const curatedBehaviors = {
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
};

export {
  defaultBehavior,
  curatedBehaviors,
};
