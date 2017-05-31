import HighestLowest2016 from "./highestLowest2016";

export interface Insight {
  componentClass: any;
  title: string;
  disabled: boolean;
}

const insights: { [name: string] : Insight } = {
  "highestLowest2016": {
    componentClass: HighestLowest2016,
    title: "Highest / lowest defects (2016)",
    disabled: false,
  },
  "predictions2017": {
    componentClass: null,
    title: "Predictions (2017)",
    disabled: true,
  },
  "smartOps": {
    componentClass: null,
    title: "SmartOps",
    disabled: true,
  }
}

export default insights;
