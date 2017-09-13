import * as React from "react";
import { Link } from "react-router";
import * as classNames from "classnames";
import * as moment from "moment";
import { gql, graphql } from "react-apollo";

const s = require("./style.css");
import { FeedItem, Build } from "../../models";

interface IProps {
  data: {
    loading: boolean;
    build: Build;
    error: any;
  };
  params: {
    build: number;
  };
  buildPipeline: Function;
}

export function isBuild(item: FeedItem): item is Build {
  return item.type === "build";
}

class BuildView extends React.Component<IProps, {}> {
  intentForStatus = (status) => {
    switch (status) {
      case "running": return "pt-intent-primary";
      case "success": return "pt-intent-success";
      case "failure": return "pt-intent-danger";
    }
  }

  renderItem = (item: FeedItem) => {
    if (isBuild(item)) {
      return (
        <div className={s.feedItem} key={item.id}>
          <span className={classNames(s.cardIcon, "pt-icon-large", "pt-icon-git-commit")} />
          <div className={classNames("pt-tag", "pt-minimal", this.intentForStatus(item.status), s.statusText)}>
            {item.status}
          </div>
          <div className={s.cardTime}>
              <small>{moment.min(moment.unix(item.time), moment()).fromNow()}</small>
          </div>

          <div className={s.cardBody}>
            <h5>
              <Link to={`/pipeline/${item.number}`}>
                Build #{item.number}
              </Link>
            </h5>
            <b>Branch</b> {item.branch}
          </div>
        </div>
      );
    }
  }


  render() {
    return (
      <div className={s.container} />
    );
  }
}

const query = gql`
  query Build($buildNumber: Long) {
    build (buildNumber: $buildNumber) {
      id,
      events {
        ... on Default {
          time
        }
      }
    }
}`;

export default graphql<any, IProps>(query, {
  options: ({ params }) => (
    { variables: { buildNumber: params.build } }
  )})(BuildView);
