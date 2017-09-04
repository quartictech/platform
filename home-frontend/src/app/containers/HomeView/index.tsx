import * as React from "react";
import { connect } from "react-redux";

import { Link } from "react-router";
import * as classNames from "classnames";

import * as moment from "moment";

import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
const s = require("./style.css");

import { FeedItem, Build } from "../../models";

interface IProps {
  fetchFeed: Function;
  feedItems: FeedItem[];
}

export function isBuild(item: FeedItem): item is Build {
  return item.type === "build";
}

class HomeView extends React.Component<IProps, {}> {
  componentDidMount() {
    this.props.fetchFeed();
  }

  intentForStatus = (status) => {
    switch (status) {
      case "running": return "pt-intent-primary";
      case "success": return "pt-intent-success";
      case "error": return "pt-intent-danger";
    }
  }

  renderItem = (item: FeedItem) => {
    if (isBuild(item)) {
      return (
        <div className={s.feedItem} key={item.id}>
          <div className={classNames("pt-card", s.feedCard)}>
            <span className={classNames(s.cardIcon, "pt-icon-large", "pt-icon-build")} />
            <div className={classNames("pt-tag", this.intentForStatus(item.status), s.statusText)}>{item.status}</div>

            <div className={s.cardBody}>
              <h5>
                <Link to={`/pipeline/${item.buildNumber}`}>
                  Build {item.buildNumber}
                </Link>&nbsp;
                <small>{moment.unix(item.time).fromNow()}</small>
              </h5>
              <b>Branch</b> {item.branch}
            </div>
          </div>
        </div>
      );
    }
  }

  render() {
    return (
      <div className={s.container}>
        <h1>Activity</h1>
        <div className={s.feed}>
          {this.props.feedItems.map(item => this.renderItem(item))}
        </div>
      </div>
    );
  }
}

export { HomeView };

const mapDispatchToProps = {
  fetchFeed: actions.fetchFeed,
};

const mapStateToProps = createStructuredSelector({
  ui: selectors.selectUi,
  feedItems: selectors.selectFeed,
});

export default connect(
  mapStateToProps,
  mapDispatchToProps,
)(HomeView);
