import React from "react";
import {
  Classes,
} from "@blueprintjs/core";
import * as _ from "underscore";

const Select = (props) => (
  <label className={Classes.LABEL} htmlFor={props.label}>
    {props.label}
    <div className={Classes.SELECT}>
      <select
        id={props.label}
        value={props.selectedKey || ""}
        onChange={(e) => props.onChange(e.target.value === "" ? null : e.target.value)}
        className={Classes.FILL}
        disabled={props.disabled}
      >
        {props.default && <option key="" value="">{props.default}</option>}
        {
          _.map(toMap(props.entries), (v, k) => <option key={k} value={k}>{v}</option>)
        }
      </select>
    </div>
  </label>
);

Select.defaultProps = {
  disabled: false,
};

const toMap = (entries) => (_.isArray(entries)
  ? _.object(_.map(entries, x => [x, x]))
  : entries);

export default Select;
