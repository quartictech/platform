import * as React from "react";
import { connect } from "react-redux";

import { createStructuredSelector } from "reselect";
const s = require("./style.css");

interface IProps {
}

interface IState {
}

class Login extends React.Component<IProps, IState> {
  render() {
    return (
      <div className={s.container}>
        Login
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
