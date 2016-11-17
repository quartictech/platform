import * as React from 'react';
import { connect } from 'react-redux';
import { push } from 'react-router-redux';
import { slugify } from '../../helpers/Utils';

import { Grid, Col } from 'react-bootstrap';
import * as Blueprint from "@blueprintjs/core";
const { Menu, MenuItem, MenuDivider } = Blueprint;

interface IProps {
  getNodes: Redux.ActionCreator;
  push: Redux.ActionCreator;
}

class Home extends React.Component<IProps, any> {
  handleNavigate(node: string) {
    this.props.push(`/location/${slugify(node)}`);
  }

  render() {
    return (
      <Grid>
      <Col xs={6} md={4} >
      <Menu>
               <MenuItem
                   iconName="new-text-box"
                   text="New text box" />
               <MenuItem
                   iconName="new-object"
                   text="New object" />
               <MenuItem
                   iconName="new-link"
                   text="New link" />
               <MenuDivider />
               <MenuItem text="Settings..." iconName="cog" />
           </Menu>
      </Col>

      <Col xs={12} md={8} >
      </Col>
      </Grid>
    );
  }
}

export { Home };

export default connect(
  state => ({}),
  { push }
)(Home);
