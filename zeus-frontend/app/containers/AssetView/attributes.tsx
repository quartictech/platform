import * as React from "react";
import {
  Classes,
  Intent,
  Tag,
} from "@blueprintjs/core";
import * as _ from "underscore";
import * as numeral from "numeraljs";
import * as classNames from "classnames";
import Pane from "../../components/Pane";
import { Asset } from "../../models";
import { toTitleCase } from "../../helpers/Utils";
const s = require("./style.css");

interface AttributesProps {
  asset: Asset;
}

// Add an extra 40px because no title
const Attributes: React.SFC<AttributesProps> = (props) => (
  <Pane style={{ backgroundColor: "rgba(138, 155, 168, 0.15)" }}>
    <div style={{ height: "340px", padding: "10px" }}>
      <h1>{props.asset.RSL}</h1>
      <table className={classNames(Classes.TABLE)} style={{ width: "100%"}}>
        <tbody>
          {
            _.map({
              "Road name": toTitleCase(props.asset["Road Name"]),
              "Section description": toTitleCase(props.asset["Section Description"]),
              "Length (m)": numeral(props.asset["Length"]).format("0"),
            }, (v, k: string) => <tr key={k}><td className={s["attribute-name"]}>{k}</td><td>{v}</td></tr>)
          }
          {_.map(props.asset._stats, (v, k) => renderStat(k, numeral(v[0]).format("0.0"), v[1]))}
        </tbody>
      </table>
    </div>
  </Pane>
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
