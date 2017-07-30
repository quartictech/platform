import * as React from "react";
import {
  withRouter,
  InjectedRouter,
} from "react-router"

const isAuthenticated = false;

interface EnsureLoggedInProps {
  router: InjectedRouter,
};

class EnsureLoggedInComponent extends React.Component<EnsureLoggedInProps, {}> {
  isAuthenticated() {
    return localStorage.getItem("quartic-xsrf") !== null
  }

  componentDidMount() {
    if (!this.isAuthenticated()) {
      this.props.router.push("/login");
    }
  }

  render() {
    const { children } = this.props;
    return (isAuthenticated ? <div>{children}</div> : null);
  }
};
export const EnsureLoggedIn = withRouter(EnsureLoggedInComponent);

