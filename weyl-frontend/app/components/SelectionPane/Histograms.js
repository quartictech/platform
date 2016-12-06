import React from "react";
import naturalsort from "javascript-natural-sort";
import * as $ from "jquery";
import * as _ from "underscore";

// TODO: blessed vs. non-blessed
const Histograms = ({ histograms }) => (
  <div style={{ maxHeight: "30em", overflow: "auto" }}>
    <table className="ui celled very compact small fixed selectable table">
      {
        _.chain(histograms)
          .sort((a, b) => naturalsort(a.attribute, b.attribute))
          .map(histogram =>
            <AttributeHistogram
              key={histogram.attribute}
              histogram={histogram}
            />
          )
          .value()
      }
    </table>
  </div>
);

const AttributeHistogram = ({ histogram }) => (
  <tbody className="ui accordion" ref={x => $(x).accordion()}>
    <tr className="title">
      <td style={{ fontWeight: "bold" }}>
        <i className="dropdown icon"></i>
        {histogram.attribute}
      </td>
    </tr>

    <tr className="content">
      <td>
        <table className="ui celled very compact small fixed selectable definition table">
          <tbody>
            {
              _.chain(histogram.buckets)
                .sort((a, b) => naturalsort(a.value, b.value))  // Fall back to alphabetical
                .sort((a, b) => b.count - a.count)              // Highest count first
                .map(bucket =>
                  <tr key={bucket.value}>
                    <td className="right aligned">{bucket.value}</td>
                    <td>{bucket.count}</td>
                  </tr>
                )
                .value()
            }
          </tbody>
        </table>
      </td>
    </tr>
  </tbody>
);

export default Histograms;
