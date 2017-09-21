import * as React from "react";
const DocumentTitle = require("react-document-title");  // tslint:disable-line:variable-name
import { connect } from "react-redux";

import { Link } from "react-router";
import * as classNames from "classnames";

import * as moment from "moment";

import { Button, Spinner, IconClasses, Classes, Intent } from "@blueprintjs/core";

import { gql, graphql } from "react-apollo";
import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
import { intentForDatasetStatus } from "../../helpers/Utils";
const s = require("./style.css");

import { FeedItem, Validate, Execute } from "../../models";

interface IProps {
  buildPipeline: Function;
  data: {
    loading: boolean;
    feed: FeedItem[];
    error: any;
  };
}

export function isValidate(item: FeedItem): item is Validate {
  return (item as any).trigger.type === "github_webhook";
}

export function isExecute(item: FeedItem): item is Execute {
  return (item as any).trigger.type === "manual";
}

class HomeView extends React.Component<IProps, {}> {
  renderValidate(item: Validate) {
    return (
      <div className={s.feedItem} key={item.id}>
        <span className={classNames(s.cardIcon, "pt-icon-large", "pt-icon-upload")} />
        <div className={classNames("pt-tag", "pt-minimal", intentForDatasetStatus(item.status), s.statusText)}>
          {item.status}
        </div>
        <div className={s.cardTime}>
          <small>{moment.min(moment.unix(item.time), moment()).fromNow()}</small>
        </div>

        <div className={s.cardBody}>
          <h5>
            <Link to={`/build/${item.number}`}>
              {item.number} - Validate
            </Link>
          </h5>
          <b>Branch</b> {item.branch}
        </div>
      </div>
    );
  }

  renderExecute(item: Execute) {
    return (
      <div className={s.feedItem} key={item.id}>
        <span className={classNames(s.cardIcon, "pt-icon-large", "pt-icon-graph")} />
        <div className={classNames("pt-tag", "pt-minimal", intentForDatasetStatus(item.status), s.statusText)}>
          {item.status}
        </div>
        <div className={s.cardTime}>
            <small>{moment.min(moment.unix(item.time), moment()).fromNow()}</small>
        </div>

        <div className={s.cardBody}>
          <h5>
            <Link to={`/build/${item.number}`}>
              {item.number} - Build
            </Link>
          </h5>
          <b>Branch</b> {item.branch}
        </div>
      </div>
    );
  }

  renderItem = (item: FeedItem) => {
    if (isValidate(item)) {
      return this.renderValidate(item);
    } else if (isExecute(item)) {
      return this.renderExecute(item);
    } else {
      return null;
    }
  }

  renderNoItems = () => (
    <div>
      <div className={classNames(Classes.NON_IDEAL_STATE, s.noItems)}>
        <div className={classNames(Classes.NON_IDEAL_STATE_VISUAL, Classes.NON_IDEAL_STATE_ICON)}>
          <span className={classNames(Classes.ICON, IconClasses.LIGHTBULB)}/>
        </div>
        <h4 className={Classes.NON_IDEAL_STATE_TITLE}>You haven't run any builds yet.</h4>
        <div className={Classes.NON_IDEAL_STATE_DESCRIPTION}>
          Push your code or
              <a onClick={() => this.props.buildPipeline()}>
              &nbsp;manually trigger&nbsp;
              </a>
          a build to get started.
        </div>
      </div>
    </div>
  )

  renderFeed = () => (
    <div>
      <Button
        text="Build pipeline"
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
      <DocumentTitle title="Quartic - Home">
        <div className={s.container}>
          {this.renderContainer()}
        </div>
      </DocumentTitle>
    );
  }
}

export { HomeView };

const mapDispatchToProps = {
  buildPipeline: actions.buildPipeline,
};

const mapStateToProps = createStructuredSelector({
  ui: selectors.selectUi,
});

const query = gql`{
  feed {
    type
    id
    time
    status
    number
    branch
    trigger {
      type
    }
  }
}`;

export default graphql(query)(connect(
  mapStateToProps,
  mapDispatchToProps,
)(HomeView));
