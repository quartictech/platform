import * as React from "react";
import { connect } from "react-redux";
import * as actions from "../../redux/actions";

import { AnchorButton, Intent, Spinner } from "@blueprintjs/core";

import { createStructuredSelector } from "reselect";
import {
  withRouter,
  InjectedRouter,
} from "react-router";
const s = require("./style.css");
const logo = require("./quartic.svg");

interface IProps {
  location: any;
  router: InjectedRouter;
  loginGithub: (code: String, state: String) => any;
}

interface IState {
}

class Login extends React.Component<IProps, IState> {
  componentDidMount() {
    const query = this.props.location.query;
    if (query.provider === "gh" && query.code !== null && query.state !== null) {
      this.props.loginGithub(query.code, query.state);
    }
  }

  renderSpinner() {
    return (
      <div className={s.signIn}>
        <h2>Logging you in</h2>
        <div style={{ textAlign: "center", justifyContent: "center" }}>
          <Spinner className="pt-large" />
        </div>
      </div>
    );
  }

  renderLogin() {
    return (
      <div className={s.signIn}>
        <h2>Please sign in.</h2>
        <div className={s.signInButtons}>
          <AnchorButton
            href="/api/auth/gh"
            text="Sign in with GitHub"
            className="pt-large"
            intent={Intent.PRIMARY}
          />
        </div>
      </div>
    );
  }

  render() {
    return (
      <div className={s.container}>
        <div className="pt-card pt-elevation-4" style={{ width: 600, padding: 40, margin: "auto" }} >
          <img
              className={s.logo}
              src={logo}
              role="presentation"
              data-content={`Version: ${(process.env.BUILD_VERSION || "unknown")}`}
              data-variation="mini"
          />
          {this.props.location.query.provider === "gh" ? this.renderSpinner() : this.renderLogin()}
        </div>
      </div>
    );
  }
}

const mapDispatchToProps = {
  loginGithub: actions.loginGithub,
};

const mapStateToProps = createStructuredSelector({
});

// tslint:disable-next-line:variable-name
const LoginWithRouter = withRouter(Login);
export default connect(
  mapStateToProps,
  mapDispatchToProps,
)(LoginWithRouter);
