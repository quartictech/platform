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
  componentDidMount() {
    console.log("redirecting");
    if (!isAuthenticated) {
      console.log("redirecting");
      this.props.router.push("/login");
    }
  }

  render() {
    const { children } = this.props;
    console.log("Hello");
    return (isAuthenticated ? <div>{children}</div> : null);
  }
};
export const EnsureLoggedIn = withRouter(EnsureLoggedInComponent);

