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
  fetchBuilds: Function;
  builds: FeedItem[];
}

export function isBuild(item: FeedItem): item is Build {
  return item.type === "build";
}

// const feedItems = [
//   {type: FeedItemType.BUILD, id: "0", buildNumber: 100, status: "running" },
//   {type: FeedItemType.BUILD, id: "1", buildNumber: 101, status: "success" },
// ];

class HomeView extends React.Component<IProps, {}> {
  componentDidMount() {
    this.props.fetchBuilds();
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
                </Link>
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
        <div className={s.feed}>
          {this.props.builds.map(item => this.renderItem(item))}
        </div>
      </div>
    );
  }
}

export { HomeView };

const mapDispatchToProps = {
  fetchBuilds: actions.fetchBuilds,
};

const mapStateToProps = createStructuredSelector({
  ui: selectors.selectUi,
  builds: selectors.selectBuilds,
});

export default connect(
  mapStateToProps,
  mapDispatchToProps,
)(HomeView);
