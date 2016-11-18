import * as React from 'react';
import { connect } from 'react-redux';
import { push } from 'react-router-redux';
import { slugify } from '../../helpers/Utils';

import { Grid, Col } from 'react-bootstrap';
import * as Blueprint from "@blueprintjs/core";
const { Menu, MenuItem, MenuDivider } = Blueprint;

interface IProps {
  getNodes: Redux.ActionCreator<any>;
  push: Redux.ActionCreator<any>;
}

const s = require('./style.css');

class Home extends React.Component<IProps, any> {
  handleNavigate(node: string) {
    this.props.push(`/location/${slugify(node)}`);
  }

  render() {
    return (
      <div className={s.container}>

      <div className={s.left}>
      <Menu className=".modifier pt-elevation-1">
               <MenuItem
                   iconName="new-text-box"
                   text="Live" />
               <MenuItem
                   iconName="new-object"
                   text="Static" />
               <MenuDivider />
               <MenuItem text="Settings..." iconName="cog" />
           </Menu>
      </div>

      <div className={s.main}>
      <table className="pt-table .modifier">
  <thead>
    <th>Project</th>
    <th>Description</th>
    <th>Technologies</th>
  </thead>
  <tbody>
    <tr>
      <td>Blueprint</td>
      <td>CSS framework and UI toolkit</td>
      <td>Sass, TypeScript, React</td>
    </tr>
    <tr>
      <td>TSLint</td>
      <td>Static analysis linter for TypeScript</td>
      <td>TypeScript</td>
    </tr>
    <tr>
      <td>Plottable</td>
      <td>Composable charting library built on top of D3</td>
      <td>SVG, TypeScript, D3</td>
    </tr>
  </tbody>
</table>
      </div>
      </div>
    );
  }
}

export { Home };

export default connect(
  state => ({}),
  { push }
)(Home);
