import React from "react";
import {
  Button,
  Classes,
  Collapse,
} from "@blueprintjs/core";
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
      <div style={{ display: this.props.visible ? "block" : "none" }}>
        <Media
          featureAttributes={this.props.featureAttributes}
          behavior={this.props.behavior}
        />
        <AttributesTable
          featureAttributes={this.props.featureAttributes}
          behavior={this.props.behavior}
          order={this.props.behavior.isAnythingBlessed ? this.props.behavior.blessedAttributeOrder : this.props.behavior.unblessedAttributeOrder}
          showHeaders={_.size(this.props.featureAttributes) > 1}
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
            showHeaders={false}
          />
        </Collapse>
      </div>
    );
  }
}

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
    ? <video autoPlay loop src={url} style={{ width: "100%" }} />
    : <img role="presentation" src={url} style={{ width: "100%" }} />
  );
};

const AttributesTable = ({ featureAttributes, behavior, order, showHeaders }) => (
  <div style={{ maxHeight: "30em", overflow: "auto" }}>
    <table className="pt-table pt-interactive pt-elevation-0" style={{ width: "100%", tableLayout: "fixed" }}>
      {
        showHeaders &&
          <thead>
            <tr>
              <th />
              {_.map(featureAttributes, (attrs, id) => <th key={id}>{behavior.title(attrs)}</th>)}
            </tr>
          </thead>
      }
      <tbody>
        {order
          .filter(key => _.some(_.values(featureAttributes), attrs => isAttributeDisplayable(key, attrs)))
          .map(key => (
            <tr key={key}>
              <td
                style={{ textAlign: "right", wordWrap: "break-word" }}
              >
                <small>{key}</small>
              </td>
              {
                _.map(featureAttributes, (attrs, id) => (
                  <td
                    className="bp-table-cell-client"
                    style={{ fontWeight: "bold", wordWrap: "break-word" }}
                    key={id}
                  >
                    <small>{attrs[key]}</small>
                  </td>
                ))
              }
            </tr>
          ))
        }
      </tbody>
    </table>
  </div>
);

const isAttributeDisplayable = (key, attributes) =>
  !key.startsWith("_") && (key in attributes) && (String(attributes[key]).trim() !== "");

export default NonHistograms;
