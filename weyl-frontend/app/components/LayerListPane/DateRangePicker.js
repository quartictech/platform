import React from "react";

import {
  Button,
  Classes,
  Checkbox,
} from "@blueprintjs/core";
import { DateTimePicker } from "@blueprintjs/datetime";

import moment from "moment";
import { formatDateTime } from "../../utils/time";

export class DateRangePicker extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor(props) {
    super(props);
    this.state = {
      startTime: props.startTime ? moment(props.startTime) : null,
      endTime: props.endTime ? moment(props.endTime) : null,
    };
  }

  startTimeError() {
    if (this.props.minTime && this.state.startTime != null && this.state.startTime < this.props.minTime) {
      return `start time is earlier than minimum in data: ${formatDateTime(this.props.minTime)}`;
    }
    return null;
  }

  endTimeError() {
    if (this.props.maxTime && this.state.endTime != null && this.state.endTime > this.props.maxTime) {
      return `end time is later than maximum in data: ${formatDateTime(this.props.maxTime)}`;
    }
    return null;
  }

  datePickerProps() {
    const props = {};
    if (this.props.minTime) {
      props.minDate = moment(this.props.minTime).startOf("day").toDate();
    }

    if (this.props.maxTime) {
      props.maxDate = moment(this.props.maxTime).endOf("day").toDate();
    }
    return props;
  }

  render() {
    return (
      <div>
        <div style={{ display: "flex", flexDirection: "row" }}>
          <div style={{ padding: 10, minHeight: 250, width: 300 }}>
            <Checkbox
              checked={this.state.startTime != null}
              label="Start Time"
              onChange={() => this.onStartTimeToggle()}
            />
            {this.state.startTime ?
              <DateTimePicker
                className={Classes.ELEVATION_1}
                value={this.state.startTime.toDate()}
                datePickerProps={this.datePickerProps()}
                onChange={(dt) => this.onStartTimeChange(dt)}
              /> : null}
              {this.startTimeError() ?
                <span style={{ margin: 10 }} className="pt-tag pt-intent-danger">{this.startTimeError()}</span> : null}
          </div>
          <div style={{ padding: 10, width: 300 }}>
            <Checkbox
              checked={this.state.endTime != null}
              label="End Time"
              onChange={() => this.onEndTimeToggle()}
            />
            {this.state.endTime ?
              <DateTimePicker
                className={Classes.ELEVATION_1}
                value={this.state.endTime.toDate()}
                datePickerProps={this.datePickerProps()}
                onChange={(dt) => this.onEndTimeChange(dt)}
              /> : null}
              {this.endTimeError() ?
                <span style={{ margin: 10 }} className="pt-tag pt-intent-danger">{this.endTimeError()}</span> : null}
          </div>
        </div>
        <div className="pt-dialog-footer">
          <Button
            className="pt-popover-dismiss"
            style={{ margin: 5 }}
            iconName="refresh"
            text="Apply"
            disabled={this.startTimeError() || this.endTimeError()}
            onClick={() => this.onClickApply()}
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
    return true;
  }

  getDefaultStartTime() {
    if (this.props.minTime) {
      return moment(this.props.minTime);
    }
    return moment();
  }

  getDefaultEndTime() {
    if (this.props.maxTime) {
      return moment(this.props.maxTime);
    }
    return moment();
  }

  onStartTimeToggle() {
    if (this.state.startTime == null) {
      this.setState({ startTime: this.getDefaultStartTime() });
    } else {
      this.setState({ startTime: null });
    }
  }

  onEndTimeToggle() {
    if (this.state.endTime == null) {
      this.setState({ endTime: this.getDefaultEndTime() });
    } else {
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
