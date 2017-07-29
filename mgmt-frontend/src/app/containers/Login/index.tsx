import * as React from "react";
import { connect } from "react-redux";

import { AnchorButton, Intent } from "@blueprintjs/core";

import { createStructuredSelector } from "reselect";
const s = require("./style.css");
const logo = require("./quartic.svg");

interface IProps {
}

interface IState {
}

class Login extends React.Component<IProps, IState> {
  render() {
    return (
      <div className={s.container}>
        <div className="pt-card pt-elevation-4" style={{width: 600, padding: 40, margin: "auto"}} >
          <img
              className={s.logo}
              src={logo}
              role="presentation"
              data-content={`Version: ${(process.env.BUILD_VERSION || "unknown")}`}
              data-variation="mini"
            />
          <div className={s.signIn}>
            <h2>Please sign in.</h2>
            <div className={s.signInButtons}>
              <AnchorButton text="Sign in with GitHub" className="pt-large" intent={Intent.PRIMARY} />
            </div>
          </div>
        </div>
      </div>
    );
  }
}

const mapDispatchToProps = {
};

const mapStateToProps = createStructuredSelector({
});

export default connect(
  mapStateToProps,
  mapDispatchToProps,
)(Login);
