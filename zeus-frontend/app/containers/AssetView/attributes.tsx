import * as React from "react";
import {
  Classes,
  Intent,
  Tag,
} from "@blueprintjs/core";
import * as _ from "underscore";
import * as numeral from "numeraljs";
import * as classNames from "classnames";
import { Asset } from "../../models";
import { toTitleCase } from "../../helpers/Utils";
const s = require("./style.css");

interface AttributesProps {
  asset: Asset;
}

const Attributes: React.SFC<AttributesProps> = (props) => (
  <div>
    <div className={classNames(Classes.CALLOUT)} style={{ margin: "10px" }}>
      <h1>{props.asset.RSL}</h1>
      <table className={classNames(Classes.TABLE, Classes.TABLE_CONDENSED)} style={{ width: "100%"}}>
        <tbody>
          {
            _.map({
              "Road name": toTitleCase(props.asset["Road Name"]),
              "Section description": toTitleCase(props.asset["Section Description"]),
              "Link place": `${props.asset["Link"]} ${props.asset["Place"]}`,
              "Length (m)": numeral(props.asset["Length"]).format("0.00"),
              "Speed limit (mph)": props.asset["Speed Limit"],
            }, (v, k: string) => <tr key={k}><td className={s["attribute-name"]}>{k}</td><td>{v}</td></tr>)
          }
          {_.map(props.asset._stats, (v, k) => renderStat(k, numeral(v[0]).format("0.00"), v[1]))}
        </tbody>
      </table>
    </div>
  </div>
);

function renderStat(key, value, quartile) {
  let color = null;
  let tag = null;
  if (quartile > 0.9) {
    color = "rgba(219, 55, 55, 0.4)";   // Alpha-modified version of callout with INTENT_DANGER
    tag = <Tag style={{ float: "right" }} intent={Intent.DANGER}>Top 10% offender</Tag>;
  } else if (quartile > 0.67) {
    color = "rgba(217, 130, 43, 0.4)";  // Alpha-modified version of callout with INTENT_WARNING
    tag = <Tag style={{ float: "right" }} intent={Intent.WARNING}>Top 33% offender</Tag>;
  } else if (quartile < 0.25) {
    color = "rgba(15, 153, 96, 0.4)";  // Alpha-modified version of callout with INTENT_SUCCESS
    tag = <Tag style={{ float: "right" }} intent={Intent.SUCCESS}>Bottom 25% offender</Tag>;
  }
  return (
    <tr className={classNames(Classes.CALLOUT)} style={{ backgroundColor: color }}>
      <td className={s["attribute-name"]}>{key}</td>
      <td>{value} {tag}</td>
    </tr>
  );
}


export default Attributes;
