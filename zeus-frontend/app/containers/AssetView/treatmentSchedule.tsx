import * as React from "react";
import {
  Classes,
} from "@blueprintjs/core";
import * as moment from "moment";
import * as _ from "underscore";
import * as classNames from "classnames";
import Pane from "../../components/Pane";
import { Asset } from "../../models";

interface Props {
  asset: Asset;
}

const TreatmentSchedule: React.SFC<Props> = (props) => (
  <Pane title="Treatment schedule" iconName="calendar">
    <div style={{ height: "300px" }}>
      <table
        className={classNames(Classes.TABLE, Classes.INTERACTIVE, Classes.TABLE_STRIPED, Classes.TABLE_CONDENSED)}
        style={{ width: "100%", tableLayout: "fixed" }}
      >
        <thead>
          <tr>
            <th width="20%">#</th>
            <th>Date</th>
            <th>Type</th>
          </tr>
        </thead>
        <tbody>
          {_.map(props.asset._model_treatments, treatment => (
            <tr key={treatment["number"]}>
              <td>{treatment["number"]}</td>
              <td>
                  {moment(treatment["date"]).format("Do MMM YYYY")}
                  &nbsp;
                  <span className={classNames(
                    Classes.ICON_STANDARD,
                    Classes.INTENT_SUCCESS,
                    { [Classes.iconClass("endorsed")]: treatment["date"] < Date.now() },
                  )} />
              </td>
              <td>{treatment["type"] || "???"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  </Pane>
);


export default TreatmentSchedule;
