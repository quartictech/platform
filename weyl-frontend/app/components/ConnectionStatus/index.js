import React from "react";
const $ = require("jquery");

class ConnectionStatus extends React.Component { // eslint-disable-line react/prefer-stateless-function
  render() {
    if (this.props.connectionUp) {
      $(".ui.modal").modal("hide");
    } else {
      $(".ui.modal")
        .modal({ blurring: true, closable: false })
        .modal("show");
    }

    return (
      <div className="ui basic modal">
        <div className="ui massive active text loader">Establishing connection...</div>
      </div>
    );
  }
}

export default ConnectionStatus;
