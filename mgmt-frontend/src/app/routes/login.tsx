import * as React from "react";
import {
  withRouter,
  InjectedRouter,
} from "react-router"

interface EnsureLoggedInProps {
  router: InjectedRouter,
};

class EnsureLoggedInComponent extends React.Component<EnsureLoggedInProps, {}> {
  isAuthenticated() {
    return localStorage.getItem("quartic-xsrf") !== null;
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

