import * as React from "react";
import {
  withRouter,
  InjectedRouter,
} from "react-router";

import { QUARTIC_XSRF } from "../helpers/Utils";

interface EnsureLoggedInProps {
  router: InjectedRouter,
};

class EnsureLoggedInComponent extends React.Component<EnsureLoggedInProps, {}> {
  isAuthenticated() {
    return localStorage.getItem(QUARTIC_XSRF) !== null;
  }

  componentDidMount() {
    if (!this.isAuthenticated()) {
      this.props.router.push("/login");
    }
  }

  componentWillReceiveProps() {
    if (!this.isAuthenticated()) {
      this.props.router.push("/login");
    }
  }

  render() {
    const { children } = this.props;
    return (this.isAuthenticated() ? <div>{children}</div> : null);
  }
};
export const EnsureLoggedIn = withRouter(EnsureLoggedInComponent);

