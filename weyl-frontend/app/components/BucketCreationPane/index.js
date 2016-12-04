import React from "react";
import {
  Button,
  Checkbox,
  Classes,
  IconContents,
  InputGroup,
  Intent,
  Menu,
  MenuItem,
  Popover,
  PopoverInteractionKind,
  Position,
} from "@blueprintjs/core";
import classNames from "classnames";
import * as _ from "underscore";
import Pane from "../Pane";
import Select from "../Select";

class BucketCreationPane extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor(props) {
    super(props);
    this.state = {
      selectedLayer: null,
      selectedBuckets: null,
      selectedAggregation: "count",
      selectedAttribute: null,
      normalizeToArea: false,
    };
    this.onComputeClick = this.onComputeClick.bind(this);
    this.onAggregationChange = this.onAggregationChange.bind(this);
    this.onNormalizeToAreaChange = this.onNormalizeToAreaChange.bind(this);
    this.onBucketsChange = this.onBucketsChange.bind(this);
    this.onLayerChange = this.onLayerChange.bind(this);
    this.isValid = this.isValid.bind(this);
    this.requiresAttribute = this.requiresAttribute.bind(this);
    this.layerIdsToNames = this.layerIdsToNames.bind(this);
    this.attributeNames = this.attributeNames.bind(this);
  }

  componentWillReceiveProps(nextProps) {
    // Invalidate if layers are closed
    if (!nextProps.layers.has(this.state.selectedLayer)) {
      this.onLayerChange(null);
    }
    if (!nextProps.layers.has(this.state.selectedBuckets)) {
      this.onBucketsChange(null);
    }
  }

  render() {
    return (
      <Pane
        title="Bucket"
        iconName="helper-management"
        visible={this.props.visible}
        onClose={this.props.onClose}
      >
        <label className={Classes.LABEL}>
          <div>Entities</div>
          <Picker
            iconName="layers"
            placeholder="Select layer..."
            entries={this.layerIdsToNames()}
            selectedKey={this.state.selectedLayer}
            onChange={this.onLayerChange}
          />
        </label>

        <label className={Classes.LABEL}>
          <div>Buckets</div>
          <Picker
            iconName="layers"
            placeholder="Select layer..."
            entries={this.layerIdsToNames()}
            selectedKey={this.state.selectedBuckets}
            onChange={this.onBucketsChange}
          />
        </label>

        <label className={Classes.LABEL} htmlFor="aggregation">
          Aggregation
          <div className="pt-control-group" id="aggregation">
            <Select
              entries={{ count: "Count", sum: "Sum of", mean: "Mean of" }}
              selectedKey={this.state.selectedAggregation}
              onChange={this.onAggregationChange}
            />

            <Picker
              iconName="property"
              entries={this.attributeNames()}
              disabled={!this.requiresAttribute(this.state.selectedAggregation)}
              value={this.state.selectedAttribute}
              onChange={(v) => { console.log(v); this.setState({ selectedAttribute: v}); }}
            />
          </div>
        </label>

        <Checkbox
          label="Normalise to area"
          checked={this.state.normalizeToArea}
          onChange={this.onNormalizeToAreaChange}
        />

        <div style={{ textAlign: "right" }}>
          <Button
            iconName="calculator"
            text="Compute"
            disabled={!this.isValid()}
            intent={Intent.PRIMARY}
            onClick={this.onComputeClick}
          />
        </div>
      </Pane>
    );
  }

  isValid() {
    return (
      (this.state.selectedLayer) &&
      (this.state.selectedBuckets) &&
      (!this.requiresAttribute(this.state.selectedAggregation) || (this.state.selectedAttribute))
    );
  }

  requiresAttribute(aggregation) {
    return _.contains(["sum", "mean"], aggregation);
  }

  layerIdsToNames() {
    return _.object(
      _.map(this.props.layers.toArray(), l => [l.get("id"), l.getIn(["metadata", "name"])])
    );
  }

  attributeNames() {
    const selectedFeatureLayer = this.props.layers.get(this.state.selectedLayer);
    const numericAttributes = [];
    if (selectedFeatureLayer !== undefined) {
      for (const key of Object.keys(selectedFeatureLayer.toJS().attributeSchema.attributes)) {
        const attribute = selectedFeatureLayer.toJS().attributeSchema.attributes[key];
        if (attribute.type === "NUMERIC") {
          numericAttributes.push(key);
        }
      }
    }
    return numericAttributes;
  }

  onLayerChange(value) {
    // Avoid unnecessarily resetting attribute
    if (value !== this.state.selectedLayer) {
      this.setState({ selectedLayer: value, selectedAttribute: null });
    }
  }

  onBucketsChange(value) {
    this.setState({ selectedBuckets: value });
  }

  onNormalizeToAreaChange(e) {
    this.setState({ normalizeToArea: e.currentTarget.checked });
  }

  onAggregationChange(value) {
    this.setState({
      selectedAggregation: value,
      selectedAttribute: this.requiresAttribute(value) ? this.state.selectedAttribute : null
    });
  }

  onComputeClick() {
    const computeSpec = {
      type: "bucket",
      features: this.state.selectedLayer,
      buckets: this.state.selectedBuckets,
      aggregation: {
        type: this.state.selectedAggregation,
      },
      normalizeToArea: this.state.normalizeToArea,
    };

    if (this.requiresAttribute(this.state.selectedAggregation)) {
      computeSpec.aggregation.attribute = this.state.selectedAttribute;
    }

    this.props.onCompute(computeSpec);
  }
}

