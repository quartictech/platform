import * as React from "react";
const DocumentTitle = require("react-document-title");  // tslint:disable-line:variable-name
import * as moment from "moment";
import { gql, graphql } from "react-apollo";

import * as _ from "underscore";

import { Button, Collapse } from "@blueprintjs/core";

import * as classNames from "classnames";

const s = require("./style.css");
import { Build, BuildEvent } from "../../models";
import { intentForDatasetStatus } from "../../helpers/Utils";

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

interface IState {
  openPhases: {
    [key: string]: boolean,
  };
}

class BuildView extends React.Component<IProps, IState> {
  constructor() {
    super();
    this.state = { openPhases: {} };
  }

  private renderLogs(events) {
    return events.map(event => `[${this.formatTime(event.time)}] ${event.message}`)
      .join("\n");
  }

  private orderPhases(events: BuildEvent[]) {
    return events.filter(event => event.type === "phase_started")
      .sort((a, b) => a.time - b.time);
  }

  private groupByPhase(events: BuildEvent[]) {
    const groupedEvents = _.groupBy(events, event => event.phase_id);
    return _.mapObject(groupedEvents, (val, _) =>
      val.filter(event => event.type === "log").sort((a, b) => a.time - b.time));
  }

  private formatTime = time => moment.unix(time).format("YYYY-MM-DD HH:mm:ss");

  private onPhaseClick(phaseId: string) {
    this.setState({ openPhases:
      Object.assign(this.state.openPhases, {
        [phaseId]: this.state.openPhases[phaseId] ? !this.state.openPhases[phaseId] : true,
      }),
    });
  }

  private renderPhase(phase, events) {
    if (events.length === 0) {
      return (
        <div key={phase.phase_id} className={s.phaseItem}>
          <span className={s.phaseTitle}>
            <small>{this.formatTime(phase.time)}</small>
            <b> {phase.description}</b>
          </span>
        </div>
      );
    } else {
      return (
        <div key={phase.phase_id} className={s.phaseItem}>
          <div className={s.phaseHeader}>
            <Button
              className="pt-minimal pt-intent-primary"
              style={{ float: "right" }}
              onClick={() => this.onPhaseClick(phase.phase_id)}
            >
              Expand
            </Button>
            <span className={s.phaseTitle}>
              <small>{this.formatTime(phase.time)}</small>
              <b> {phase.description}</b>
              </span>
          </div>
          <Collapse isOpen={this.state.openPhases[phase.phase_id]}>
            <pre className={s.bash}>{this.renderLogs(events)}</pre>
          </Collapse>
        </div>
      );
    }
  }

  private renderPhases(events) {
    const phases = this.orderPhases(events);
    const eventsByPhase = this.groupByPhase(events);
    return phases.map(phase => this.renderPhase(phase, eventsByPhase[phase.phase_id]));
  }

  render() {
    if (!this.props.data.loading) {
      const logEvents = this.props.data.build.events;
      return (
        <DocumentTitle title={`Quartic - Build #${this.props.data.build.number}`}>
          <div className={s.container}>
            <div>
              <span
                style={{ float: "right" }}
                className={classNames("pt-tag", "pt-minimal", intentForDatasetStatus(this.props.data.build.status))}
              >
                {this.props.data.build.status}
              </span>
              <h1>Build #{this.props.data.build.number}</h1>
            </div>
            <div>
              {this.renderPhases(logEvents)}
            </div>
          </div>
        </DocumentTitle>
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
          stream, message, time, phase_id, type
        }
        ... on PhaseStarted {
          description, phase_id, time, type
        }
        ... on PhaseCompleted {
          phase_id, time, type
        }
        ... on Other {
          time
        }
      }
    }
  }
`;

export default graphql(query, {
  options: (props: IProps) => ({
    variables: {
      buildNumber: props.params.build,
    },
  }),
})(BuildView as any);
