import React from "react";

import {
  Button,
  Classes,
  Intent,
  Tag,
  Checkbox,
} from "@blueprintjs/core";
import { DateTimePicker } from "@blueprintjs/datetime";

import moment from "moment";

export class DateRangePicker extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor(props) {
    super(props);
    this.state = {
      startTime: props.startTime ? moment(props.startTime) : null,
      endTime: props.endTime ? moment(props.endTime) : null,
    };
  }

  render() {
    return (
      <div>
      <div style={{display: "flex", flexDirection: "row"}}>
        <div style={{ padding: 10 }}>
          <Checkbox
            checked={this.state.startTime != null}
            label="Start Time"
            onChange={this.onStartTimeToggle.bind(this)}
          />
          {this.state.startTime ?  <DateTimePicker
             className={Classes.ELEVATION_1}
             value={this.state.startTime.toDate()}
             onChange={this.onStartTimeChange.bind(this)}
            /> : null}
        </div>
        <div style={{ padding: 10 }}>
          <Checkbox checked={this.state.endTime != null} label="End Time" onChange={this.onEndTimeToggle.bind(this)} />
          {this.state.endTime ? <DateTimePicker
             className={Classes.ELEVATION_1}
             value={this.state.endTime.toDate()}
             onChange={this.onEndTimeChange.bind(this)}
          /> : null }
        </div>
      </div>
      <div className="pt-dialog-footer">
      <Button
        className="pt-popover-dismiss"
        style={{margin: 5}}
        iconName="refresh"
        text="Apply"
        onClick={this.onClickApply.bind(this)}
      />
  </div>
      </div>
    );
  }

  onClickApply() {
    this.props.onApply(
      this.state.startTime != null ? this.state.startTime.valueOf() : null,
      this.state.endTime != null ? this.state.endTime.valueOf() : null
    );
  }

  onStartTimeToggle() {
      if (this.state.startTime == null) {
          this.setState({ startTime: moment() });
      }
      else {
        this.setState({ startTime: null });
      }
  }

  onEndTimeToggle() {
    if (this.state.endTime == null) {
      this.setState({ endTime: moment() });
    }
    else {
      this.setState({ endTime: null });
    }
  }

  onStartTimeChange(startTime) {
    this.setState({ startTime: moment(startTime) });
  }

  onEndTimeChange(endTime) {
    this.setState({ endTime: moment(endTime) });
  }
}
