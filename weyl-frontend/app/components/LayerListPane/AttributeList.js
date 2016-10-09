import React from "react";
import naturalsort from "javascript-natural-sort";
const $ = require("jquery");

const AttributeValue = ({
  value,
  checked,
  onClick,
}) => (
  <div className="item" key={value}>
    <div className="ui checked checkbox">
      <input type="checkbox" checked={checked} name={value} onChange={() => onClick(value)} />
      <label htmlFor={value}>{value}</label>
    </div>
  </div>
);

const AttributeValueList = ({
  attribute,
  values,
  uncheckedValues,
  onClick,
}) => (
  <div className="ui list">
    {values.sort(naturalsort).map(value => (
      <AttributeValue
        key={value}
        value={value}
        checked={!uncheckedValues.some(x => x === value)}
        onClick={(v) => onClick(attribute, v)}
      />
    ))}
  </div>
);

const AttributeList = ({
  layerId,
  attributes,
  filter,
  onClick,
}) => (
  <div className="content">
    {
      Object.keys(attributes)
        .filter(key => attributes[key].categories !== null)
        .sort(naturalsort)
        .map(key => (
          <div className="ui accordion" key={key} ref={x => $(x).accordion()}>
            <div className="title">
              <i className="dropdown icon"></i>
              {key}
            </div>
            <div className="content">
              <AttributeValueList
                key={key}
                attribute={key}
                values={attributes[key].categories}
                uncheckedValues={(key in filter) ? filter[key] : []}
                onClick={(a, v) => onClick(layerId, a, v)}
              />
            </div>
          </div>
        ))
    }
  </div>
);

export default AttributeList;
