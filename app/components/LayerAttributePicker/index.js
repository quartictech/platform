/**
*
* LayerAttributePicker
*
*/

import React from 'react';

import { FormattedMessage } from 'react-intl';
import messages from './messages';
import styles from './styles.css';
import classNames from 'classnames';

class LayerAttributePicker extends React.Component { // eslint-disable-line react/prefer-stateless-function
  componentDidMount() {
    $(this.dropdown)
    .dropdown({
       onChange: (value) => {
         console.log(value);
         this.props.onChange(value);
       }
     });
  }

  onChange(value) {
    this.props.onChange(value);
  }

  render() {
    let rows = [];
    for (var attribute of this.props.attributes) {
      let itemClasses = classNames("item active", {"selected": attribute === this.props.selected})
      console.log(itemClasses);
      rows.push(<div className={itemClasses} key={attribute} data-value={attribute}>{attribute}</div>);
    }

    return (
      <div className={styles.attributePicker}>
        <div className="ui floating labeled icon dropdown button" ref={(x) => this.dropdown = x}>
                <i className="filter icon"></i>
                <span className="text">{this.props.selected ? this.props.selected : "Pick Attribute"}</span>
                <div className="menu">
                  {rows}
                </div>
              </div>
      </div>
    );
  }
}

export default LayerAttributePicker;
