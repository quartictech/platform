import * as React from "react";
const DocumentTitle = require("react-document-title");  // tslint:disable-line:variable-name
import * as moment from "moment";
import { gql, graphql } from "react-apollo";

import * as _ from "underscore";

import { Button, Collapse, Colors, Classes, Intent, Tag } from "@blueprintjs/core";

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

  logStyle: (event: BuildEvent) => React.CSSProperties = (event) => {
    switch (event.stream) {
      case "stdout": return { color: "white" };
      case "stderr": return { color: Colors.RED5 };
      case "progress": return { color: Colors.BLUE5, fontWeight: "bold" };
      default: return { color: "white" };
    }
  }

  private renderLogLine(event) {
    return (
      <tr key={event.id}>
        <td className={s.logTimestamp}>{this.formatTime(event.time)}</td>
        <td style={this.logStyle(event)}>{event.message}</td>
      </tr>
    );
  }

  private renderLogs(logEvents) {
    return (
      <table>
        <tbody>
          {logEvents.map(event => this.renderLogLine(event))}
        </tbody>
      </table>
    );
  }

  private orderPhases(events: BuildEvent[]) {
    return _.sortBy(
        events.filter(event => event.type === "phase_started"),
        event => event.time,
    );
  }

  private groupByPhase(events: BuildEvent[]) {
    const groupedEvents = _.groupBy(events, event => event.phase_id);
    return _.mapObject(groupedEvents, (val, _) =>
      val.sort((a, b) => a.time - b.time));
  }

  private formatTime = time => moment.unix(time).format("YYYY-MM-DD HH:mm:ss");

  private onPhaseClick(phaseId: string) {
    this.setState({ openPhases:
      Object.assign(this.state.openPhases, {
        [phaseId]: this.state.openPhases[phaseId] ? !this.state.openPhases[phaseId] : true,
      }),
    });
  }

  private getPhaseIntent = (completedEvent) => {
    if (completedEvent) {
      switch (completedEvent.result.__typename) {
        case "Success": return Intent.SUCCESS;
        case "UserError":
        case "InternalError":
          return Intent.DANGER;
      }
    }
    return Intent.NONE;
  }

  private expandButton = phase => (
    <Button
      className="pt-minimal pt-intent-primary"
      style={{ float: "right" }}
      onClick={() => this.onPhaseClick(phase.phase_id)}
    >
      Expand
    </Button>
  )

  private renderPhase(phase, events, phaseIntent?) {
    const logEvents = events.filter(event => event.type === "log");
    const completedEvent = events
      .filter(event => event.type === "phase_completed")[0];
    const intent = phaseIntent || this.getPhaseIntent(completedEvent);

    const hasUserError = completedEvent &&
      completedEvent.result &&
      completedEvent.result.__typename === "UserError";
    const hasLogEvents = logEvents.length > 0;

    const expandable = logEvents.length > 0 || hasUserError;

    return (
      <div key={phase.phase_id} className={s.phaseItem}>
        <div className={s.phaseHeader}>
          {expandable ? this.expandButton(phase) : null}
          <span className={s.phaseTitle}>
            <Tag className={classNames(Classes.MINIMAL, s.phaseTitleTag)} intent={intent}>
              {this.formatTime(phase.time)}
            </Tag>
            <b> {phase.description}</b>
            </span>
        </div>
        <Collapse isOpen={this.state.openPhases[phase.phase_id]}>
          {hasLogEvents ? <pre className={s.bash}>{this.renderLogs(logEvents)}</pre> : null}
          {hasUserError ? <pre className={s.userError}>{completedEvent.result.error}</pre> : null}
        </Collapse>
      </div>
    );
  }

  private renderPhases(events) {
    const phases = this.orderPhases(events);
    const eventsByPhase = this.groupByPhase(events);
    return phases.map(phase =>
      this.renderPhase(phase, eventsByPhase[phase.phase_id]),
    );
  }

  private renderTrigger(events) {
    return events.filter(event => event.type === "trigger_received")
      .map(event => this.renderPhase(
        {
          phase_id: event.id,
          time: event.time,
          description: `Build triggered by ${event.trigger_type}. Awaiting container...`,
        },
        [],
        Intent.NONE,
      ));
  }

  render() {
    if (!this.props.data.loading) {
      const events = this.props.data.build.events;
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
              {this.renderTrigger(events)}
              {this.renderPhases(events)}
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
          id, stream, message, time, phase_id, type
        }
        ... on PhaseStarted {
          id, description, phase_id, time, type
        }
        ... on PhaseCompleted {
          id, phase_id, time, type,
          result {
            ... on UserError { error }
          }
        }
        ... on TriggerReceived {
          id, trigger_type, time, type
        }
        ... on Other {
          id, time
        }
      }
    }
  }
`;

export default graphql(query, {
  options: (props: IProps) => ({
    pollInterval: 5000,
    variables: {
      buildNumber: props.params.build,
    },
  }),
})(BuildView as any);
