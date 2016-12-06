import React from "react";
import {
  Button,
  Classes,
  Collapse,
  Overlay,
  Spinner,
} from "@blueprintjs/core";
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
            className={classNames(Classes.MINIMAL, { [Classes.ACTIVE]: this.state.moreAttributesVisible })}
            iconName="more"
            onClick={() => this.setState({ moreAttributesVisible: !this.state.moreAttributesVisible })}
          />
        </div>
        <Collapse isOpen={this.state.moreAttributesVisible}>
          <div className="content">
            <AttributesTable
              featureAttributes={this.props.featureAttributes}
              behavior={this.props.behavior}
              order={this.props.behavior.isAnythingBlessed ? this.props.behavior.unblessedAttributeOrder : this.props.behavior.blessedAttributeOrder}
            />
          </div>
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
    ? <video className="ui fluid image" autoPlay loop src={url} />
    : <img role="presentation" className="ui fluid image" src={url} />
  );
};

const AttributesTable = ({ featureAttributes, behavior, order }) => (
  <div style={{ maxHeight: "30em", overflow: "auto" }}>
    <table className="ui celled very compact small fixed selectable definition table">
      {
        (_.size(featureAttributes) > 1) &&
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
              <td className="right aligned">{key}</td>
              {_.map(featureAttributes, (attrs, id) => <td key={id}>{attrs[key]}</td>)}
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
