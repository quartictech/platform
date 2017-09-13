import * as React from "react";
import * as moment from "moment";
import { gql, graphql } from "react-apollo";

const s = require("./style.css");
import { Build } from "../../models";

interface IProps {
  params: {
    build: number;
  };
  data: {
    loading: boolean;
    build: Build;
    error: any;
  };
}

class BuildView extends React.Component<IProps, {}> {
  renderLogs(events) {
    return events.map(event => `[${moment.unix(event.time).format()}] ${event.message}`)
      .join("\n");
  }

  render() {
    if (!this.props.data.loading) {
      const logEvents = this.props.data.build.events;
      return (
        <div className={s.container}>
          <h1>Build #{this.props.data.build.number}</h1>
          <pre>
            {this.renderLogs(logEvents)}
          </pre>
        </div>
      );
    } else {
      return null;
    }
  }
}

const query = gql`
  query FetchById($buildNumber: Long) {
    build(number: $buildNumber) {
      type, id, time, status, number
      events {
        ... on Log {
          stream, message, time
        }
        ... on Other {
          time
        }
      }
    }
  }
`;

export default graphql(query, {
    options: (props: IProps) => ({variables: {
      buildNumber: props.params.build
    }})
  })(BuildView as any);
