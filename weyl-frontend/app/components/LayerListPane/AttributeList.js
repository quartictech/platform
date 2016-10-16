import React from "react";
import naturalsort from "javascript-natural-sort";
const $ = require("jquery");
const _ = require("underscore");

const AttributeValue = ({
  value,
  checked,
  onClick,
}) => (
  <tr>
    <td className="right aligned">{value}</td>
    <td>
      <div className="ui fitted toggle checkbox">
        <input type="checkbox" checked={checked} name={value} onChange={() => onClick(value)} />
        <label htmlFor={value}></label>
      </div>
    </td>
  </tr>
);

const AttributeValueList = ({
  attribute,
  values,
  uncheckedValues,
  onClick,
}) => (
  <tbody className="ui accordion" ref={x => $(x).accordion()}>
    <tr className="title">
      <td style={{ fontWeight: "bold" }}>
        <i className="dropdown icon"></i>
        {attribute}
      </td>
    </tr>

    <tr className="content">
      <td>
        <table className="ui celled very compact small fixed selectable definition table">
          <tbody>
            <AttributeValue
              key="< N/A >"
              value="< N/A >"
              checked={!uncheckedValues.notApplicable}
              onClick={(v) => onClick(attribute)} // Note no second argument to callback
            />
            {
              values.sort(naturalsort).map(value => (
                <AttributeValue
                  key={value}
                  value={value}
                  checked={!uncheckedValues.categories.some(x => x === value)}
                  onClick={(v) => onClick(attribute, v)}
                />
              ))
            }
          </tbody>
        </table>
      </td>
    </tr>
  </tbody>
);

const AttributeList = ({
  layerId,
  attributes,
  filter,
  onClick,
}) => (
  <div style={{ maxHeight: "30em", overflow: "auto" }}>
    <table className="ui celled very compact small fixed selectable table">
      {
        Object.keys(attributes)
          .filter(key => attributes[key].categories !== null)
          .sort(naturalsort)
          .map(key => (
            <AttributeValueList
              key={key}
              attribute={key}
              values={attributes[key].categories}
              uncheckedValues={(key in filter) ? filter[key] : defaultUncheckedValues()}
              onClick={(a, v) => onClick(layerId, a, v)}
            />
          ))
      }
    </table>
  </div>
);

const defaultUncheckedValues = () => ({
  notApplicable: false,
  categories: [],
});


export default AttributeList;
