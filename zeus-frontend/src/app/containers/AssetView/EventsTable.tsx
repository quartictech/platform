import * as React from "react";
import {
  Classes,
} from "@blueprintjs/core";
import * as moment from "moment";
import * as classNames from "classnames";
import Pane from "../../components/Pane";
import { Asset } from "../../models";
const s = require("./style.css");

interface EventsTableProps {
  asset: Asset;
}

// tslint:disable-next-line:variable-name
const EventsTable: React.SFC<EventsTableProps> = (props) => {
  const treatments = props.asset._treatments
      .map(t => Object.assign(t, { type: "treatment", _date: t["Estimated Completion Date"] }));

  const jobs = props.asset._jobs
      .map(j => Object.assign(j, { type: "job", _date: j["Start Date"] }));

  const jobsGeo = props.asset._jobs_geo
      .map(j => Object.assign(j, { type: "job_geo", _date: j["Start Date"] }));

  const events = treatments.concat(jobs).concat(jobsGeo);

  events.sort((a, b) => a._date - b._date);

  return (
    <Pane title="Events" iconName="timeline-events">
      <table
        className={classNames(Classes.TABLE, Classes.INTERACTIVE, Classes.TABLE_STRIPED, Classes.TABLE_CONDENSED)}
        style={{ width: "100%", tableLayout: "fixed" }}
      >
        <thead>
          <tr>
            <th width={120}>Job No.</th>
            <th width={120}>Date</th>
            <th width={200}>Type</th>
            <th className={s.eventsTableDetailsColumns}>Details</th>
          </tr>
        </thead>
        <tbody>
          {events.map((e, idx) => renderEventRow(idx, e))}
        </tbody>
      </table>
    </Pane>
  );
};

function renderEventRow(idx, event) {
  if (event.type === "job" || event.type === "job_geo") {
    return (
      <tr key={idx}>
        <td>
            { event.type === "job" ? <span className="pt-icon-standard pt-icon-person"></span> :
              <span className="pt-icon-standard pt-icon-geosearch"></span> } {event["Number"]}
        </td>

        <td>
          {event._date ? moment(event._date).format("Do MMM YYYY") : null}
        </td>

        <td>
          { event["Type"] }
        </td>

        <td>
          {event["Notes"]}
        </td>
      </tr>);
  } else if (event.type === "treatment") {
    return (
      <tr key={idx}>
        <td>
          <span className="pt-icon-standard pt-icon-tint"></span> {event["Confirm Number"]}
        </td>

        <td>
          {event._date ? moment(event._date).format("Do MMM YYYY") : null}
        </td>

        <td>
          {event["Treatment"]} <br />
        </td>

        <td>
          {event["Notes"]}
        </td>
      </tr>);
  }
}

export default EventsTable;
