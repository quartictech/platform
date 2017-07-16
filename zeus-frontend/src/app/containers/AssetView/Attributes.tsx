import * as React from "react";
import {
  Classes,
  Intent,
  Popover,
  PopoverInteractionKind,
  Position,
  Tag,
} from "@blueprintjs/core";
import * as _ from "underscore";
import * as moment from "moment";
import * as numeral from "numeraljs";
import * as classNames from "classnames";
import Pane from "../../components/Pane";
import { Asset } from "../../models";
import { toTitleCase } from "../../helpers/Utils";
const s = require("./style.css");

interface AttributesProps {
  asset: Asset;
}

// Add 40px to compensate for not having a title bar
// tslint:disable-next-line:variable-name
const Attributes: React.SFC<AttributesProps> = props => (
  <Pane style={{ backgroundColor: "rgba(138, 155, 168, 0.15)" }}>
    <div style={{ height: "340px", padding: "10px" }}>
      <h1>{props.asset.RSL}</h1>
      <h2>{toTitleCase(props.asset["Road Name"])}</h2>
      <h5>({toTitleCase(props.asset["Section Description"])})</h5>
      <table className={classNames(Classes.TABLE)} style={{ width: "100%" }}>
        <tbody>{rows}</tbody>
      </table>
    </div>
  </Pane>
);

function rows(props: AttributesProps) {
  return _.map(
    {
      "Length": `${numeral(props.asset["Length"]).format("0")} m`,
      "Next treatment": treatmentSchedule(props.asset["_model_treatments"]),
    },
    (v, k: string) => <tr key={k}><td className={s["attribute-name"]}>{k}</td><td>{v}</td></tr>,
  );
}

function treatmentSchedule(treatments: any[]) {
  const nextTreatment = _.find(treatments, t => t["date"] > Date.now());

  return (
    <div>
      {nextTreatment ? moment(nextTreatment["date"]).format("Do MMM YYYY") : "None scheduled"}
      &nbsp;
      <Popover
        content={treatmentTable(treatments)}
        interactionKind={PopoverInteractionKind.HOVER}
        popoverClassName={Classes.DARK}
        position={Position.BOTTOM_LEFT}
      >
        <span
          className={classNames(Classes.ICON_STANDARD, Classes.INTENT_PRIMARY, Classes.iconClass("info-sign"))}
          style={{ paddingLeft: "10px" }}
        />
      </Popover>
    </div>
  );
}

function treatmentTable(treatments: any[]) {
  return (
    <table
      className={classNames(Classes.TABLE, Classes.INTERACTIVE, Classes.TABLE_STRIPED, Classes.TABLE_CONDENSED)}
    >
      <thead>
        <tr>
          <th width="20%">#</th>
          <th>Date</th>
          <th>Type</th>
        </tr>
      </thead>
      <tbody>
        {_.map(treatments, treatment => (
          <tr key={treatment["number"]}>
            <td>{treatment["number"]}</td>
            <td>
                {moment(treatment["date"]).format("Do MMM YYYY")}
                &nbsp;
                <span
                  className={classNames(
                    Classes.ICON_STANDARD,
                    Classes.INTENT_SUCCESS,
                    { [Classes.iconClass("endorsed")]: treatment["date"] < Date.now() },
                  )}
                />
            </td>
            <td>{treatment["type"] || "???"}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

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
