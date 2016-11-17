import * as React from 'react';
import { Link } from 'react-router';

const s = require('./style.css');
const logo = require('./quartic.svg');

class Header extends React.Component<void, void> {
  render() {
    return (
          <nav className="pt-navbar .modifier pt-dark">

  <div className="pt-navbar-group pt-align-left">
    <img
              className={s.logo}
              src={logo}
              role="presentation"
              data-content={`Version: ${(process.env.BUILD_VERSION || "unknown")}`}
              data-variation="mini"
            >
            </img>
            <input className="pt-input" placeholder="Search datasets..." type="text" />
    </div>
    <div className="pt-navbar-group pt-align-right">
      <button className="pt-button pt-minimal pt-icon-document">Upload Data</button>
    </div>
</nav>
          );
  }
}

export { Header }
