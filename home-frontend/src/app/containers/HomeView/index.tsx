import * as React from "react";
import { connect } from "react-redux";
import { Link } from "react-router";
import * as classNames from "classnames";
import * as moment from "moment";
import { Button, Spinner, Classes, Intent } from "@blueprintjs/core";
import { createStructuredSelector } from "reselect";
import { gql, graphql } from "react-apollo";

import * as actions from "../../redux/actions";
const s = require("./style.css");
import { FeedItem, Build } from "../../models";

interface IProps {
  data: {
    loading: boolean;
    feed: FeedItem[];
    error: any;
  };
  buildPipeline: Function;
}

export function isBuild(item: FeedItem): item is Build {
  return item.type === "build";
}

class HomeView extends React.Component<IProps, {}> {
  componentDidMount() {
  }

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

  renderNoItems = () => (
    <div className={classNames("pt-non-ideal-state", s.noItems)}>
      <div className="pt-non-ideal-state-visual pt-non-ideal-state-icon">
        <span className="pt-icon pt-icon-lightbulb"/>
      </div>
      <h4 className="pt-non-ideal-state-title">You haven't run any builds yet.</h4>
      <div className="pt-non-ideal-state-description">
        Push your code or manually trigger a build to get started.
      </div>
    </div>
  )

  renderFeed = () => (
    <div>
      <Button
        text="Build Pipeline"
        iconName="play"
        intent={Intent.SUCCESS}
        style={{ float: "right" }}
        onClick={() => this.props.buildPipeline()}
      />
      <h2>Activity</h2>
      <div className={s.feed}>
        {this.props.data.feed.map(item => this.renderItem(item))}
      </div>
    </div>
  )

  renderContainer() {
    if (this.props.data.loading) {
      return (
        <div className={s.noItems}>
          <Spinner className={Classes.LARGE} />
        </div>
      );
    } else if (this.props.data.feed) {
      if (this.props.data.feed.length > 0) {
        return this.renderFeed();
      } else {
        return this.renderNoItems();
      }
    }
  }

  render() {
    return (
      <div className={s.container}>
        {this.renderContainer()}
      </div>
    );
  }
}

const mapDispatchToProps = {
  buildPipeline: actions.buildPipeline,
};

const mapStateToProps = createStructuredSelector({
});

const query = gql`{
  feed { type, id, time, status, number }
}`;

export default graphql(query)(connect(
  mapStateToProps,
  mapDispatchToProps,
)(HomeView));
