import React from "react";
import {
  Button,
  Classes,
  Collapse,
  Overlay,
  Position,
  Spinner,
  Tooltip,
} from "@blueprintjs/core";
import {
  Cell,
  Column,
  ColumnHeaderCell,
  RowHeaderCell,
  Table,
} from "@blueprintjs/table";
import classNames from "classnames";
import naturalsort from "javascript-natural-sort";
import * as $ from "jquery";
import * as _ from "underscore";

class NonHistograms extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor(props) {
    super(props);
    this.state = {
      moreAttributesVisible: false,
    };
  }

  render() {
    return (
      <div>
        <Media
          featureAttributes={this.props.featureAttributes}
          behavior={this.props.behavior}
        />
        <AttributesTable
          featureAttributes={this.props.featureAttributes}
          behavior={this.props.behavior}
          order={this.props.behavior.isAnythingBlessed ? this.props.behavior.blessedAttributeOrder : this.props.behavior.unblessedAttributeOrder}
        />
        <div style={{ textAlign: "center" }}>
          <Button
            className={Classes.MINIMAL}
            iconName="more"
            onClick={() => this.setState({ moreAttributesVisible: !this.state.moreAttributesVisible })}
          />
        </div>
        <Collapse isOpen={this.state.moreAttributesVisible}>
          <AttributesTable
            featureAttributes={this.props.featureAttributes}
            behavior={this.props.behavior}
            order={this.props.behavior.isAnythingBlessed ? this.props.behavior.unblessedAttributeOrder : this.props.behavior.blessedAttributeOrder}
          />
        </Collapse>
      </div>
    );
  }
};

const Media = ({ featureAttributes, behavior }) => {
  if (behavior.imageUrlKey) {
    if (_.size(featureAttributes) === 1) {
      return (
        <Image url={_.values(featureAttributes)[0][behavior.imageUrlKey]} />
      );
    }

    return (
      <table className={Classes.TABLE}>
        <tbody>
          <tr>
            {_.map(featureAttributes, (attrs, id) =>
              <td key={id}><Image url={attrs[behavior.imageUrlKey]} /></td>
            )}
          </tr>
        </tbody>
      </table>
    );
  }

  return null;
};

const Image = ({ url }) => {
  if (!url) {
    return null;
  }

  const isVideo = url.endsWith(".mp4");

  return (isVideo
    ? <video autoPlay loop src={url} style={{ width: "100%", height: "100%" }} />
    : <img role="presentation" src={url} style={{ width: "100%", height: "100%" }} />
  );
};

const AttributesTable = ({ featureAttributes, behavior, order }) => {
  const filteredOrder = order.filter(name => _.some(
    _.values(featureAttributes),
    attrs => isAttributeDisplayable(name, attrs)
  ));
  const numFeatures = _.size(featureAttributes);

  return (
    <Table
      numRows={_.size(filteredOrder)}
      isColumnResizable={false}
      columnWidths={Array(numFeatures).fill(75)}
      renderRowHeader={(row) => <MyRowHeaderCell name={filteredOrder[row]} />}
    >
      {
        _.map(featureAttributes, (attributeMap, id) => {
          return (
            <Column
              key={id}
              name={behavior.title(attributeMap)}
              renderCell={row => <Cell>{attributeMap[filteredOrder[row]]}</Cell>}
            />
          );
        })
      }
    </Table>
  );
};

const MyRowHeaderCell = ({ name }) => (
  <Tooltip
    content={name}
    position={Position.LEFT}
    hoverOpenDelay={50}
  >
    <div className="bp-table-header" style={{ width: "75px" }}>
        <div className="bp-table-row-name">
            <div className="bp-table-row-name-text bp-table-truncated-text">
                {name}
            </div>
        </div>
    </div>
  </Tooltip>
);

const isAttributeDisplayable = (key, attributes) =>
  !key.startsWith("_") && (key in attributes) && (String(attributes[key]).trim() !== "");

export default NonHistograms;