class Picker extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor(props) {
    super(props);
    this.state = {
      text: "",
      menuVisible: false,
    };

    this.menu = this.menu.bind(this);
    this.onSelectEntry = this.onSelectEntry.bind(this);
    this.onChangeText = this.onChangeText.bind(this);
    this.entriesAsMap = this.entriesAsMap.bind(this);
  }

  componentWillReceiveProps(nextProps) {
    console.log(this.props.selectedKey, nextProps.selectedKey);
    if (nextProps.selectedKey && (this.props.selectedKey !== nextProps.selectedKey)) {
      console.log(this.entriesAsMap());
      this.setState({ text: this.entriesAsMap()[nextProps.selectedKey] });
    }
  }

  render() {
    return (
      <Popover
        autoFocus={false}
        enforceFocus={false}
        popoverClassName={Classes.MINIMAL}
        content={this.menu()}
        isDisabled={this.props.disabled}
        isOpen={this.props.disabled ? null : this.state.menuVisible}
        onInteraction={(nextOpenState) => this.setState({ menuVisible: nextOpenState })}
        interactionKind={PopoverInteractionKind.CLICK}
        position={Position.BOTTOM_LEFT}
      >
        <InputGroup
          disabled={this.props.disabled}
          leftIconName={this.props.iconName}
          placeholder={this.props.placeholder}
          value={this.state.text}
          onChange={(e) => this.onChangeText(e.target.value)}
          intent={(this.props.selectedKey && !this.props.invalid) ? Intent.NONE : Intent.DANGER}
          className="pt-fill"
        />
      </Popover>
    );
  }

  menu() {
    return (
      <Menu>
        {
          _.chain(this.entriesAsMap())
            .pairs()
            .filter(entry => !this.state.text || entry[1].toLowerCase().includes(this.state.text.toLowerCase()))
            .map(entry =>
              <MenuItem
                key={entry[0]}
                text={entry[1]}
                label={(this.props.selectedKey === entry[0]) ? IconContents.TICK : ""}
                iconName={this.props.iconName}
                className={classNames(Classes.MENU_ITEM)}
                onClick={() => this.onSelectEntry(entry[0])}
              />
            )
            .value()
        }
      </Menu>
    );
  }

  onSelectEntry(key) {
    this.setState({ menuVisible: false });
    this.props.onChange(key);
  }

  onChangeText(text) {
    this.setState({ text, menuVisible: true });
    this.props.onChange(_.invert(this.entriesAsMap())[text]);
  }

  entriesAsMap() {
    return _.isArray(this.props.entries)
      ? _.object(_.map(this.props.entries, x => [x, x]))
      : this.props.entries;
  }
};

Picker.defaultProps = {
  disabled: false,
  invalid: false,
};

export default BucketCreationPane;
